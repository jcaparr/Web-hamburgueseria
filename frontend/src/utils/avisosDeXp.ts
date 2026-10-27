/**
 * Cómo se enteran el aviso de XP y el resto de la app de que una reseña dio puntos (#312).
 *
 * Quien publica no dibuja nada: avisa con un evento y {@link AvisoDeXp}, montado una sola
 * vez en App, lo muestra cuando corresponde. Así el aviso no depende de en qué pantalla
 * pasó, y espera solo a que el modal de reseña se haya cerrado.
 */
export const EVENTO_DE_XP = 'burgometro:xp-ganada'

export interface XpGanada {
  /** Cuánta XP sumó la reseña. */
  ganada: number
  /** El nivel al que subió, o null si no subió. */
  nivelNuevo: number | null
}

export function avisarXpGanada(aviso: XpGanada) {
  window.dispatchEvent(new CustomEvent<XpGanada>(EVENTO_DE_XP, { detail: aviso }))
}
