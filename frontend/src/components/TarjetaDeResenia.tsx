import { Link } from 'react-router-dom'
import { AvatarDeUsuario } from './AvatarDeUsuario'
import { FotoDeResenia } from './FotoDeResenia'
import { Stars } from './Stars'
import { IconPencil } from './icons'
import type { Rating } from '../types'
import { relativeDate } from '../utils/relativeDate'

/**
 * Una reseña de un local, como se ve en la ficha del local.
 *
 * Vive aparte porque el mismo dibujo se usa en dos lugares de la misma pantalla: la
 * lista de todas, y las de la gente que seguís asomadas arriba. Repetirlo era la forma
 * de que dentro de un mes una de las dos tuviera la foto y la otra no.
 *
 * Mismas partes y mismo orden que la tarjeta del feed, para que una reseña se lea igual
 * acá que allá. No lleva el nombre del local, que sería repetir el título de la pantalla
 * en cada tarjeta.
 */
export function TarjetaDeResenia({
  resenia,
  esMia,
  onEditar,
}: {
  resenia: Rating
  esMia: boolean
  /** Si viene, se muestra el lápiz para editarla. Solo tiene sentido en la propia. */
  onEditar?: () => void
}) {
  return (
    <article className="overflow-hidden rounded-box bg-base-100 ring-1 ring-inset ring-base-content/10">
      <header className="flex items-center gap-3 px-4 py-3">
        {/* Desde acá se llega a su perfil, que es de donde sale la gente a seguir:
            alguien que reseñó lo mismo que vos es mejor candidato que cualquiera que
            encuentres buscando a ciegas. */}
        <Link to={`/u/${resenia.username}`} className="flex min-w-0 items-center gap-3">
          <AvatarDeUsuario username={resenia.username} size={36} />
          <div className="flex min-w-0 flex-col">
            <span className="truncate text-sm font-semibold hover:text-primary">
              @{resenia.username}
              {esMia && <span className="font-normal text-base-content/50"> · vos</span>}
            </span>
            <span className="text-xs text-base-content/50">{relativeDate(resenia.createdAt)}</span>
          </div>
        </Link>

        <div className="ml-auto flex flex-none items-center gap-1">
          <Stars value={resenia.score} size={15} />
          {onEditar && (
            <button
              type="button"
              onClick={onEditar}
              aria-label="Editar tu reseña"
              className="btn btn-ghost btn-xs btn-square text-base-content/50"
            >
              <IconPencil size={15} />
            </button>
          )}
        </div>
      </header>

      {resenia.photoUrl && (
        <FotoDeResenia src={resenia.photoUrl} autorUsername={resenia.username} />
      )}

      {resenia.comment && (
        <p className="whitespace-pre-line px-4 py-3 text-sm leading-relaxed text-base-content/80">
          {resenia.comment}
        </p>
      )}
    </article>
  )
}
