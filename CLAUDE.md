# Reglas del código (aprendidas de errores reales)

## Fechas y tareas programadas
- Para "el lunes de la semana pasada" usar `previousOrSame(...)` y restar semanas; `previous(...)` da la semana en curso cualquier día que no sea ese. Todo cálculo de período se prueba con un test que recorra los siete días de la semana, no solo con el día en que corre la tarea (#354).
- Una tarea que además corre al arrancar el servidor (recuperar cierres perdidos) tiene que ser correcta cualquier día y a cualquier hora, y repetirla no debe hacer nada. Si no, un deploy a mitad de semana la rompe.
- Las fechas de negocio van en hora de Buenos Aires (`America/Argentina/Buenos_Aires`); los períodos son `[desde, hasta)` y se prueban en el borde (domingo 23:30 y lunes 00:30).

## Consultas y rendimiento
- Ningún endpoint que se llama a cada visita lee una tabla entera para filtrarla en Java. Se filtra en la consulta (por fecha, por usuario) o se usa un dato ya guardado (como `users.xp`).
- Si una cuenta tiene un dato calculado y guardado, las pantallas usan el guardado y el cálculo queda en un solo lugar.

## Listas de personas
- Todo listado o ranking de personas pasa por `Bloqueos.queNoPuedeVer` y tiene un test con un bloqueo en cada sentido.

## Tests
- Las tres tandas, siempre: `mvn -B -o test` (backend) y `npm run lint && npx tsc -b && npm run build` (frontend).
- Cada logro o cierre automático tiene test del caso feliz, del borde de período y de "no vuelve a darse".
