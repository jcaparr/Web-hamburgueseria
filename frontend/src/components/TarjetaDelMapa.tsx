import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { LocalDelMapa } from '../types'
import { estadoDeHoy, momentoEnBuenosAires } from '../utils/horario'
import { comoLlegarUrl } from '../utils/maps'
import { cantidad } from '../utils/numeros'
import { colorDelArco } from '../utils/rangoDeNota'
import { rutaDelLocal } from '../utils/rutaDelLocal'
import { IconHeart, IconRoute } from './icons'
import { MedidorGrande } from './MedidorGrande'

interface Props {
  local: LocalDelMapa
  /** "a 350 m", si se sabe dónde está quien mira. */
  distancia: string | null
  guardado: boolean
  onGuardar: () => void
  onCerrar: () => void
}

/**
 * Lo que se abre al tocar un punto del mapa (#395). La foto va a la izquierda; el nombre —que
 * lleva a la ficha, por eso no hay un "Ver ficha"—, el barrio, las reseñas y el horario de hoy, en
 * el medio; la nota, a la derecha, en el medidor de la marca (el tablero del logo) con el arco del
 * color de su rango. Abajo, Guardar y Cómo llegar:
 * este último es lo único en kétchup. En el teléfono sube desde abajo; en escritorio flota al lado
 * del punto.
 */
export function TarjetaDelMapa({ local, distancia, guardado, onGuardar, onCerrar }: Props) {
  // La hora se toma al abrir la tarjeta: abierta durante horas se desactualiza, pero se cierra al tocar otro punto.
  const [ahora] = useState(() => new Date())
  const horario = estadoDeHoy(local.franjas, momentoEnBuenosAires(ahora))
  const cuantas = local.ratingsCount
  const [estado, hasta] = horario ? horario.texto.split(' · ') : []

  return (
    <div className="rounded-t-[1.375rem] bg-base-100 p-4 shadow-[0_-10px_30px_-12px_rgb(0_0_0/0.45)] ring-1 ring-inset ring-base-content/15 md:w-[24rem] md:rounded-box md:shadow-xl">
      <button
        type="button"
        onClick={onCerrar}
        aria-label="Cerrar"
        className="absolute right-1 top-1 grid h-11 w-11 place-items-center rounded-full text-xl leading-none text-base-content/70 hover:text-base-content focus-visible:outline-2 focus-visible:outline-primary"
      >
        <span aria-hidden="true">×</span>
      </button>

      <div className="flex items-start gap-3 pr-9">
        <div className="h-16 w-16 flex-none overflow-hidden rounded-2xl bg-base-300">
          {local.miniatura && <img src={local.miniatura} alt="" className="h-full w-full object-cover" />}
        </div>

        <div className="flex min-w-0 flex-1 flex-col items-start">
          <Link
            to={rutaDelLocal(local)}
            className="inline-flex min-h-11 items-center break-words font-display text-lg font-extrabold leading-tight text-base-content underline decoration-1 underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          >
            {local.name}
          </Link>
          <p className="-mt-1 text-sm text-base-content/70">
            {[
              local.area,
              cuantas === 0 ? 'Sin reseñas todavía' : cuantas === 1 ? '1 reseña' : `${cantidad(cuantas)} reseñas`,
              distancia,
            ]
              .filter(Boolean)
              .join(' · ')}
          </p>
          {horario && (
            <p className="mt-1 flex flex-wrap items-center gap-x-2 text-sm">
              <span
                aria-hidden="true"
                className={`h-2.5 w-2.5 flex-none rounded-full ${horario.abierto ? 'bg-success' : 'bg-error'}`}
              />
              <span className={`whitespace-nowrap font-semibold ${horario.abierto ? 'text-success' : 'text-error'}`}>
                {estado}
              </span>
              {hasta && <span className="whitespace-nowrap text-base-content/70">{hasta}</span>}
            </p>
          )}
        </div>

        {local.averageScore != null && (
          <MedidorGrande
            valor={local.averageScore}
            tamanio="sm"
            etiqueta="Nota"
            conLetraDelLogo
            colorDelArco={colorDelArco(local.averageScore)}
          />
        )}
      </div>

      <div className="mt-3 grid grid-cols-2 gap-2">
        <button
          type="button"
          onClick={onGuardar}
          aria-pressed={guardado}
          className={`btn min-h-11 ${guardado ? 'btn-neutral' : 'btn-outline'}`}
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
