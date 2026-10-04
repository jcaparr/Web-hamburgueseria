import { Link } from 'react-router-dom'
import type { SavedTour, Tour } from '../types'
import { shortAddress } from '../utils/address'
import { kilometros } from '../utils/numeros'
import { enHoras } from '../utils/tiempo'
import { IconPin, IconRoute } from './icons'
import { JointPhoto } from './JointPhoto'
import { ScoreBadge } from './ScoreBadge'

/**
 * El recorrido que se acaba de armar: el resumen, las paradas en orden y los botones
 * para guardarlo o abrirlo en Maps.
 */
export function Recorrido({
  tour,
  enlace,
  enAuto,
  puedeGuardar,
  guardando,
  guardado,
  onGuardar,
}: {
  tour: Tour
  enlace: string
  enAuto: boolean
  puedeGuardar: boolean
  guardando: boolean
  guardado: SavedTour | null
  onGuardar: () => void
}) {
  if (tour.paradas.length === 0) {
    return (
      <p className="rounded-box bg-base-200 p-4 text-sm text-base-content/70">
        {tour.aviso ?? 'No salió ningún recorrido con esos filtros. Probá con más kilómetros o con otros barrios.'}
      </p>
    )
  }

  return (
    <section aria-labelledby="titulo-del-recorrido" className="flex flex-col gap-4">
      <h2 id="titulo-del-recorrido" className="font-display text-lg font-bold">
        Tu recorrido
      </h2>
      <div className="flex items-center justify-around rounded-box bg-neutral p-4 text-base-100">
        <Dato valor={String(tour.paradas.length)} etiqueta="paradas" />
        <Dato valor={kilometros(tour.kilometros)} etiqueta="km" />
        <Dato valor={enHoras(tour.minutos)} etiqueta={enAuto ? 'manejando' : 'caminando'} />
      </div>

      {tour.aviso && (
        <p className="rounded-box bg-base-200 px-4 py-3 text-sm text-base-content/70">
          {tour.aviso}
        </p>
      )}

      <ol className="flex flex-col gap-3">
        {tour.paradas.map((parada) => (
          <li
            key={parada.local.id}
            className="flex items-center gap-3 rounded-box bg-base-100 p-3 ring-1 ring-inset ring-base-content/15"
          >
            {/* El número ya lo dice la lista numerada; este es para mirar. */}
            <span aria-hidden="true" className="flex h-7 w-7 flex-none items-center justify-center rounded-full bg-neutral font-display text-sm font-bold text-secondary">
              {parada.orden}
            </span>

            <Link to={`/burger-joints/${parada.local.id}`} className="flex min-w-0 flex-1 items-center gap-3">
              <figure className="h-14 w-14 flex-none overflow-hidden rounded-xl bg-base-200">
                <JointPhoto
                  src={parada.local.photoUrl}
                  name={parada.local.name}
                  className="h-full w-full object-cover"
                />
              </figure>

              <div className="flex min-w-0 flex-col gap-0.5">
                <h3 className="font-display line-clamp-1 text-base font-bold">{parada.local.name}</h3>
                <p className="line-clamp-1 text-xs text-base-content/70">
                  {shortAddress(parada.local.address, parada.local.area)}
                </p>
                <div className="mt-0.5 flex items-center gap-2">
                  {parada.local.averageScore ? (
                    <ScoreBadge score={parada.local.averageScore} size="sm" />
                  ) : (
                    <span className="text-xs font-medium text-base-content/70">
                      Sin calificaciones
                    </span>
                  )}
                  {parada.kilometros > 0 && (
                    <span className="text-xs text-base-content/70">
                      +{kilometros(parada.kilometros)} km
                    </span>
                  )}
                  {parada.visitada && (
                    <span className="rounded-full bg-secondary/40 px-2 text-xs font-semibold text-neutral">Ya fuiste</span>
                  )}
                </div>
              </div>
            </Link>
          </li>
        ))}
      </ol>

      {puedeGuardar && (
        <button
          type="button"
          onClick={onGuardar}
          disabled={guardando || guardado !== null}
          className="btn btn-outline btn-block"
        >
          {guardado ? 'Guardado en tu perfil' : guardando ? 'Guardando…' : 'Guardar este recorrido'}
        </button>
      )}

      <a
        href={enlace}
        target="_blank"
        rel="noopener noreferrer"
        className="btn btn-neutral btn-block"
      >
        <IconRoute size={18} />
        {enAuto ? 'Abrir el recorrido en auto' : 'Abrir el recorrido a pie'}
        <span className="sr-only"> (se abre en otra pestaña)</span>
      </a>
      <p className="-mt-2 flex items-center justify-center gap-1 text-center text-xs text-base-content/70">
        <IconPin />
        Los kilómetros son una estimación: Maps te da el camino exacto.
      </p>
    </section>
  )
}

function Dato({ valor, etiqueta }: { valor: string; etiqueta: string }) {
  return (
    <div className="flex flex-col items-center">
      <span className="font-display text-xl font-bold text-secondary">{valor}</span>
      <span className="text-xs text-base-100/80">{etiqueta}</span>
    </div>
  )
}
