#!/bin/sh
set -eu

: "${PGHOST:?PGHOST is required}"
: "${PGDATABASE:?PGDATABASE is required}"
: "${PGUSER:?PGUSER is required}"
: "${PGPASSWORD:?PGPASSWORD is required}"
: "${BACKUP_ENCRYPTION_KEY:?BACKUP_ENCRYPTION_KEY is required}"

BACKUP_DIR="${BACKUP_DIR:-/backups}"
INTERVAL_SECONDS="${BACKUP_INTERVAL_SECONDS:-86400}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
STATUS_FILE="${BACKUP_STATUS_FILE:-/backups/.last-success}"
HOSTNAME_LABEL="${ALERT_INSTANCE:-jackpot-saver}"

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

run_backup() {
  timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
  final="${BACKUP_DIR}/${PGDATABASE}-${timestamp}.dump.enc"
  temp="${final}.partial"
  plain="/tmp/${PGDATABASE}-${timestamp}.dump.gz"

  rm -f "$temp" "$plain"
  if ! pg_dump --format=custom --compress=9 --no-owner --no-acl --file="$plain"; then
    rm -f "$plain"
    alert "🚨 ${HOSTNAME_LABEL}: PostgreSQL backup failed during pg_dump"
    return 1
  fi

  if ! pg_restore --list "$plain" >/dev/null; then
    rm -f "$plain"
    alert "🚨 ${HOSTNAME_LABEL}: PostgreSQL backup validation failed"
    return 1
  fi

  if ! openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 \
      -pass env:BACKUP_ENCRYPTION_KEY -in "$plain" -out "$temp"; then
    rm -f "$plain" "$temp"
    alert "🚨 ${HOSTNAME_LABEL}: PostgreSQL backup encryption failed"
    return 1
  fi

  mv "$temp" "$final"
  (cd "$BACKUP_DIR" && sha256sum "$(basename "$final")" > "$(basename "$final").sha256")
  rm -f "$plain"

  if [ -n "${BACKUP_S3_BUCKET:-}" ]; then
    destination="backup:${BACKUP_S3_BUCKET}/${BACKUP_S3_PREFIX:-jackpot-saver}"
    if ! rclone copyto "$final" "${destination}/$(basename "$final")" \
        || ! rclone copyto "${final}.sha256" "${destination}/$(basename "$final").sha256"; then
      alert "🚨 ${HOSTNAME_LABEL}: encrypted backup was created locally but S3 upload failed"
      return 1
    fi
  fi

  date -u +%s > "$STATUS_FILE"
  find "$BACKUP_DIR" -type f \( -name '*.dump.enc' -o -name '*.dump.enc.sha256' \) \
    -mtime "+${RETENTION_DAYS}" -delete
  echo "Backup created: $(basename "$final")"
}

mkdir -p "$BACKUP_DIR"
while true; do
  run_backup || true
  sleep "$INTERVAL_SECONDS"
done
