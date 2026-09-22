import { isAxiosError } from 'axios'

export function isSessionExpired(error: unknown): boolean {
  return (error as { response?: { status?: number } })?.response?.status === 401
}

export function isNotFound(error: unknown): boolean {
  return isAxiosError(error) && error.response?.status === 404
}

/**
 * Qué decir cuando una pantalla no pudo traer sus datos.
 *
 * Existe para que ninguna pantalla muestre su estado vacío cuando en realidad falló:
 * "Todavía no guardaste ninguna hamburguesería" es mentira si no pudimos preguntar,
 * y a quien sí tiene favoritos le hace creer que los perdió.
 *
 * Sin respuesta es que no llegamos al servidor —caído, reiniciándose en un deploy,
 * o sin conexión—, y vale la pena distinguirlo porque reintentar suele alcanzar.
 */
export function loadErrorMessage(error: unknown): string {
  if (isAxiosError(error) && !error.response) {
    return 'No pudimos conectarnos. Revisá tu conexión y probá de nuevo.'
  }
  return 'Algo salió mal al cargar. Probá de nuevo en un rato.'
}
