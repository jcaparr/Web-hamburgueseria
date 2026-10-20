# Carrusel de Instagram (#272)

Diez láminas en dos formatos: `laminas/` en 4:5 (1080×1350, el del carrusel del feed) y `laminas-9x16/` en 9:16 (1080×1920, para historias, con lo importante dentro de la zona segura: sin nada clave en los ~250 px de arriba y abajo). El celu se ve entero en las dos con las funciones principales de Burgómetro. Estilo C
(amarillo tipográfico), con la plantilla que armó Juan.

| Lámina | Archivo | Qué muestra |
|---|---|---|
| 1 | `laminas/01-portada.png` | Portada, con Explorar |
| 2 | `laminas/02-sugerir-local.png` | Sugerir un local |
| 3a | `laminas/03a-instalar-android.png` | Android (Chrome): 3 puntitos de arriba > «Instalar y crear acceso directo» > «Instalar» (menú ilustrado en tema claro, con los nombres reales) |
| 3b | `laminas/03b-instalar-iphone.png` | iPhone (Safari): 3 puntitos de abajo > «Compartir» > «Agregar a Inicio» (menú y hoja de compartir ilustrados) |
| 4 | `laminas/04-recorrido.png` | Armar un recorrido (Tour) |
| 5 | `laminas/05-buscar-gente.png` | Buscar gente (nombres de ejemplo) |
| 6 | `laminas/06-avatar.png` | Armar tu hamburguesa |
| 7 | `laminas/07-guardadas.png` | Guardadas |
| 8 | `laminas/08-cierre.png` | Cierre con la dirección |
| 9 | `laminas/09-compartir-resenia.png` | «Compartir en historias» (#278): el botón en la reseña y la imagen que arma la web |

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

La lámina 9 muestra «Compartir en historias» (#278), ya en main; el local, el usuario y la reseña de la imagen son inventados y la imagen se generó con el mismo código de la web.

Las láminas 3a, 3b y 9 asumen que el manifest (#281) ya está deployado: sin él, Chrome en Android solo ofrece
«Agregar a la pantalla principal», con un ícono común, y no «Instalar». **No publiques el post antes del deploy del manifest.**
En iPhone (Safari) «Compartir > Agregar a inicio» ya funciona hoy. Los pasos de Android e iPhone los verificó Juan en sus celulares. Los pasos van en tarjetas fuera del celu, numerados en rojo con el mismo número en un aro rojo sobre la pantalla (sin flechas) (así no tapan la captura).
