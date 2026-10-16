# Carrusel de Instagram (#272)

Ocho láminas de 1080×1350 (4:5) con las funciones principales de Burgómetro. Estilo C
(amarillo tipográfico), con la plantilla que armó Juan.

| Lámina | Archivo | Qué muestra |
|---|---|---|
| 1 | `laminas/01-portada.png` | Portada, con Explorar |
| 2 | `laminas/02-sugerir-local.png` | Sugerir un local |
| 3 | `laminas/03-instalar.png` | Agregar a la pantalla de inicio (ilustración del menú de Chrome) |
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
