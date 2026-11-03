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

/**
 * Los colores de cada tarjeta. Son fijos y no cambian con el tema: la tarjeta es una
 * pieza propia, como una historia, y así el contraste es el mismo en claro y en oscuro.
 * Cada par se midió con la fórmula de WCAG: todos pasan 4,5 : 1 (de 4,88 a 12,42).
 */
export const FONDOS = {
  portada: { fondo: '#b23a2e', texto: '#fffdf8' },
  resenias: { fondo: '#f2b705', texto: '#3a2a1e' },
  barrios: { fondo: '#2f4f23', texto: '#f7efd8' },
  mejor: { fondo: '#3a2a1e', texto: '#f3e8cf' },
  anio: { fondo: '#2c545d', texto: '#f7efd8' },
  cierre: { fondo: '#faf3e3', texto: '#3a2a1e' },
} as const

export type TipoDeTarjeta = keyof typeof FONDOS

/** Qué tarjetas lleva el resumen: la del año solo en diciembre, y la de la mejor nota si hay. */
export function tarjetasDe(resumen: TuMes): TipoDeTarjeta[] {
  const tarjetas: TipoDeTarjeta[] = ['portada', 'resenias']
  if (resumen.barrios.length > 0) tarjetas.push('barrios')
  if (resumen.mejor) tarjetas.push('mejor')
  if (resumen.anio) tarjetas.push('anio')
  tarjetas.push('cierre')
  return tarjetas
}
