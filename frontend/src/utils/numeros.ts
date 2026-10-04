/*
 * Los números como se escriben acá: con coma decimal. "4.5" es como lo escribe un
 * sistema en inglés; en la Argentina se lee "4,5", que es además como lo muestra Maps.
 */

const UN_DECIMAL = new Intl.NumberFormat('es-AR', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})

const HASTA_UN_DECIMAL = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 })

/** Una nota o un promedio, siempre con un decimal: "4,0", "4,5". */
export function nota(valor: number): string {
  return UN_DECIMAL.format(valor)
}

/** Una distancia, con un decimal solo si hace falta: "3", "3,2". */
export function kilometros(valor: number): string {
  return HASTA_UN_DECIMAL.format(valor)
}
