#!/usr/bin/env bash
# Verifica las migraciones de Flyway contra un Postgres 17 limpio y real (no H2): levanta un
# contenedor vacío, arranca el backend con ddl-auto=validate (Hibernate comprueba que las
# entidades coinciden con el esquema que dejaron las migraciones) y revisa que todas se
# aplicaron sin fallos. Lo que hacen los tests con H2 no alcanza: la sintaxis propia de
# Postgres (regex, índices parciales, check) solo se ejecuta acá.
#
#   bash backend/scripts/verificar-migraciones.sh
#
# Variables (todas opcionales):
#   DOCKER   comando de docker. En esta PC corre dentro de WSL:
#            DOCKER="wsl -d ubuntu -u root -- docker"
#   PUERTO   puerto del Postgres de prueba (5434) y SERVER_PORT el del backend (8089).
#   MVN      comando de Maven (mvn).
set -euo pipefail

DOCKER="${DOCKER:-docker}"
PUERTO="${PUERTO:-5434}"
SERVER_PORT="${SERVER_PORT:-8089}"
MVN="${MVN:-mvn}"
NOMBRE="pg-verificar-migraciones"
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
LOG="$(mktemp)"

limpiar() {
    [ -n "${BACKEND_PID:-}" ] && kill "$BACKEND_PID" 2>/dev/null || true
    $DOCKER rm -f "$NOMBRE" >/dev/null 2>&1 || true
}
trap limpiar EXIT

echo "→ Postgres 17 limpio en el puerto $PUERTO"
$DOCKER rm -f "$NOMBRE" >/dev/null 2>&1 || true
$DOCKER run -d --name "$NOMBRE" -e POSTGRES_USER=verificar -e POSTGRES_PASSWORD=verificar \
    -e POSTGRES_DB=verificar -p "$PUERTO:5432" postgres:17-alpine >/dev/null
for _ in $(seq 1 30); do
    $DOCKER exec "$NOMBRE" pg_isready -U verificar >/dev/null 2>&1 && break
    sleep 1
done

echo "→ Arrancando el backend con ddl-auto=validate (las migraciones se aplican desde cero)"
cd "$RAIZ"
DATABASE_URL="jdbc:postgresql://localhost:$PUERTO/verificar" DB_USER=verificar DB_PASSWORD=verificar \
    SERVER_PORT="$SERVER_PORT" SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
    $MVN -B -o spring-boot:run >"$LOG" 2>&1 &
BACKEND_PID=$!

for _ in $(seq 1 90); do
    grep -qE "Started HamburguesasApplication|APPLICATION FAILED|BUILD FAILURE" "$LOG" && break
    sleep 2
done

if ! grep -q "Started HamburguesasApplication" "$LOG"; then
    echo "✗ El backend no arrancó. Últimas líneas:"; tail -25 "$LOG"; exit 1
fi

APLICADAS=$($DOCKER exec "$NOMBRE" psql -U verificar -At -c "select count(*) from flyway_schema_history where success")
FALLADAS=$($DOCKER exec "$NOMBRE" psql -U verificar -At -c "select count(*) from flyway_schema_history where not success")
ULTIMA=$($DOCKER exec "$NOMBRE" psql -U verificar -At -c "select version||' '||description from flyway_schema_history order by installed_rank desc limit 1")
echo "→ Migraciones aplicadas: $APLICADAS, fallidas: $FALLADAS, última: $ULTIMA"
[ "$FALLADAS" = "0" ] || { echo "✗ Hay migraciones fallidas"; exit 1; }
echo "✓ Esquema válido: Flyway aplicó todo desde cero y Hibernate (validate) lo aceptó."
