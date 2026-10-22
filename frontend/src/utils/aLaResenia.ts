/**
 * Llegar a una reseña puntual dentro de la ficha de su local (#260).
 *
 * Tocar una reseña en el feed o en un perfil llevaba a la ficha y dejaba arriba de todo:
 * había que buscarla entre las demás, y si era vieja ni siquiera estaba en la página.
 * El ancla dice cuál es; la ficha la trae si hace falta, baja hasta ella y la marca.
 */
import { rutaDelLocal } from './rutaDelLocal'

const PREFIJO = 'resenia-'

/** El id del elemento de la reseña en la ficha. */
export function anclaDeResenia(ratingId: number) {
  return `${PREFIJO}${ratingId}`
}

/** La dirección de la ficha, ya en esa reseña. */
export function aLaResenia(local: { id: number; slug: string | null }, ratingId: number) {
  return `${rutaDelLocal(local)}#${anclaDeResenia(ratingId)}`
}

/** La reseña que pide el ancla de la dirección, o null si no pide ninguna. */
export function reseniaDelAncla(hash: string): number | null {
  const encontrada = new RegExp(`^#${PREFIJO}(\\d+)$`).exec(hash)
  return encontrada ? Number(encontrada[1]) : null
}
