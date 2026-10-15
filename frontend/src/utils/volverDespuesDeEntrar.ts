/**
 * A dónde ir después de iniciar sesión (#257).
 *
 * Quien manda a iniciar sesión puede dejar en el estado de la navegación a dónde volver:
 * `navigate('/login', { state: { volverA: '/?sugerir=1' } })`. Sin eso, al inicio.
 *
 * Solo direcciones de la propia web: una que empiece con "//" o con un esquema llevaría
 * a otro sitio, y el estado se puede armar desde afuera.
 */
export function volverDespuesDeEntrar(estado: unknown): string {
  const volverA = (estado as { volverA?: unknown } | null)?.volverA
  if (typeof volverA === 'string' && volverA.startsWith('/') && !volverA.startsWith('//')) {
    return volverA
  }
  return '/'
}
