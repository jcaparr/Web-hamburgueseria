const MIN = 60_000
const HORA = 60 * MIN
const DIA = 24 * HORA

/** Cuánto falta para que cierre el período, en el texto del contador. Pasa a minutos en la última hora. */
export function tiempoParaElCierre(cierraEl: string, ahora: number): { texto: string; ultimaHora: boolean } {
  const falta = new Date(cierraEl).getTime() - ahora
  if (falta <= 0) return { texto: 'Cerrando…', ultimaHora: true }
  if (falta < MIN) return { texto: 'menos de 1 min', ultimaHora: true }
  if (falta < HORA) return { texto: `${Math.floor(falta / MIN)} min`, ultimaHora: true }
  if (falta < DIA) {
    const h = Math.floor(falta / HORA)
    const m = Math.floor((falta % HORA) / MIN)
    return { texto: m > 0 ? `${h} h ${m} min` : `${h} h`, ultimaHora: false }
  }
  const d = Math.floor(falta / DIA)
  const h = Math.floor((falta % DIA) / HORA)
  return { texto: h > 0 ? `${d} d ${h} h` : `${d} d`, ultimaHora: false }
}
