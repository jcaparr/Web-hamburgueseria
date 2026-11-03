/**
 * El color de una nota en el mapa (#395): verde de 4,5 para arriba, cheddar de 4,0 a 4,4 y
 * neutro por debajo. El color refuerza el número, nunca lo reemplaza: siempre se escribe la nota.
 *
 * Devuelve la clase de index.css (.nota-alta, .nota-media o .nota-baja), que usan los puntos del
 * mapa y la tarjeta.
 */
export function claseDeNota(nota: number): 'nota-alta' | 'nota-media' | 'nota-baja' {
  if (nota >= 4.5) return 'nota-alta'
  if (nota >= 4) return 'nota-media'
  return 'nota-baja'
}
