import { Link } from 'react-router-dom'
import type { BurgerJoint } from '../types'
import { shortAddress } from '../utils/address'
import { BotonSobreFoto } from './BotonSobreFoto'
import { IconHeart } from './icons'
import { JointPhoto } from './JointPhoto'
import { ScoreBadge } from './ScoreBadge'

/**
 * Una hamburguesería en la lista de Explorar: la foto con el corazón para guardarla,
 * el nombre, dónde queda y la nota.
 *
 * Toda la tarjeta lleva a la ficha. Antes tenía además "Ver en Maps" abajo, un
 * enlace chico que competía con la tarjeta entera; ahora la ficha tiene "Cómo llegar"
 * a un toque. El corazón es el mismo de la portada de la ficha, y está acá para
 * guardar sin tener que entrar.
 *
 * @param onAbrir lo que hay que hacer justo antes de ir a la ficha, como anotar dónde
 *                estaba la lista para volver al mismo lugar
 */
export function TarjetaDeLocal({
  local,
  onAbrir,
  onGuardar,
}: {
  local: BurgerJoint
  onAbrir?: () => void
  onGuardar: (local: BurgerJoint) => void
}) {
  return (
    <article className="relative h-full overflow-hidden rounded-box bg-base-100 ring-1 ring-inset ring-base-content/15 transition-shadow hover:shadow-md">
      <Link to={`/burger-joints/${local.id}`} onClick={onAbrir} className="flex h-full flex-col">
        <JointPhoto
          src={local.photoUrl}
          name={local.name}
          className="aspect-[16/10] w-full object-cover sm:aspect-[4/3]"
        />
        <div className="checker-strip" />
        <div className="flex flex-1 flex-col gap-1 p-4">
          <h3 className="line-clamp-1 font-display text-lg font-bold">{local.name}</h3>
          <p className="line-clamp-1 text-sm text-base-content/70">
            {shortAddress(local.address, local.area)}
          </p>
          <div className="mt-auto pt-2">
            {local.averageScore ? (
              <span className="flex items-center gap-2">
                <ScoreBadge score={local.averageScore} size="sm" />
                <span className="text-xs text-base-content/70">
                  {local.ratingsCount === 1 ? '1 reseña' : `${local.ratingsCount} reseñas`}
                </span>
              </span>
            ) : (
              <span className="text-xs text-base-content/70">Sin calificaciones</span>
            )}
          </div>
        </div>
      </Link>

      {/* Afuera del enlace: un botón adentro de un enlace no es válido, y tocar el
          corazón llevaría a la ficha en vez de guardar. */}
      <BotonSobreFoto
        onClick={() => onGuardar(local)}
        aria-pressed={local.inWishlist}
        aria-label={local.inWishlist ? `Quitar ${local.name} de guardadas` : `Guardar ${local.name}`}
        className={`absolute right-3 top-3 ${local.inWishlist ? 'text-primary' : ''}`}
      >
        <IconHeart size={20} filled={local.inWishlist} />
      </BotonSobreFoto>
    </article>
  )
}

/** Lo que se ve mientras llegan las tarjetas: su forma, para que la lista no salte. */
export function TarjetaDeLocalCargando() {
  return (
    <div aria-hidden="true" className="overflow-hidden rounded-box ring-1 ring-inset ring-base-content/10">
      <div className="skeleton aspect-[16/10] w-full rounded-none sm:aspect-[4/3]" />
      <div className="flex flex-col gap-2 p-4">
        <div className="skeleton h-5 w-2/3" />
        <div className="skeleton h-4 w-1/2" />
        <div className="skeleton mt-2 h-6 w-16" />
      </div>
    </div>
  )
}
