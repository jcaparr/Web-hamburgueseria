/**
 * El color de una nota en el mapa (#395): verde de 4,5 para arriba, cheddar de 4,0 a 4,4 y
 * naranja tostado por debajo (no gris: una nota baja también es una nota, y tiene que verse). El color refuerza el número, nunca lo reemplaza: siempre se escribe la nota.
 *
 * Devuelve la clase de index.css (.nota-alta, .nota-media o .nota-baja), que usan los puntos del
 * mapa y la tarjeta.
 */
export function claseDeNota(nota: number): 'nota-alta' | 'nota-media' | 'nota-baja' {
  // Se compara la nota como se escribe (un decimal): un 4,46 se lee "4,5" y tiene que pintarse verde.
  const escrita = Math.round(nota * 10) / 10
  if (escrita >= 4.5) return 'nota-alta'
  if (escrita >= 4) return 'nota-media'
  return 'nota-baja'
}

/**
 * El color del arco del medidor en la tarjeta del mapa, con los mismos rangos que las pastillas: verde,
 * cheddar o naranja tostado. Son variables del tema, así que cambian solas de claro a oscuro.
 */
export function colorDelArco(nota: number): string {
  if (nota >= 4.5) return 'var(--color-success)'
  if (nota >= 4) return 'var(--color-secondary)'
  return 'var(--nota-baja)'
}
