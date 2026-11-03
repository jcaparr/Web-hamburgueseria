/**
 * La vista de Explorar que se eligió la última vez (#395): la lista o el mapa. Se recuerda en este
 * navegador para abrir directo donde se quedó.
 *
 * Es una comodidad: si el almacenamiento no anda (ventana privada, datos bloqueados), Explorar abre
 * en la lista y no pasa nada más.
 */
export type Vista = 'lista' | 'mapa'

const CLAVE = 'explorar.vista'

export function leerVistaGuardada(): Vista | null {
  try {
    const guardada = window.localStorage.getItem(CLAVE)
    return guardada === 'mapa' || guardada === 'lista' ? guardada : null
  } catch {
    return null
  }
}

export function guardarVista(vista: Vista) {
  try {
    window.localStorage.setItem(CLAVE, vista)
  } catch {
    // Sin almacenamiento se abre en la lista cada vez, nada más.
  }
}

/** La dirección de cada vista: la lista es la raíz y el mapa la lleva en "?vista=mapa". */
export function direccionDeLaVista(vista: Vista): string {
  return vista === 'mapa' ? '/?vista=mapa' : '/'
}
