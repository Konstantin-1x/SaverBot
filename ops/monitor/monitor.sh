#!/bin/sh
set -eu

CHECK_INTERVAL_SECONDS="${MONITOR_INTERVAL_SECONDS:-60}"
FAILURE_THRESHOLD="${MONITOR_FAILURE_THRESHOLD:-3}"
REMINDER_SECONDS="${MONITOR_REMINDER_SECONDS:-3600}"
INSTANCE="${ALERT_INSTANCE:-jackpot-saver}"
APP_HEALTH_URL="${APP_HEALTH_URL:-http://app:8080/actuator/health/readiness}"

failures=0
alerted=0
last_alert=0

alert() {
  message="$1"
  escaped="$(printf '%s' "$message" | sed 's/\\/\\\\/g; s/"/\\"/g')"
  if [ -n "${ALERT_WEBHOOK_URL:-}" ]; then
    curl -fsS --max-time 15 -H 'Content-Type: application/json' \
      -d "{\"text\":\"$escaped\"}" "$ALERT_WEBHOOK_URL" >/dev/null || true
  fi
  if [ -n "${BOT_TOKEN:-}" ] && [ -n "${ALERT_TELEGRAM_CHAT_ID:-}" ]; then
    curl -fsS --max-time 15 \
      --data-urlencode "chat_id=${ALERT_TELEGRAM_CHAT_ID}" \
      --data-urlencode "text=${message}" \
      "https://api.telegram.org/bot${BOT_TOKEN}/sendMessage" >/dev/null || true
  fi
}

while true; do
  date -u +%s > /tmp/.last-check
  app_ok=0
  db_ok=0
  curl -fsS --max-time 10 "$APP_HEALTH_URL" >/dev/null && app_ok=1
  pg_isready -q && db_ok=1

  if [ "$app_ok" -eq 1 ] && [ "$db_ok" -eq 1 ]; then
    if [ "$alerted" -eq 1 ]; then
      alert "✅ ${INSTANCE}: service recovered; application and PostgreSQL are healthy"
    fi
    failures=0
    alerted=0
  else
    failures=$((failures + 1))
    now="$(date -u +%s)"
    if [ "$failures" -ge "$FAILURE_THRESHOLD" ] \
      && { [ "$alerted" -eq 0 ] || [ $((now - last_alert)) -ge "$REMINDER_SECONDS" ]; }; then
      alert "🚨 ${INSTANCE}: health failure app=${app_ok} postgres=${db_ok} consecutive=${failures}"
      alerted=1
      last_alert="$now"
    fi
  fi
  sleep "$CHECK_INTERVAL_SECONDS"
done
