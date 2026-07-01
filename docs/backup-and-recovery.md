# PostgreSQL Backup and Recovery

The `postgres-backup` service creates an encrypted backup immediately after startup and then every `BACKUP_INTERVAL_SECONDS`.

Each backup is:

1. produced by `pg_dump --format=custom`;
2. validated with `pg_restore --list`;
3. encrypted with AES-256-CBC and PBKDF2 using `BACKUP_ENCRYPTION_KEY`;
4. accompanied by a SHA-256 checksum;
5. retained for `BACKUP_RETENTION_DAYS`.

Backups are stored in the `postgres-backups` Docker volume. Keep a second copy outside the Docker host. A local Docker volume protects against logical corruption but not against loss of the entire server.

For automatic off-host copies, configure `BACKUP_S3_BUCKET` and the
`BACKUP_S3_*` credentials. AWS S3 and S3-compatible providers are supported
through `rclone`. When off-host upload is configured, a failed upload triggers
an alert and the backup health timestamp is not advanced.

## Verify the newest backup

```bash
docker exec video-bot-postgres-backup sh -c \
  'latest=$(ls -1t /backups/*.dump.enc | head -1); restore-check.sh "$latest"'
```

## Copy backups off the host

```bash
docker run --rm \
  -v jackpotsaverbot_postgres-backups:/from:ro \
  -v "$PWD/backups:/to" \
  alpine cp -a /from/. /to/
```

Store this copy in encrypted object storage with versioning enabled.

## Full restore drill

Never overwrite production during a drill.

```bash
docker run --rm \
  --entrypoint sh \
  --network jackpotsaverbot_default \
  -v jackpotsaverbot_postgres-backups:/backups:ro \
  -e BACKUP_ENCRYPTION_KEY \
  -e PGPASSWORD="$DB_PASSWORD" \
  jackpotsaverbot-postgres-backup -c '
    latest=$(ls -1t /backups/*.dump.enc | head -1)
    openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 \
      -pass env:BACKUP_ENCRYPTION_KEY -in "$latest" -out /tmp/restore.dump
    createdb -h postgres -U jackpot jackpot_restore_test
    pg_restore -h postgres -U jackpot -d jackpot_restore_test --no-owner --no-acl /tmp/restore.dump
    psql -h postgres -U jackpot -d jackpot_restore_test -c "select count(*) from flyway_schema_history"
    dropdb -h postgres -U jackpot jackpot_restore_test
  '
```

Run a restore drill after schema changes and at least monthly.

## Alerts

Set either:

- `ALERT_TELEGRAM_CHAT_ID` to receive alerts through this bot;
- `ALERT_WEBHOOK_URL` for an external webhook receiver.

The monitor alerts after three consecutive failed checks and sends a recovery notification. Backup failures alert immediately.
