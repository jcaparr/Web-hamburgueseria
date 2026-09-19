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

## Email

Los códigos de verificación y de recuperación de contraseña salen por SMTP de Gmail,
desde una cuenta dedicada a la app. Hace falta:

1. Activar la **verificación en dos pasos** en esa cuenta.
2. Generar una **contraseña de aplicación** en https://myaccount.google.com/apppasswords
   (la contraseña normal de la cuenta no funciona por SMTP).
3. Pegarla en `SPRING_MAIL_PASSWORD` dentro del `.env`.

El límite es de unos 500 mails por día, de sobra para arrancar.

Como todavía no hay dominio propio, los mails salen desde una dirección `@gmail.com`
y pueden caer en spam. Por eso todos los mensajes de la app avisan de revisar esa
carpeta. Cuando haya dominio, conviene pasar a un proveedor con SPF y DKIM propios:
se cambia solo la configuración, el código queda igual.

Si `SPRING_MAIL_HOST` queda vacío no se envía nada y los códigos se escriben en el
log. Sirve para desarrollo, **nunca para producción**: cualquiera con acceso a los
logs podría activar cuentas ajenas.

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
