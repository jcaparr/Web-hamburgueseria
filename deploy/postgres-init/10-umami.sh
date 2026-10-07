#!/bin/sh
# La base y el usuario de Umami, las estadísticas de visitas.
#
# Postgres corre lo que hay en /docker-entrypoint-initdb.d una sola vez: cuando arranca
# con el volumen de datos vacío. Si la base ya existía, esto no corre y hay que crearlas
# a mano con los mismos dos comandos (ver deploy/README.md).
#
# Aparte de la base de la app y con su propio usuario, que ni siquiera puede conectarse
# a la de la app: si alguna vez Umami tuviera un agujero, por ahí no se llega a los
# mails ni a las contraseñas de nadie.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<EOSQL
CREATE ROLE umami LOGIN PASSWORD '${UMAMI_DB_PASSWORD}';
CREATE DATABASE umami OWNER umami;
REVOKE CONNECT ON DATABASE "${POSTGRES_DB}" FROM PUBLIC;
EOSQL
