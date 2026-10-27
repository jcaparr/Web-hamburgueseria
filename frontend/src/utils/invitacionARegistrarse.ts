/**
 * Cuándo se le muestra la invitación a registrarse a quien entra sin sesión (#346).
 *
 * Lo que se guarda es solo del navegador: si el almacenamiento falla (modo privado,
 * datos bloqueados), la invitación se comporta como si nadie la hubiera cerrado.
 */

const CLAVE_CIERRES = 'burgometro.invitacion'
const CLAVE_VISTA = 'burgometro.invitacion.vista'

const DIA = 24 * 60 * 60 * 1000
const DIAS_TRAS_UN_CIERRE = 7
const DIAS_TRAS_TRES_CIERRES = 30
const CIERRES_PARA_ESPERAR_MAS = 3

/** Segundos en la primera pantalla antes de mostrarla. */
export const SEGUNDOS_DE_ESPERA = 20

/** Pantallas donde ya se está entrando o creando la cuenta, o que son texto legal. */
const SIN_INVITACION = ['/login', '/register', '/verify-email', '/forgot-password', '/terminos', '/privacidad']

export function esPantallaSinInvitacion(pathname: string): boolean {
  return SIN_INVITACION.includes(pathname)
}

interface Cierres {
  cantidad: number
  hasta: number
}

function leerCierres(): Cierres | null {
  try {
    const crudo = localStorage.getItem(CLAVE_CIERRES)
    if (!crudo) return null
    const dato = JSON.parse(crudo) as Partial<Cierres>
    if (typeof dato.cantidad !== 'number' || typeof dato.hasta !== 'number') return null
    return { cantidad: dato.cantidad, hasta: dato.hasta }
  } catch {
    return null
  }
}

/** Si ya la cerró hace poco: 7 días, o 30 si la cerró tres veces. */
export function estaEnPausa(ahora = Date.now()): boolean {
  const cierres = leerCierres()
  return cierres !== null && ahora < cierres.hasta
}

export function registrarCierre(ahora = Date.now()) {
  const cantidad = (leerCierres()?.cantidad ?? 0) + 1
  const dias = cantidad >= CIERRES_PARA_ESPERAR_MAS ? DIAS_TRAS_TRES_CIERRES : DIAS_TRAS_UN_CIERRE
  try {
    localStorage.setItem(CLAVE_CIERRES, JSON.stringify({ cantidad, hasta: ahora + dias * DIA }))
  } catch {
    // Sin almacenamiento no hay dónde recordarlo: se cierra y listo.
  }
}

/** Una sola vez por visita, aunque se recargue la página. */
export function yaSeMostroEnEstaVisita(): boolean {
  try {
    return sessionStorage.getItem(CLAVE_VISTA) === '1'
  } catch {
    return false
  }
}

export function marcarMostradaEnEstaVisita() {
  try {
    sessionStorage.setItem(CLAVE_VISTA, '1')
  } catch {
    // Igual que arriba.
  }
}
