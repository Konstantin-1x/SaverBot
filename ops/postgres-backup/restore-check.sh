#!/bin/sh
set -eu

: "${BACKUP_ENCRYPTION_KEY:?BACKUP_ENCRYPTION_KEY is required}"

backup="${1:?Usage: restore-check.sh BACKUP_FILE}"
checksum="${backup}.sha256"
plain="/tmp/restore-check.dump"

test -f "$backup"
test -f "$checksum"
(cd "$(dirname "$backup")" && sha256sum -c "$(basename "$checksum")")
rm -f "$plain"
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 \
  -pass env:BACKUP_ENCRYPTION_KEY -in "$backup" -out "$plain"
pg_restore --list "$plain" >/dev/null
rm -f "$plain"
echo "Backup integrity and restore catalog are valid: $backup"
