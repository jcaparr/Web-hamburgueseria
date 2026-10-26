/**
 * Los niveles y la XP (#309). Solo las reseñas dan XP; el servidor la cuenta y la manda
 * en `xp`, y acá se calcula lo que se dibuja con ella.
 *
 * La tabla y las reglas son las de `Niveles.java`: si cambia una, cambia la otra.
 */

/** XP mínima de cada nivel: el 1 arranca en 0 y el 20 en 6.500. */
export const UMBRALES = [
  0, 100, 200, 400, 600, 800, 1000, 1100, 1300, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500,
  6000, 6500,
] as const

export const NIVEL_MAXIMO = UMBRALES.length

/** Lo que da una reseña con solo la foto. */
export const XP_CON_FOTO = 70
/** Lo que da una reseña con un comentario de verdad; es la unidad en que se cuenta "cuánto falta". */
export const XP_CON_COMENTARIO = 100
/** Se suma una vez a quien tiene al menos una reseña. */
export const XP_DE_BIENVENIDA = 30

/** Un comentario cuenta desde estas letras y estas palabras, las dos cosas juntas. */
export const LETRAS_MINIMAS = 80
export const PALABRAS_MINIMAS = 10

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

/** Cuántas reseñas completas (100 XP) hacen falta para juntar esa XP, redondeando para arriba. */
export function reseniasParaJuntar(xp: number): number {
  return Math.ceil(xp / XP_CON_COMENTARIO)
}

/** Letras y palabras de un comentario, con la misma cuenta que hace el servidor. */
export function medirComentario(texto: string): { letras: number; palabras: number } {
  const letras = (texto.match(/\p{L}/gu) ?? []).length
  const limpio = texto.trim()
  // Solo espacios ASCII entre palabras, como el servidor: un espacio no separable no suma una palabra.
  return { letras, palabras: limpio === '' ? 0 : limpio.split(/[ 	
]+/).length }
}

/** Cuánta XP da una reseña con este comentario (puede estar vacío). */
export function xpDeComentario(texto: string | null | undefined): number {
  const { letras, palabras } = medirComentario(texto ?? '')
  return letras >= LETRAS_MINIMAS && palabras >= PALABRAS_MINIMAS ? XP_CON_COMENTARIO : XP_CON_FOTO
}

/** La XP que queda al sacar una reseña: sin ninguna más, se va también la bienvenida. */
export function xpSinLaResenia(xpTotal: number, xpDeEsa: number): number {
  const resto = xpTotal - xpDeEsa
  return resto <= XP_DE_BIENVENIDA ? 0 : resto
}
