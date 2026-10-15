/**
 * A dónde ir después de iniciar sesión (#257).
 *
 * Quien manda a iniciar sesión puede dejar en el estado de la navegación a dónde volver:
 * `navigate('/login', { state: { volverA: '/?sugerir=1' } })`. Sin eso, al inicio.
 *
 * Solo direcciones de la propia web. No alcanza con mirar que empiece con "/": el
 * navegador borra tabs y saltos de línea y toma "\" como "/", así que "/\t/otro.com" o
 * "/\otro.com" llevarían a otro sitio. Se resuelve la dirección como lo haría el
 * navegador y se compara el origen.
 */
export function volverDespuesDeEntrar(estado: unknown): string {
  const volverA = (estado as { volverA?: unknown } | null)?.volverA
  if (typeof volverA !== 'string' || !volverA.startsWith('/')) return '/'
  try {
    const destino = new URL(volverA, window.location.origin)
    if (destino.origin !== window.location.origin) return '/'
    return destino.pathname + destino.search + destino.hash
  } catch {
    return '/'
  }
}

/**
 * Lo que hay que pasarle a la pantalla siguiente para no perder a dónde volver: de
 * iniciar sesión a crear cuenta, y de ahí a verificar el email.
 */
export function conVolverA(estado: unknown): { volverA?: string } {
  const volverA = (estado as { volverA?: unknown } | null)?.volverA
  return typeof volverA === 'string' ? { volverA } : {}
}
