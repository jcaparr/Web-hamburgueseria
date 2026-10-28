import { Link } from 'react-router-dom'
import { AvatarDeUsuario } from './AvatarDeUsuario'
import { FotosDeResenia } from './FotosDeResenia'
import { BotonCompartirEnHistorias } from './BotonCompartirEnHistorias'
import { Stars } from './Stars'
import { IconPencil } from './icons'
import { ReaccionesDeResenia } from './ReaccionesDeResenia'
import type { Rating } from '../types'
import { fechaYVisita } from '../utils/fechaYHora'

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
  local,
}: {
  resenia: Rating
  esMia: boolean
  /** Falso en la propia y sin sesión: las reacciones se ven, pero no se tocan. */
  puedeReaccionar: boolean
  /** Si viene, se muestra el lápiz para editarla. Solo tiene sentido en la propia. */
  onEditar?: () => void
  /** Si viene en la propia, se ofrece armar la imagen para las historias de Instagram (#278). */
  local?: { nombre: string; barrio: string | null }
}) {
  // Solo las que puso: las tres son opcionales, y un guion en las que faltan sería ruido.
  const todas: [string, number | null][] = [
    ['Local', resenia.scoreLocal],
    ['Atención', resenia.scoreAtencion],
    ['Precio', resenia.scorePrecio],
  ]
  const notas = todas.filter(([, nota]) => nota != null)

  return (
    <article className="overflow-hidden tarjeta">
      <header className="flex items-center gap-3 px-4 py-3">
        {/* Desde acá se llega a su perfil, que es de donde sale la gente a seguir:
            alguien que reseñó lo mismo que vos es mejor candidato que cualquiera que
            encuentres buscando a ciegas. */}
        <Link to={`/u/${resenia.username}`} className="-my-1 flex min-w-0 items-center gap-3 rounded-field py-1">
          <AvatarDeUsuario username={resenia.username} hamburguesa={resenia.hamburguesa} xp={resenia.xp} size={36} />
          <div className="flex min-w-0 flex-col">
            <span className="truncate text-sm font-semibold hover:text-primary">
              @{resenia.username}
              {esMia && <span className="font-normal text-base-content/70"> (vos)</span>}
            </span>
            {/* Cuándo se publicó y, si fue otro día, cuándo se fue (#368). */}
            <span className="text-xs text-base-content/70">{fechaYVisita(resenia)}</span>
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

      {(resenia.comment || notas.length > 0 || puedeReaccionar || resenia.reacciones.cuantas.length > 0) && (
        <div className="flex flex-col gap-2 px-4 py-3">
          {notas.length > 0 && (
            <ul className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-base-content/70" aria-label="Otras notas">
              {notas.map(([rotulo, nota]) => (
                <li key={rotulo}>
                  {rotulo} <span className="font-semibold text-base-content">{nota}</span>
                  <span aria-hidden="true">/5</span>
                  <span className="sr-only"> de 5</span>
                </li>
              ))}
            </ul>
          )}
          {resenia.comment && (
            <p className="whitespace-pre-line text-sm leading-relaxed text-base-content/80">
              {resenia.comment}
            </p>
          )}
          <ReaccionesDeResenia ratingId={resenia.id} reacciones={resenia.reacciones} puedeReaccionar={puedeReaccionar} />
        </div>
      )}

      {esMia && local && (
        <div className="px-4 pb-3">
          <BotonCompartirEnHistorias resenia={resenia} nombreDelLocal={local.nombre} barrio={local.barrio} />
        </div>
      )}
    </article>
  )
}
