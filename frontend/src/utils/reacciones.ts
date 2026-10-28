import type { Reacciones, TipoDeReaccion } from '../types'

/**
 * Las reacciones, en el orden del selector, y cómo se nombran (#186).
 *
 * El dibujo de cada una es Burgómetro haciéndola (DibujoDeReaccion, #365). El nombre es
 * lo que lee el lector de pantalla y lo que aparece al dejar el mouse encima: un dibujo
 * solo no siempre se entiende igual.
 */
export const REACCIONES: { tipo: TipoDeReaccion; nombre: string }[] = [
  { tipo: 'HAMBRE', nombre: 'Me dio hambre' },
  { tipo: 'MARAVILLOSO', nombre: 'Maravilloso' },
  { tipo: 'ENCANTA', nombre: 'Me encanta' },
  { tipo: 'RISA', nombre: 'Me hizo reír' },
  { tipo: 'SORPRESA', nombre: 'Sorpresa' },
]

export function reaccion(tipo: TipoDeReaccion) {
  return REACCIONES.find((r) => r.tipo === tipo) ?? REACCIONES[0]
}

/**
 * Cómo quedan las reacciones si quien mira cambia la suya, antes de que conteste el
 * servidor: se le resta a la que tenía y se le suma a la nueva.
 *
 * Mismo orden que el servidor —de la más usada a la menos, y a igual cantidad el del
 * selector—, para que al llegar la respuesta nada salte de lugar.
 */
export function conMiReaccion(actuales: Reacciones, nueva: TipoDeReaccion | null): Reacciones {
  const cuantas = new Map(actuales.cuantas.map((c) => [c.tipo, c.cuantas]))
  if (actuales.mia) cuantas.set(actuales.mia, (cuantas.get(actuales.mia) ?? 1) - 1)
  if (nueva) cuantas.set(nueva, (cuantas.get(nueva) ?? 0) + 1)

  const orden = REACCIONES.map((r) => r.tipo)
  return {
    mia: nueva,
    cuantas: [...cuantas.entries()]
      .filter(([, n]) => n > 0)
      .map(([tipo, n]) => ({ tipo, cuantas: n }))
      .sort((a, b) => b.cuantas - a.cuantas || orden.indexOf(a.tipo) - orden.indexOf(b.tipo)),
  }
}
