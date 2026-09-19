#!/usr/bin/env bash
# Dumps the database (and the downloaded photos) to ./backups, keeps the last N days,
# and optionally copies them off the server.
#
# A backup that only exists on the machine it is backing up is not a backup, so set
# RCLONE_REMOTE (for example "r2:hamburguesas-backups") once you have somewhere to put it.
#
# Daily at 03:15, as root on the VPS:
#   15 3 * * * /opt/hamburgueserias/deploy/backup.sh >> /var/log/hamburguesas-backup.log 2>&1
set -euo pipefail

cd "$(dirname "$0")"

# A failing dump still creates the output file, and set -e aborts the script before
# any check below can run. Without this trap the directory ends up holding a 0-byte
# file that looks like a backup until the day you try to restore it.
DUMP=""
PHOTOS=""
trap 'status=$?; if [ "$status" -ne 0 ]; then echo "!! Backup failed (exit $status), removing partial files" >&2; rm -f "$DUMP" "$PHOTOS"; fi' EXIT

COMPOSE="docker compose -f docker-compose.prod.yml --env-file .env"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
BACKUP_DIR="${BACKUP_DIR:-$(pwd)/backups}"
STAMP="$(date +%Y%m%d-%H%M%S)"

# shellcheck disable=SC1091
set -a; source .env; set +a

mkdir -p "$BACKUP_DIR"
# The dump can contain every user's email: keep it unreadable to anyone else.
chmod 700 "$BACKUP_DIR"

DUMP="$BACKUP_DIR/db-$STAMP.sql.gz"
echo "==> Dumping database to $DUMP"
$COMPOSE exec -T db pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-privileges \
    | gzip > "$DUMP"
chmod 600 "$DUMP"

# An empty dump means the command failed somewhere in the pipe: fail loudly now
# rather than at restore time.
if [ ! -s "$DUMP" ]; then
    echo "!! Dump is empty, removing it" >&2
    rm -f "$DUMP"
    exit 1
fi

PHOTOS="$BACKUP_DIR/photos-$STAMP.tar.gz"
echo "==> Archiving photos to $PHOTOS"
$COMPOSE exec -T backend tar -czf - -C /data place-photos > "$PHOTOS"
chmod 600 "$PHOTOS"

# Same reasoning as the dump: an empty archive is a failure that would otherwise
# only surface the day you need the photos back.
if [ ! -s "$PHOTOS" ]; then
    echo "!! Photo archive is empty, removing it" >&2
    rm -f "$PHOTOS"
    exit 1
fi

if [ -n "${RCLONE_REMOTE:-}" ]; then
    echo "==> Uploading to $RCLONE_REMOTE"
    rclone copy "$DUMP" "$RCLONE_REMOTE"
    rclone copy "$PHOTOS" "$RCLONE_REMOTE"
fi

echo "==> Deleting local backups older than $RETENTION_DAYS days"
find "$BACKUP_DIR" -name 'db-*.sql.gz' -mtime "+$RETENTION_DAYS" -delete
find "$BACKUP_DIR" -name 'photos-*.tar.gz' -mtime "+$RETENTION_DAYS" -delete

# --apparent-size, because a few-hundred-byte archive rounds to 0 with block sizes
# and makes a perfectly good backup look like a failed one.
echo "==> Done: $(du -h --apparent-size "$DUMP" | cut -f1) database, $(du -h --apparent-size "$PHOTOS" | cut -f1) photos"
