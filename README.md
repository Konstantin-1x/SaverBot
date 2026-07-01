# JackpotSaverBot

Production-oriented Telegram bot for downloading public videos from YouTube, YouTube Shorts, Instagram and TikTok.

## Stack

- Java 17
- Spring Boot 3.5
- PostgreSQL
- Spring Data JPA
- Flyway
- Telegram Bot API over long polling
- `yt-dlp` and FFmpeg for video downloading
- Docker Compose
- Encrypted PostgreSQL backups and external health alerts

## Run Locally

1. Copy `.env.example` to `.env`.
2. Set `BOT_TOKEN`, `BOT_USERNAME`, strong unique values for `DB_PASSWORD`,
   `DATA_HASH_KEY`, `BACKUP_ENCRYPTION_KEY`, and optional comma-separated
   `ADMIN_TELEGRAM_IDS`.
3. Build the application:

```bash
./gradlew clean test bootJar
```

4. Start the project:

```bash
docker compose up --build
```

The application applies Flyway migrations, stores downloaded files in `./storage`, and exposes health and Prometheus endpoints inside the Docker network.

```bash
docker compose ps
docker exec video-bot-app curl -fsS http://localhost:8080/actuator/health/readiness
docker exec video-bot-app curl -fsS http://localhost:8080/actuator/prometheus
```

PostgreSQL and Actuator are not published to host ports by default.

The backup container creates an encrypted, validated PostgreSQL backup on
startup and daily afterwards. See [docs/backup-and-recovery.md](docs/backup-and-recovery.md)
for restore verification, off-host copies and alert configuration.

## User Commands

- `/start` - greeting and short instruction.
- `/help` - usage help.
- `/language` - interface language selection.

The normal user flow has no main menu, no Back button, no audio mode and no history view.

## Admin Commands

Admin access is granted by Telegram IDs from `ADMIN_TELEGRAM_IDS`.

- `/admin_stats`
- `/block <telegram_id>`
- `/unblock <telegram_id>`
- `/platforms <YOUTUBE|YOUTUBE_SHORTS|INSTAGRAM|TIKTOK> <on|off>`
- `/errors`

## Configuration

Important environment variables:

- `BOT_TOKEN`
- `BOT_USERNAME`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DOWNLOAD_MAX_PARALLEL_JOBS`
- `DOWNLOAD_MAX_FILE_SIZE_MB`
- `DOWNLOAD_FILE_LIFETIME_HOURS`
- `DOWNLOAD_MAX_ATTEMPTS`
- `DOWNLOAD_RETRY_DELAY_SECONDS`
- `LIMIT_REQUEST_COOLDOWN_SECONDS`
- `LIMIT_DOWNLOADS_PER_DAY`
- `YTDLP_COMMAND`
- `BOT_CONNECT_TIMEOUT_SECONDS`
- `BOT_RESPONSE_TIMEOUT_SECONDS`
- `BOT_MAX_RETRIES`
- `BOT_POLLING_UNHEALTHY_AFTER_SECONDS`
- `DATA_HASH_KEY`
- `BACKUP_ENCRYPTION_KEY`
- `BACKUP_INTERVAL_SECONDS`
- `BACKUP_RETENTION_DAYS`
- `ALERT_TELEGRAM_CHAT_ID`
- `ALERT_WEBHOOK_URL`
- `BACKUP_S3_BUCKET`
- `BACKUP_S3_ACCESS_KEY_ID`
- `BACKUP_S3_SECRET_ACCESS_KEY`

## Verification

```bash
./gradlew clean test
```

PostgreSQL integration tests use Testcontainers and require a running Docker daemon.
