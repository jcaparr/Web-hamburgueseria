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

/** "2026-01" → "2025-12". */
export function mesAnterior(mes: string): string {
  const [anio, numero] = mes.split('-').map(Number)
  return numero === 1 ? `${anio - 1}-12` : `${anio}-${String(numero - 1).padStart(2, '0')}`
}

/** De qué período habla el resumen: "octubre", o "2026" en el recuento anual. */
export function periodoDe(resumen: TuMes): string {
  return resumen.anual ? String(anioDelMes(resumen.mes)) : nombreDelMes(resumen.mes)
}

/** El nombre de un mes por su número, de 1 a 12. */
export function nombreDelMesNumero(numero: number): string {
  return nombreDelMes(`2000-${String(numero).padStart(2, '0')}`)
}

const DIAS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo']

/** De 1 (lunes) a 7 (domingo) → "jueves". */
export function nombreDelDia(numero: number): string {
  return DIAS[numero - 1] ?? DIAS[0]
}

const FRANJAS: Record<string, string> = {
  madrugada: 'de madrugada',
  mañana: 'a la mañana',
  tarde: 'a la tarde',
  noche: 'a la noche',
}

export function franjaEnPalabras(franja: string): string {
  return FRANJAS[franja] ?? franja
}

/**
 * La línea que compara con el período anterior, o null si no hay nada que decir. En el
 * resumen de un mes se dice siempre (con cero, que antes no había reseñado); en el
 * recuento anual solo si el año anterior tiene datos.
 */
export function comparacionCon(resumen: TuMes): string | null {
  const antes = resumen.reseniasAnteriores
  const ahora = resumen.resenias
  const cuando = resumen.anual ? String(anioDelMes(resumen.mes) - 1) : nombreDelMes(mesAnterior(resumen.mes))
  if (antes === 0) return resumen.anual ? null : `En ${cuando} no habías reseñado.`
  if (ahora === antes) return `Igual que en ${cuando}.`
  if (ahora > antes) return `${ahora - antes} más que en ${cuando}.`
  return `${antes - ahora} menos que en ${cuando}, pero acá seguimos.`
}

export type TipoDeTarjeta =
  | 'portada' | 'resenias' | 'mesActivo' | 'dia' | 'barrios' | 'mejor'
  | 'logros' | 'podio' | 'reacciones' | 'nivel' | 'cierre'

/**
 * Qué tan prendidos están los rayos del fondo en cada tarjeta, de 0 (casi apagados) a 1
 * (a pleno). Se van prendiendo de la portada a la mejor nota y vuelven a bajar en el
 * cierre. Es la opacidad de la capa de rayos (la variable --nitidez de TuMes.css).
 */
export const NITIDEZ: Record<TipoDeTarjeta, number> = {
  portada: 0,
  resenias: 0.2,
  mesActivo: 0.25,
  dia: 0.3,
  barrios: 0.4,
  mejor: 1,
  logros: 0.55,
  podio: 0.45,
  reacciones: 0.5,
  nivel: 0.6,
  cierre: 0.65,
}

/**
 * Qué tarjetas lleva el resumen: las que no tienen datos no se arman. Las del recuento
 * anual (el mes más activo y los niveles del año) solo están en diciembre.
 */
export function tarjetasDe(resumen: TuMes): TipoDeTarjeta[] {
  const tarjetas: TipoDeTarjeta[] = ['portada', 'resenias']
  if (resumen.recuento) tarjetas.push('mesActivo')
  if (resumen.dia) tarjetas.push('dia')
  if (resumen.barrios.length > 0) tarjetas.push('barrios')
  if (resumen.mejor) tarjetas.push('mejor')
  if ((resumen.logrosGanados ?? []).length > 0) tarjetas.push('logros')
  if ((resumen.podios ?? []).length > 0) tarjetas.push('podio')
  if (resumen.reacciones) tarjetas.push('reacciones')
  if (resumen.recuento) tarjetas.push('nivel')
  tarjetas.push('cierre')
  return tarjetas
}

/** "2026-11-02" → "2 de noviembre". */
export function diaDe(fecha: string): string {
  const [anio, mes, dia] = fecha.split('-').map(Number)
  return new Date(anio, mes - 1, dia).toLocaleDateString('es-AR', { day: 'numeric', month: 'long' })
}
