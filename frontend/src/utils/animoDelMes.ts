/**
 * Burgui con el ánimo de un mes, para la imagen que se comparte de "Tu mes" (#421). La aguja
 * de su cara hace de medidor: cuantas más reseñas, más arriba y más contento. Cambia de cara
 * cada cinco reseñas, para que haya una próxima a la que llegar.
 */
export type Animo = 'enojado' | 'serio' | 'contento' | 'euforico'

export function animoDelMes(resenias: number): Animo {
  if (resenias >= 15) return 'euforico'
  if (resenias >= 10) return 'contento'
  if (resenias >= 5) return 'serio'
  return 'enojado'
}

export const FRASE_DEL_ANIMO: Record<Animo, string> = {
  enojado: '¿Solo eso? Tengo hambre',
  serio: 'Vamos bien, pero quiero más',
  contento: '¡Así me gusta!',
  euforico: '¡Imparable!',
}

/** El cuadro del dibujo: deja lugar a los brazos en alto y a los papelitos. */
export const CUADRO_DE_BURGUI = { x: -44, y: -44, ancho: 218, alto: 244 }
