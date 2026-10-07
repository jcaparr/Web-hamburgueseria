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

### Llevar los datos de desarrollo

La base arranca vacía. Volver a bajar los locales de Google gasta cuota (las fotos tienen
un tope de 1.000 por mes), así que conviene llevar los de la base de desarrollo. Se arma
el mismo par de archivos que deja `backup.sh`, desde la raíz del repo en la máquina de
desarrollo:

```bash
STAMP=$(date +%Y%m%d-%H%M%S)
mkdir -p deploy/backups
pg_dump -h localhost -U hamburguesas -d hamburguesas --no-owner --no-privileges | gzip > deploy/backups/db-$STAMP.sql.gz
tar -czf deploy/backups/photos-$STAMP.tar.gz -C backend/data place-photos rating-photos
```

En Windows, desde Git Bash: `pg_dump` no está en el PATH, está en
`"/c/Program Files/PostgreSQL/17/bin/pg_dump.exe"`, y la contraseña es la de
`application-dev.yml` (`PGPASSWORD=... pg_dump ...`).

Se copian al servidor (`scp`) y se restauran con `./restore.sh` (ver [Backups](#backups)).
Las cuentas de desarrollo viajan con la base: si hay alguna de prueba, conviene borrarla
antes de abrir el sitio.

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

## Ingreso con Google

Hace falta un **ID de cliente de OAuth** (tipo "Aplicación web") creado en Google Cloud
Console, en el mismo proyecto que la API key de Maps.

En **Orígenes autorizados de JavaScript** hay que listar cada dominio desde el que se
sirve la app. Sin eso, Google no dibuja el botón:

```
http://localhost:5173          (desarrollo)
https://tu-dominio             (producción)
```

El Client ID va en `GOOGLE_CLIENT_ID` dentro del `.env`. Es público: viaja dentro del
bundle del frontend, y por eso el `docker-compose` lo pasa como argumento de build y no
como variable de entorno del contenedor. El *client secret* no se usa: validamos el ID
token, no hacemos intercambio de código.

Vacío, el botón simplemente no aparece y el endpoint rechaza cualquier intento.

Mientras la app esté en modo "Prueba" en la pantalla de consentimiento, funciona igual
para cualquier cuenta, porque solo pedimos permisos básicos (email y perfil). Antes de
abrirla al público conviene completar la pantalla de consentimiento con el nombre real
de la app: es lo que ve el usuario al entrar. En la misma pantalla van los enlaces a
`https://tu-dominio/privacidad` y `https://tu-dominio/terminos`.

## Buscadores

La web ya publica `robots.txt` y `sitemap.xml`. El mapa del sitio lo arma el backend
con `SITE_URL`, así que tiene que ser la dirección pública exacta, con `https://`.

Para que Google la empiece a recorrer y avise de problemas:

1. Entrar a [Google Search Console](https://search.google.com/search-console) y agregar
   el dominio como **propiedad de dominio**.
2. Google da un registro TXT: se carga en el DNS del dominio (en Cloudflare, **DNS →
   Add record → TXT**) y se toca **Verificar**.
3. En **Sitemaps**, enviar `https://tu-dominio/sitemap.xml`.

Las páginas legales (`/terminos` y `/privacidad`) toman el nombre y el domicilio del
responsable de `frontend/src/utils/legal.ts`: completarlos antes del primer deploy.

## Actualizar

```bash
git pull
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

## Backups

`backup.sh` guarda la base y las fotos en `deploy/backups` y borra lo que pasa de 14 días.
Las fotos son de dos clases: las que bajan de Google y las que sube la gente con sus
reseñas. Las segundas no se pueden volver a conseguir de ningún lado, así que van las dos.
Un backup que vive en el mismo servidor que la base no sirve de mucho, así que conviene
configurar `RCLONE_REMOTE` para copiarlo afuera.

```bash
crontab -e
# 15 3 * * * /opt/hamburgueserias/deploy/backup.sh >> /var/log/hamburguesas-backup.log 2>&1
```

Para restaurar, la base y, si se pasan, las fotos del mismo momento:

```bash
./restore.sh backups/db-AAAAMMDD-HHMMSS.sql.gz backups/photos-AAAAMMDD-HHMMSS.tar.gz
```

**Probalo una vez antes de necesitarlo**, con un backup real, para saber que funciona.

## Probar el stack localmente

Sin dominio ni HTTPS, con un `.env` aparte para no tocar el de verdad: copiá
`.env.example` a, por ejemplo, `.env.prueba` y poné `SITE_ADDRESS=:80`,
`SITE_URL=http://localhost`, `MAIL_ENABLED=false` (los códigos salen en el log) y
`COMPOSE_PROJECT_NAME=prueba`, para que los volúmenes de la prueba no se mezclen con
otros.

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prueba up -d --build
curl http://localhost/api/burger-joints?size=1
```

`backup.sh` y `restore.sh` usan ese mismo archivo con `ENV_FILE=.env.prueba`. Al terminar,
`docker compose -f docker-compose.prod.yml --env-file .env.prueba down -v` borra todo lo
de la prueba, volúmenes incluidos.
