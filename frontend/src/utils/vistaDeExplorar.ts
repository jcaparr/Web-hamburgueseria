/** Las vistas de Explorar (#395): la lista o el mapa. La app siempre abre en la lista (#415). */
export type Vista = 'lista' | 'mapa'

/** La dirección de cada vista: la lista es la raíz y el mapa la lleva en "?vista=mapa". */
export function direccionDeLaVista(vista: Vista): string {
  return vista === 'mapa' ? '/?vista=mapa' : '/'
}
