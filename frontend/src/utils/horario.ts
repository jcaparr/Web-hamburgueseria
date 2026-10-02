import type { FranjaHoraria } from '../types'

/**
 * Las cuentas del horario de un local: si está abierto ahora y qué horario tiene cada día.
 *
 * El servidor guarda el horario y nada más. Abierto o cerrado se calcula acá porque
 * depende del momento en que se mira, y así el servidor no tiene que contestar distinto
 * a cada minuto.
 */

const MINUTOS_POR_DIA = 24 * 60

/** Como los numera Google y como los guarda el servidor: 0 es domingo. */
export const NOMBRES_DE_LOS_DIAS = [
  'Domingo',
  'Lunes',
  'Martes',
  'Miércoles',
  'Jueves',
  'Viernes',
  'Sábado',
]

/** El orden en que se lee una semana acá: de lunes a domingo. */
export const SEMANA_DE_LUNES_A_DOMINGO = [1, 2, 3, 4, 5, 6, 0]

/**
 * El día y la hora en Buenos Aires, mire desde donde se mire.
 *
 * Los horarios son de locales de acá. Alguien que abre la página desde Madrid quiere
 * saber si el local está abierto ahora en Buenos Aires, no a la hora de Madrid.
 */
export type Momento = { dia: number; minuto: number }

const DIAS_EN_INGLES = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

const RELOJ_DE_BUENOS_AIRES = new Intl.DateTimeFormat('en-US', {
  timeZone: 'America/Argentina/Buenos_Aires',
  weekday: 'short',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

export function momentoEnBuenosAires(fecha: Date): Momento {
  const partes = Object.fromEntries(
    RELOJ_DE_BUENOS_AIRES.formatToParts(fecha).map((p) => [p.type, p.value]),
  )
  return {
    dia: DIAS_EN_INGLES.indexOf(partes.weekday),
    minuto: Number(partes.hour) * 60 + Number(partes.minute),
  }
}

/** "19:00", "00:30": como se escribe una hora acá. Pasada la medianoche vuelve a empezar. */
export function hora(minutos: number): string {
  const delDia = minutos % MINUTOS_POR_DIA
  const h = Math.floor(delDia / 60)
  const m = delDia % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

/** Las franjas que empiezan ese día, en orden. */
export function franjasDelDia(franjas: FranjaHoraria[], dia: number): FranjaHoraria[] {
  return franjas.filter((f) => f.dia === dia).sort((a, b) => a.abre - b.abre)
}

/**
 * El horario de un día en palabras: "12:00–15:30, 19:00–00:30".
 *
 * Una franja de medianoche a medianoche se lee "Abierto las 24 horas", que es como lo
 * dice cualquiera, y un día sin franjas, "Cerrado".
 */
export function horarioDelDia(franjas: FranjaHoraria[], dia: number): string {
  const delDia = franjasDelDia(franjas, dia)
  if (delDia.length === 0) return 'Cerrado'
  if (abreTodoElDia(franjas, dia)) return 'Abierto las 24 horas'
  return delDia.map((f) => `${hora(f.abre)}–${hora(f.cierra)}`).join(', ')
}

/** Si ese día abre de medianoche a medianoche. */
export function abreTodoElDia(franjas: FranjaHoraria[], dia: number): boolean {
  return franjasDelDia(franjas, dia).some((f) => f.abre === 0 && f.cierra >= MINUTOS_POR_DIA)
}

/**
 * Si el local está abierto en este momento.
 *
 * Mira también las franjas del día anterior: un viernes de 19 a 1 sigue abierto el
 * sábado a las 0:30, y esa franja está guardada en el viernes.
 */
export function estaAbierto(franjas: FranjaHoraria[], { dia, minuto }: Momento): boolean {
  return franjas.some((f) => {
    // Cuántos días después de que abrió la franja estamos, contando la vuelta de la
    // semana: del sábado (6) al domingo (0) es un día, no menos seis.
    const diasDesdeQueAbre = (dia - f.dia + 7) % 7
    const minutoDesdeQueAbre = diasDesdeQueAbre * MINUTOS_POR_DIA + minuto
    return minutoDesdeQueAbre >= f.abre && minutoDesdeQueAbre < f.cierra
  })
}
