#!/usr/bin/env bash
# Restores a dump produced by backup.sh, and optionally its photos:
#   ./restore.sh backups/db-20260919-031500.sql.gz [backups/photos-20260919-031500.tar.gz]
#
# This REPLACES the current database. The backend is stopped first so nothing writes
# to a half-restored schema, and started again at the end.
#
# Test this at least once before you need it. An untested backup is a guess.
set -euo pipefail

cd "$(dirname "$0")"

DUMP="${1:-}"
PHOTOS="${2:-}"
if [ -z "$DUMP" ] || [ ! -f "$DUMP" ] || { [ -n "$PHOTOS" ] && [ ! -f "$PHOTOS" ]; }; then
    echo "Usage: $0 <backups/db-XXXXXXXX-XXXXXX.sql.gz> [backups/photos-XXXXXXXX-XXXXXX.tar.gz]" >&2
    exit 1
fi

# ENV_FILE lets you rehearse against a throwaway stack without touching the real .env.
ENV_FILE="${ENV_FILE:-.env}"
COMPOSE="docker compose -f docker-compose.prod.yml --env-file $ENV_FILE"

# shellcheck disable=SC1091
set -a; source "$ENV_FILE"; set +a

echo "About to overwrite database '$POSTGRES_DB' with $DUMP."
read -r -p "Type the database name to confirm: " CONFIRM
if [ "$CONFIRM" != "$POSTGRES_DB" ]; then
    echo "Cancelled." >&2
    exit 1
fi

echo "==> Stopping backend"
$COMPOSE stop backend

echo "==> Recreating schema"
$COMPOSE exec -T db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 \
    -c "drop schema public cascade; create schema public;"

echo "==> Restoring"
gunzip -c "$DUMP" | $COMPOSE exec -T db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1

echo "==> Starting backend"
$COMPOSE start backend

# Into the running backend, so the files end up owned by the app user that serves them.
if [ -n "$PHOTOS" ]; then
    echo "==> Restoring photos from $PHOTOS"
    $COMPOSE exec -T backend tar -xzf - -C /data < "$PHOTOS"
fi

echo "==> Done. Check the logs: $COMPOSE logs -f backend"
