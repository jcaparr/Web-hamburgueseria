import type { TuMes } from '../types'

/** "2026-11" → "noviembre". */
export function nombreDelMes(mes: string): string {
  const [anio, numero] = mes.split('-').map(Number)
  return new Date(anio, numero - 1, 1).toLocaleDateString('es-AR', { month: 'long' })
}

/** "2026-11" → 2026. */
export function anioDelMes(mes: string): number {
  return Number(mes.split('-')[0])
}

export type TipoDeTarjeta = 'portada' | 'resenias' | 'barrios' | 'mejor' | 'anio' | 'cierre'

/**
 * Qué tan nítida se ve la foto de fondo en cada tarjeta, de 0 (solo la desenfocada) a 1
 * (la foto entera). La foto se va revelando de la portada a la mejor nota y vuelve a
 * velarse un poco en el cierre. Es la opacidad de la capa nítida: no se anima el desenfoque.
 */
export const NITIDEZ: Record<TipoDeTarjeta, number> = {
  portada: 0,
  resenias: 0.25,
  barrios: 0.45,
  mejor: 1,
  anio: 0.4,
  cierre: 0.6,
}

/** Qué tarjetas lleva el resumen: la del año solo en diciembre, y la de la mejor nota si hay. */
export function tarjetasDe(resumen: TuMes): TipoDeTarjeta[] {
  const tarjetas: TipoDeTarjeta[] = ['portada', 'resenias']
  if (resumen.barrios.length > 0) tarjetas.push('barrios')
  if (resumen.mejor) tarjetas.push('mejor')
  if (resumen.anio) tarjetas.push('anio')
  tarjetas.push('cierre')
  return tarjetas
}
