/**
 * Los niveles y la XP (#309). Solo las reseñas dan XP; el servidor la cuenta y la manda
 * en `xp`, y acá se calcula lo que se dibuja con ella.
 *
 * La tabla es la misma de `Niveles.java`: si cambia una, cambia la otra.
 */

/** XP mínima de cada nivel: el 1 arranca en 0 y el 20 en 6.500. */
export const UMBRALES = [
  0, 100, 200, 400, 600, 800, 1000, 1100, 1300, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500,
  6000, 6500,
] as const

export const NIVEL_MAXIMO = UMBRALES.length

/** Lo que da una reseña con un comentario de verdad; es la unidad en que se cuenta "cuánto falta". */
export const XP_DE_RESENIA_COMPLETA = 100

/** El nivel de 1 a 20. La XP sigue sumando pasado el 20, pero el nivel no. */
export function nivelDe(xp: number): number {
  let nivel = 1
  UMBRALES.forEach((umbral, i) => {
    if (xp >= umbral) nivel = i + 1
  })
  return nivel
}

/** Cuánto del camino al siguiente nivel lleva hecho, de 0 a 1. En el 20 queda lleno. */
export function avanceDe(xp: number): number {
  const nivel = nivelDe(xp)
  if (nivel >= NIVEL_MAXIMO) return 1
  const desde = UMBRALES[nivel - 1]
  const hasta = UMBRALES[nivel]
  return (xp - desde) / (hasta - desde)
}

/** Cuánta XP le falta para el siguiente nivel, o null si ya está en el último. */
export function xpParaSiguiente(xp: number): number | null {
  const nivel = nivelDe(xp)
  return nivel >= NIVEL_MAXIMO ? null : UMBRALES[nivel] - xp
}
