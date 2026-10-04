import { Link } from 'react-router-dom'
import { IconRoute } from './icons'
import { JointPhoto } from './JointPhoto'
import type { SavedTour } from '../types'
import { routeUrl } from '../utils/maps'
import { kilometros } from '../utils/numeros'
import { enHoras } from '../utils/tiempo'

/**
 * Un recorrido guardado, como se ve en el perfil.
 *
 * Muestra las paradas en miniatura y no la lista entera: en el perfil hay varios, y lo
 * que se quiere ver de un vistazo es cuál es cuál. El detalle está a un toque, en Maps.
 */
export function SavedTourCard({
  tour,
  onBorrar,
  borrando,
}: {
  tour: SavedTour
  onBorrar: (id: number) => void
  borrando: boolean
}) {
  const enAuto = tour.modo === 'EN_AUTO'

  return (
    <article className="flex flex-col gap-3 rounded-box bg-base-100 p-4 ring-1 ring-inset ring-base-content/15">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="font-display truncate text-sm font-bold">{tour.name}</h3>
          <p className="text-xs text-base-content/70">
            {kilometros(tour.kilometros)} km: {enHoras(tour.minutos)} {enAuto ? 'en auto' : 'caminando'}
          </p>
        </div>
        <button
          type="button"
          // Borrar no se deshace: antes se iba con un solo toque, y el botón está en la
          // misma tarjeta que se toca para abrir el recorrido.
          onClick={() => {
            if (confirm(`¿Borrar el recorrido “${tour.name}”? No se puede deshacer.`)) onBorrar(tour.id)
          }}
          disabled={borrando}
          className="btn btn-ghost btn-sm -mr-2 -mt-1 shrink-0 text-error"
        >
          {borrando ? 'Borrando…' : 'Borrar'}
          <span className="sr-only"> el recorrido {tour.name}</span>
        </button>
      </div>

      <ol className="flex flex-wrap gap-2">
        {tour.paradas.map((parada) => (
          <li key={parada.local.id}>
            <Link
              to={`/burger-joints/${parada.local.id}`}
              className="flex items-center gap-2 rounded-full bg-base-200 py-1 pl-1 pr-3"
            >
              <JointPhoto
                src={parada.local.photoUrl}
                name={parada.local.name}
                className="h-6 w-6 flex-none rounded-full object-cover"
              />
              <span className="max-w-32 truncate text-xs font-medium">{parada.local.name}</span>
            </Link>
          </li>
        ))}
      </ol>

      <a
        href={routeUrl(
          tour.paradas.map((p) => p.local),
          { enAuto },
        )}
        target="_blank"
        rel="noopener noreferrer"
        className="-my-2 flex w-fit items-center gap-1 py-2 text-xs font-semibold text-primary hover:underline"
      >
        <IconRoute size={14} />
        Abrir el recorrido en Maps
        <span className="sr-only"> (se abre en otra pestaña)</span>
      </a>
    </article>
  )
}
