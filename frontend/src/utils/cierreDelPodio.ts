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

const MESES = ['ene.', 'feb.', 'mar.', 'abr.', 'may.', 'jun.', 'jul.', 'ago.', 'sep.', 'oct.', 'nov.', 'dic.']
const MES_Y_ANIO = new Intl.DateTimeFormat('es-AR', { month: 'long', year: 'numeric', timeZone: 'UTC' })

/**
 * El nombre de una semana o un mes cerrado: "Del 13 al 19 de oct." o "Octubre de 2026". Las
 * fechas llegan como días sin hora, así que se leen en UTC para que no corran un día.
 */
export function nombreDelPeriodoCerrado(periodo: 'semana' | 'mes', { desde, hasta }: { desde: string; hasta: string }): string {
  const d = new Date(`${desde}T00:00:00Z`)
  const h = new Date(`${hasta}T00:00:00Z`)
  if (periodo === 'mes') {
    const texto = MES_Y_ANIO.format(d)
    return texto.charAt(0).toUpperCase() + texto.slice(1)
  }
  const dia = (f: Date) => `${f.getUTCDate()} de ${MESES[f.getUTCMonth()]}`
  const mismoMes = d.getUTCMonth() === h.getUTCMonth()
  return `Del ${mismoMes ? d.getUTCDate() : dia(d)} al ${dia(h)}`
}
