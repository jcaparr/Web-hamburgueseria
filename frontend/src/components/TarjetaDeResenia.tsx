import { Link } from 'react-router-dom'
import { AvatarDeUsuario } from './AvatarDeUsuario'
import { FotosDeResenia } from './FotosDeResenia'
import { Stars } from './Stars'
import { IconPencil } from './icons'
import { ReaccionesDeResenia } from './ReaccionesDeResenia'
import type { Rating } from '../types'
import { fechaYHora } from '../utils/fechaYHora'

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
  puedeReaccionar,
  onEditar,
}: {
  resenia: Rating
  esMia: boolean
  /** Falso en la propia y sin sesión: las reacciones se ven, pero no se tocan. */
  puedeReaccionar: boolean
  /** Si viene, se muestra el lápiz para editarla. Solo tiene sentido en la propia. */
  onEditar?: () => void
}) {
  return (
    <article className="overflow-hidden tarjeta">
      <header className="flex items-center gap-3 px-4 py-3">
        {/* Desde acá se llega a su perfil, que es de donde sale la gente a seguir:
            alguien que reseñó lo mismo que vos es mejor candidato que cualquiera que
            encuentres buscando a ciegas. */}
        <Link to={`/u/${resenia.username}`} className="-my-1 flex min-w-0 items-center gap-3 rounded-field py-1">
          <AvatarDeUsuario username={resenia.username} hamburguesa={resenia.hamburguesa} size={36} />
          <div className="flex min-w-0 flex-col">
            <span className="truncate text-sm font-semibold hover:text-primary">
              @{resenia.username}
              {esMia && <span className="font-normal text-base-content/70"> (vos)</span>}
            </span>
            <span className="text-xs text-base-content/70">{fechaYHora(resenia.createdAt)}</span>
          </div>
        </Link>

        <div className="ml-auto flex flex-none items-center gap-1">
          <Stars value={resenia.score} size={15} />
          {onEditar && (
            <button
              type="button"
              onClick={onEditar}
              aria-label="Editar tu reseña"
              className="btn btn-ghost btn-square -my-2 -mr-2 text-base-content/70"
            >
              <IconPencil size={15} />
            </button>
          )}
        </div>
      </header>

      <FotosDeResenia fotos={resenia.fotos} autorUsername={resenia.username} />

      {(resenia.comment || puedeReaccionar || resenia.reacciones.cuantas.length > 0) && (
        <div className="flex flex-col gap-2 px-4 py-3">
          {resenia.comment && (
            <p className="whitespace-pre-line text-sm leading-relaxed text-base-content/80">
              {resenia.comment}
            </p>
          )}
          <ReaccionesDeResenia ratingId={resenia.id} reacciones={resenia.reacciones} puedeReaccionar={puedeReaccionar} />
        </div>
      )}
    </article>
  )
}
