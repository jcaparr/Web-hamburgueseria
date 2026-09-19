# Deploy

Toda la aplicación corre en un solo servidor con Docker Compose: Caddy sirve el frontend
y hace de proxy a `/api`, Spring Boot corre el backend y Postgres guarda los datos.
Caddy consigue y renueva el certificado HTTPS solo.

```
internet ──▶ caddy :80/:443 ──┬──▶ /api/*  ──▶ backend :8080 ──▶ db :5432
                              └──▶ resto   ──▶ archivos estáticos del frontend
```

Ni el backend ni la base publican puertos: solo se llega a ellos desde adentro de la red
de Docker. La única puerta de entrada es Caddy.

## Primera vez

1. **Un hostname.** Sin dominio, un subdominio gratis de [DuckDNS](https://www.duckdns.org)
   alcanza. Apuntalo a la IP del servidor.
2. **Abrir los puertos 80 y 443** en el firewall del proveedor y en el del sistema.
   En Oracle Cloud hay que hacerlo en la *security list* además de en el `iptables` local.
3. **Configurar el entorno:**
   ```bash
   cd deploy
   cp .env.example .env && chmod 600 .env
   nano .env   # generar las claves con los comandos que están en los comentarios
   ```
4. **Levantar todo:**
   ```bash
   docker compose -f docker-compose.prod.yml --env-file .env up -d --build
   ```
5. **Mirar los logs** hasta ver que el backend arrancó y que Caddy sacó el certificado:
   ```bash
   docker compose -f docker-compose.prod.yml logs -f
   ```

Flyway crea el esquema en el primer arranque. Si faltara alguna variable de entorno,
el backend no arranca: eso es a propósito, para que nada quede corriendo con un valor
por defecto.

## Actualizar

```bash
git pull
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

## Backups

`backup.sh` guarda la base y las fotos en `deploy/backups` y borra lo que pasa de 14 días.
Un backup que vive en el mismo servidor que la base no sirve de mucho, así que conviene
configurar `RCLONE_REMOTE` para copiarlo afuera.

```bash
crontab -e
# 15 3 * * * /opt/hamburgueserias/deploy/backup.sh >> /var/log/hamburguesas-backup.log 2>&1
```

Para restaurar: `./restore.sh backups/db-AAAAMMDD-HHMMSS.sql.gz`. **Probalo una vez antes
de necesitarlo**, con un backup real, para saber que funciona.

## Probar el stack localmente

Sin dominio ni HTTPS, poniendo `SITE_ADDRESS=:80` en el `.env`:

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
curl http://localhost/api/burger-joints?size=1
```
