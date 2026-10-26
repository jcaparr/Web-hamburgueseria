/**
 * Cómo se entera el aviso de logros de que pasó algo que puede haber dado uno (#317).
 *
 * Quien hace la acción no dibuja nada: avisa con un evento y {@link AvisoDeLogro}, montado
 * una vez en App, pide las novedades y las muestra cuando corresponde.
 */
export const EVENTO_REVISAR_LOGROS = 'burgometro:revisar-logros'

export function revisarLogros() {
  window.dispatchEvent(new Event(EVENTO_REVISAR_LOGROS))
}

/** Las acciones propias que pueden dar un logro: reseña, recorrido, guardada y hamburguesa. */
const ACCIONES_QUE_DAN_LOGROS = /^\/(burger-joints\/[^/]+\/ratings|tours|wishlist\/|profile\/hamburguesa)/
const METODOS_QUE_CAMBIAN = new Set(['post', 'put', 'delete'])

export function esAccionQueDaLogros(metodo: string | undefined, url: string | undefined): boolean {
  return METODOS_QUE_CAMBIAN.has((metodo ?? '').toLowerCase()) && ACCIONES_QUE_DAN_LOGROS.test(url ?? '')
}
