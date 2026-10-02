import { useEffect, useState } from 'react'
import type { FranjaHoraria } from '../types'
import {
  NOMBRES_DE_LOS_DIAS,
  SEMANA_DE_LUNES_A_DOMINGO,
  abreTodoElDia,
  estaAbierto,
  franjasDelDia,
  horarioDelDia,
  momentoEnBuenosAires,
} from '../utils/horario'
import { IconChevronRight } from './icons'

/**
 * Si el local está abierto ahora y su horario de hoy, con la semana entera a un toque.
 *
 * Lo de hoy va a la vista porque es lo que se viene a buscar: "¿puedo ir ahora?". El
 * resto de la semana se pide, y se despliega en el lugar en vez de abrir otra pantalla.
 *
 * Sin horario no se muestra nada. Un "horario no disponible" ocuparía un renglón para
 * decir que no hay nada que decir.
 */
export function HorarioDelLocal({ franjas }: { franjas: FranjaHoraria[] }) {
  const ahora = useAhora()

  if (franjas.length === 0) return null

  const momento = momentoEnBuenosAires(ahora)
  const abierto = estaAbierto(franjas, momento)

  return (
    <details className="group text-sm">
      <summary className="group/resumen flex cursor-pointer list-none flex-wrap items-center gap-x-2 gap-y-0.5 rounded-field focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary [&::-webkit-details-marker]:hidden">
        <span className={`font-semibold ${abierto ? 'text-success' : 'text-error'}`}>
          {abierto ? 'Abierto' : 'Cerrado'}
        </span>
        <span className="text-base-content/70">{loDeHoy(franjas, momento.dia)}</span>
        <span aria-hidden="true" className="flex">
          <IconChevronRight
            size={14}
            className="text-base-content/70 transition-transform group-open:rotate-90 group-hover/resumen:text-base-content motion-reduce:transition-none"
          />
        </span>
        <span className="sr-only">Ver el horario de toda la semana</span>
      </summary>

      <ul className="mt-2 flex max-w-xs flex-col">
        {SEMANA_DE_LUNES_A_DOMINGO.map((dia) => {
          const esHoy = dia === momento.dia
          const cerrado = franjasDelDia(franjas, dia).length === 0
          return (
            <li
              key={dia}
              aria-current={esHoy ? 'date' : undefined}
              className={`flex justify-between gap-4 rounded-field px-2 py-1 ${esHoy ? 'bg-base-200 font-semibold' : ''}`}
            >
              <span>{NOMBRES_DE_LOS_DIAS[dia]}</span>
              <span
                className={`text-right tabular-nums ${cerrado && !esHoy ? 'text-base-content/70' : ''}`}
              >
                {horarioDelDia(franjas, dia)}
              </span>
            </li>
          )
        })}
      </ul>
    </details>
  )
}

/** Lo que acompaña a "Abierto" o "Cerrado": el horario de hoy, dicho para esa frase. */
function loDeHoy(franjas: FranjaHoraria[], hoy: number): string {
  if (franjasDelDia(franjas, hoy).length === 0) return 'Hoy no abre'
  if (abreTodoElDia(franjas, hoy)) return 'las 24 horas'
  return `Hoy ${horarioDelDia(franjas, hoy)}`
}

/**
 * La hora, puesta al día cada minuto.
 *
 * Sin esto, quien deja la página abierta ve "Abierto" pasada la hora de cierre: el
 * cálculo se haría una sola vez, al llegar.
 */
function useAhora(): Date {
  const [ahora, setAhora] = useState(() => new Date())
  useEffect(() => {
    const reloj = setInterval(() => setAhora(new Date()), 60_000)
    return () => clearInterval(reloj)
  }, [])
  return ahora
}
