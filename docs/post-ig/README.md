# Carrusel de Instagram (#272)

Ocho láminas en dos formatos: `laminas/` en 4:5 (1080×1350, el del carrusel del feed) y `laminas-9x16/` en 9:16 (1080×1920, para historias, con lo importante dentro de la zona segura: sin nada clave en los ~250 px de arriba y abajo). El celu se ve entero en las dos con las funciones principales de Burgómetro. Estilo C
(amarillo tipográfico), con la plantilla que armó Juan.

| Lámina | Archivo | Qué muestra |
|---|---|---|
| 1 | `laminas/01-portada.png` | Portada, con Explorar |
| 2 | `laminas/02-sugerir-local.png` | Sugerir un local |
| 3 | `laminas/03-instalar.png` | Instalar la web: Android (Chrome) «Instalar app», iPhone (Safari) «Agregar a inicio» (ilustración del menú de Chrome) |
| 4 | `laminas/04-recorrido.png` | Armar un recorrido (Tour) |
| 5 | `laminas/05-buscar-gente.png` | Buscar gente (nombres de ejemplo) |
| 6 | `laminas/06-avatar.png` | Armar tu hamburguesa |
| 7 | `laminas/07-guardadas.png` | Guardadas |
| 8 | `laminas/08-cierre.png` | Cierre con la dirección |

- `capturas/`: capturas de la web local a 375 px (2: modal de sugerir un local; el resto, las pantallas).
  Salen de la base de desarrollo; los nombres de la lámina 5 y el usuario de la 6 se reemplazaron por
  ejemplos, y se ocultaron los puntajes y los conteos.
- `fuentes/`: Poppins (OFL), la letra de las láminas.
- `generar.mjs`: arma el HTML de cada lámina y lo pasa a PNG con Edge. Los textos están en `LAMINAS` y `CIERRE`.
  `node docs/post-ig/generar.mjs`
- Logo: el isotipo es el de `frontend/src/components/MarcaBurgometro.tsx`; las láminas usan el horizontal
  con texto marrón sobre amarillo, y el vertical en el cierre.

Reglas del contenido: sin cifras de la web (reseñas, usuarios); el único número es «+1.200 hamburgueserías
en Buenos Aires y alrededores».

## Antes de publicar

Las láminas 3 asumen que el manifest (#281) ya está deployado: sin él, Chrome en Android solo ofrece
«Agregar a la pantalla principal», con un ícono común, y no «Instalar app». **No publiques el post antes del deploy del manifest.**
En iPhone (Safari) «Compartir > Agregar a inicio» ya funciona hoy. Los nombres son los del manifest: la app se llama «Burgómetro».
