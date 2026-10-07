#!/usr/bin/env bash
# Dumps the database and the photos (the ones downloaded from Google and the ones people
# upload with their reviews) to ./backups, keeps the last N days, and copies them off
# the server when RCLONE_REMOTE is set.
#
# A backup that only exists on the machine it is backing up is not a backup: if the
# server or its disk goes, the backups go with it. See "Backups" in README.md for the
# off-server copy.
#
# Daily at 03:15, as root on the VPS:
#   15 3 * * * /opt/hamburgueserias/deploy/backup.sh >> /var/log/hamburguesas-backup.log 2>&1
set -euo pipefail

cd "$(dirname "$0")"

# A failing dump still creates the output file, and set -e aborts the script before
# any check below can run. Without this trap the directory ends up holding a 0-byte
# file that looks like a backup until the day you try to restore it. Once both local
# files are checked, a later failure (the upload, say) must not take them with it.
DUMP=""
PHOTOS=""
CIFRADO=""
LOCAL_OK=""
trap 'status=$?; rm -f "$CIFRADO"; if [ "$status" -ne 0 ] && [ -z "$LOCAL_OK" ]; then echo "!! Backup failed (exit $status), removing partial files" >&2; rm -f "$DUMP" "$PHOTOS"; fi' EXIT

# ENV_FILE lets you rehearse against a throwaway stack without touching the real .env.
ENV_FILE="${ENV_FILE:-.env}"
COMPOSE="docker compose -f docker-compose.prod.yml --env-file $ENV_FILE"
BACKUP_DIR="${BACKUP_DIR:-$(pwd)/backups}"
STAMP="$(date +%Y%m%d-%H%M%S)"

# shellcheck disable=SC1091
set -a; source "$ENV_FILE"; set +a

# After the .env, so it can set these too.
RETENTION_DAYS="${RETENTION_DAYS:-14}"
# Afuera se guarda lo mismo que adentro: la política de privacidad promete que lo que
# alguien borra desaparece de las copias a los 14 días.
REMOTE_RETENTION_DAYS="${REMOTE_RETENTION_DAYS:-$RETENTION_DAYS}"

# La copia de afuera se revisa antes de empezar: mal configurada tiene que fallar
# enseguida y a la vista, no quedar en un log que nadie lee mientras parece que anda.
if [ -n "${RCLONE_REMOTE:-}" ]; then
    # El dump tiene el mail y el hash de la contraseña de cada usuario: sin cifrar no sale.
    if [ -z "${BACKUP_AGE_RECIPIENT:-}" ]; then
        echo "!! RCLONE_REMOTE is set but BACKUP_AGE_RECIPIENT is empty: refusing to upload an unencrypted dump" >&2
        exit 1
    fi
    for tool in age rclone; do
        command -v "$tool" >/dev/null || { echo "!! $tool is not installed (apt install $tool)" >&2; exit 1; }
    done
fi

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
# Review photos are the one thing that cannot be downloaded again from anywhere.
$COMPOSE exec -T backend tar -czf - -C /data place-photos rating-photos > "$PHOTOS"
chmod 600 "$PHOTOS"

# Same reasoning as the dump: an empty archive is a failure that would otherwise
# only surface the day you need the photos back.
if [ ! -s "$PHOTOS" ]; then
    echo "!! Photo archive is empty, removing it" >&2
    rm -f "$PHOTOS"
    exit 1
fi
LOCAL_OK=1

echo "==> Deleting local backups older than $RETENTION_DAYS days"
find "$BACKUP_DIR" -name 'db-*.sql.gz' -mtime "+$RETENTION_DAYS" -delete
find "$BACKUP_DIR" -name 'photos-*.tar.gz' -mtime "+$RETENTION_DAYS" -delete

# --apparent-size, because a few-hundred-byte archive rounds to 0 with block sizes
# and makes a perfectly good backup look like a failed one.
echo "==> Local backup done: $(du -h --apparent-size "$DUMP" | cut -f1) database, $(du -h --apparent-size "$PHOTOS" | cut -f1) photos"

if [ -z "${RCLONE_REMOTE:-}" ]; then
    echo "!! RCLONE_REMOTE is not set: this backup only exists on this server" >&2
    exit 0
fi

# El dump sale cifrado con la clave pública. La privada no está en el servidor, así que
# ni quien lo tome por asalto ni el proveedor de afuera pueden leer los backups.
CIFRADO="$DUMP.age"
echo "==> Uploading the encrypted dump to $RCLONE_REMOTE/db"
age -r "$BACKUP_AGE_RECIPIENT" -o "$CIFRADO" "$DUMP"
rclone copy "$CIFRADO" "$RCLONE_REMOTE/db"
rm -f "$CIFRADO"

# Las fotos no viajan en el tar de cada día: con 225 MB y creciendo, catorce copias
# enteras se comían el espacio gratis. Se sincroniza la carpeta y sube solo lo nuevo.
# Van sin cifrar porque son públicas: cualquiera las ve en la web.
#
# Lo que se borra o se reemplaza acá no se pierde enseguida afuera: pasa a
# fotos/borradas/<día>, que se vacía a los REMOTE_RETENTION_DAYS días.
BACKEND_ID="$($COMPOSE ps -q backend)"
carpeta_de() {
    docker inspect -f "{{range .Mounts}}{{if eq .Destination \"$1\"}}{{.Source}}{{end}}{{end}}" "$BACKEND_ID"
}
HOY="${STAMP%%-*}"
for carpeta in place-photos rating-photos; do
    ORIGEN="$(carpeta_de "/data/$carpeta")"
    if [ -z "$ORIGEN" ] || [ ! -d "$ORIGEN" ]; then
        echo "!! Could not find the $carpeta volume on this host" >&2
        exit 1
    fi
    # Un volumen vacío (recreado por error, por ejemplo) mandaría todo a borradas, y a
    # los 14 días no quedaría ninguna foto en ningún lado.
    if [ -z "$(ls -A "$ORIGEN")" ] \
        && [ -n "$(rclone lsf "$RCLONE_REMOTE/fotos/actuales/$carpeta" 2>/dev/null | head -1)" ]; then
        echo "!! $carpeta is empty here but not off-server: refusing to sync. Check the volume." >&2
        exit 1
    fi
    echo "==> Syncing $carpeta to $RCLONE_REMOTE/fotos/actuales/$carpeta"
    rclone sync "$ORIGEN" "$RCLONE_REMOTE/fotos/actuales/$carpeta" \
        --backup-dir "$RCLONE_REMOTE/fotos/borradas/$HOY/$carpeta"
done

echo "==> Deleting off-server backups older than $REMOTE_RETENTION_DAYS days"
rclone delete --min-age "${REMOTE_RETENTION_DAYS}d" "$RCLONE_REMOTE/db"
# Lo que pasa a borradas conserva su fecha original, así que --min-age lo borraría al
# día siguiente: la edad sale del nombre de la carpeta, que es el día en que se movió.
LIMITE="$(date -d "$REMOTE_RETENTION_DAYS days ago" +%Y%m%d)"
for dia in $(rclone lsf --dirs-only "$RCLONE_REMOTE/fotos/borradas" 2>/dev/null); do
    dia="${dia%/}"
    if [[ "$dia" =~ ^[0-9]{8}$ ]] && [ "$dia" -lt "$LIMITE" ]; then
        rclone purge "$RCLONE_REMOTE/fotos/borradas/$dia"
    fi
done

echo "==> Off-server copy done"
