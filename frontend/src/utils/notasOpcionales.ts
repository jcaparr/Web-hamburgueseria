import type { DesgloseDeNotas } from '../types'

/** Una de las tres notas opcionales (#336) que alguien puso en este local. */
export interface NotaConDatos {
  rotulo: 'Local' | 'Atención' | 'Precio'
  promedio: number
  votos: number
}

/**
 * Las notas de local, atención y precio que tienen datos, en ese orden.
 *
 * Sin datos no se muestra: un recuadro con guiones sería ruido justo donde hay que
 * decidir. Lo usan la ficha (los relojitos) y la ventana de reseñar (#337), para que las
 * dos digan lo mismo.
 */
export function notasConDatos(desglose: DesgloseDeNotas | null): NotaConDatos[] {
  if (!desglose) return []
  const todas: [NotaConDatos['rotulo'], number | null, number][] = [
    ['Local', desglose.local, desglose.cuantasLocal],
    ['Atención', desglose.atencion, desglose.cuantasAtencion],
    ['Precio', desglose.precio, desglose.cuantasPrecio],
  ]
  return todas.flatMap(([rotulo, promedio, votos]) =>
    promedio != null && votos > 0 ? [{ rotulo, promedio, votos }] : [],
  )
}

/** Cómo se lee un promedio: de 4,5 para arriba es excelente, y de ahí baja de a medio punto. */
export function veredicto(promedio: number): string {
  if (promedio >= 4.5) return 'Excelente'
  if (promedio >= 4) return 'Muy bueno'
  if (promedio >= 3.5) return 'Bueno'
  return 'Regular'
}
