import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { LocalDelMapa } from '../types'
import { estadoDeHoy, momentoEnBuenosAires } from '../utils/horario'
import { comoLlegarUrl } from '../utils/maps'
import { cantidad } from '../utils/numeros'
import { rutaDelLocal } from '../utils/rutaDelLocal'
import { IconHeart, IconRoute } from './icons'
import { ScoreBadge } from './ScoreBadge'

interface Props {
  local: LocalDelMapa
  /** "a 350 m", si se sabe dónde está quien mira. */
  distancia: string | null
  guardado: boolean
  onGuardar: () => void
  onCerrar: () => void
}

/**
 * Lo que se abre al tocar un punto del mapa (#395): foto, nombre —que lleva a la ficha, por
 * eso no hay un "Ver ficha"—, barrio, nota, horario de hoy, y los dos botones: Guardar y
 * Cómo llegar. En el teléfono sube desde abajo; en escritorio flota al lado del punto.
 */
export function TarjetaDelMapa({ local, distancia, guardado, onGuardar, onCerrar }: Props) {
  // La hora se toma al abrir la tarjeta: abierta durante horas se desactualiza, pero se cierra al tocar otro punto.
  const [ahora] = useState(() => new Date())
  const horario = estadoDeHoy(local.franjas, momentoEnBuenosAires(ahora))
  const cuantas = local.ratingsCount

  return (
    <div className="rounded-t-[1.375rem] bg-base-100 p-4 shadow-[0_-10px_30px_-12px_rgb(0_0_0/0.45)] ring-1 ring-inset ring-base-content/15 md:w-[21rem] md:rounded-box md:shadow-xl">
      <button
        type="button"
        onClick={onCerrar}
        aria-label="Cerrar"
        className="absolute right-1 top-1 grid h-11 w-11 place-items-center rounded-full text-xl leading-none text-base-content/70 hover:text-base-content focus-visible:outline-2 focus-visible:outline-primary"
      >
        <span aria-hidden="true">×</span>
      </button>

      <div className="flex gap-3 pr-8">
        <div className="h-[4.75rem] w-[4.75rem] flex-none overflow-hidden rounded-field bg-base-300">
          {local.miniatura && <img src={local.miniatura} alt="" className="h-full w-full object-cover" />}
        </div>
        <div className="flex min-w-0 flex-col items-start gap-1">
          <Link
            to={rutaDelLocal(local)}
            className="inline-flex min-h-11 items-center break-words font-display text-lg font-extrabold leading-tight text-primary underline decoration-2 underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          >
            {local.name}
          </Link>
          <p className="-mt-2 text-sm text-base-content/70">
            {[local.area, distancia].filter(Boolean).join(' · ')}
          </p>
        </div>
      </div>

      <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-base-content/70">
        {local.averageScore != null ? (
          <>
            <ScoreBadge score={local.averageScore} size="sm" />
            <span>{cuantas === 1 ? "1 reseña" : `${cantidad(cuantas)} reseñas`}</span>
          </>
        ) : (
          <span>Todavía sin reseñas</span>
        )}
      </div>

      {horario && (
        <p className="mt-2 flex items-center gap-2 text-sm">
          <span
            aria-hidden="true"
            className={`h-2.5 w-2.5 flex-none rounded-full ${horario.abierto ? 'bg-success' : 'bg-error'}`}
          />
          <span className={horario.abierto ? 'font-semibold text-success' : 'font-semibold text-error'}>
            {horario.texto.split(' · ')[0]}
          </span>
          {horario.texto.includes(' · ') && (
            <span className="text-base-content/70">{horario.texto.split(' · ')[1]}</span>
          )}
        </p>
      )}

      <div className="mt-3 grid grid-cols-2 gap-2">
        <button
          type="button"
          onClick={onGuardar}
          aria-pressed={guardado}
          className={`btn min-h-11 ${guardado ? 'btn-primary' : 'btn-outline'}`}
        >
          <IconHeart size={18} filled={guardado} />
          {guardado ? 'Guardado' : 'Guardar'}
        </button>
        <a
          href={comoLlegarUrl(local)}
          target="_blank"
          rel="noopener noreferrer"
          className="btn btn-primary min-h-11"
        >
          <IconRoute size={18} />
          Cómo llegar
          <span className="sr-only"> (se abre en Maps, en otra pestaña)</span>
        </a>
      </div>
    </div>
  )
}
