/**
 * Cuándo pasó algo, como se dice acá: "Hoy 21:33hs", "Ayer 13:23hs" o "01/10/2026 09:11hs".
 *
 * Con la hora de Buenos Aires, mire desde donde se mire, como el horario de los locales:
 * la reseña de alguien que la escribió a la noche acá no puede aparecer como de mañana
 * para quien la lee desde Madrid.
 */

const RELOJ_DE_BUENOS_AIRES = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'America/Argentina/Buenos_Aires',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

const UN_DIA = 24 * 60 * 60 * 1000

function enBuenosAires(fecha: Date): { dia: string; hora: string } {
  const partes = Object.fromEntries(
    RELOJ_DE_BUENOS_AIRES.formatToParts(fecha).map((p) => [p.type, p.value]),
  )
  return { dia: `${partes.day}/${partes.month}/${partes.year}`, hora: `${partes.hour}:${partes.minute}` }
}

/** Hoy y ayer se cuentan por día del calendario, no por horas: lo de las 23:50 de ayer es de ayer. */
export function fechaYHora(iso: string, ahora: Date = new Date()): string {
  const { dia, hora } = enBuenosAires(new Date(iso))
  if (dia === enBuenosAires(ahora).dia) return `Hoy ${hora}hs`
  // Buenos Aires no cambia la hora en verano: un día antes son siempre 24 horas antes.
  if (dia === enBuenosAires(new Date(ahora.getTime() - UN_DIA)).dia) return `Ayer ${hora}hs`
  return `${dia} ${hora}hs`
}
