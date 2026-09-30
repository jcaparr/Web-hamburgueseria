import { Link } from 'react-router-dom'
import { IconPin } from './icons'
import { JointPhoto } from './JointPhoto'
import { Stars } from './Stars'
import { relativeDate } from '../utils/relativeDate'
import type { ItemDeFeed } from '../types'

/**
 * Una reseña en el feed.
 *
 * Tiene dos enlaces separados a propósito: al perfil de quien la escribió y a la
 * hamburguesería. Son las dos cosas que uno quiere hacer después de leerla —ver qué
 * más opina esa persona, o ver dónde es— y una tarjeta que lleve a un solo lado
 * obligaría a volver atrás para la otra.
 */
export function TarjetaDeFeed({ item, esMia }: { item: ItemDeFeed; esMia: boolean }) {
  return (
    <article className="flex flex-col gap-3 rounded-box bg-base-100 p-4 ring-1 ring-inset ring-base-content/15">
      <div className="flex items-center justify-between gap-2">
        <Link
          to={`/u/${item.autorUsername}`}
          className="truncate text-sm font-semibold hover:text-primary"
        >
          @{item.autorUsername}
          {esMia && ' (vos)'}
        </Link>
        <span className="flex-none text-xs text-base-content/50">
          {relativeDate(item.createdAt)}
          {/* La fecha sigue siendo la de cuando se escribió: esto solo avisa que lo
              que se está leyendo ya no es lo de ese día. */}
          {item.editada && ' · editada'}
        </span>
      </div>

      <Link to={`/burger-joints/${item.burgerJointId}`} className="flex items-center gap-3">
        <JointPhoto
          src={item.photoUrl}
          name={item.burgerJointName}
          className="h-12 w-12 flex-none rounded-lg object-cover"
        />
        <div className="flex min-w-0 flex-1 flex-col">
          <span className="truncate font-medium">{item.burgerJointName}</span>
          {item.area && (
            <span className="flex items-center gap-1 text-xs text-base-content/50">
              <IconPin size={12} />
              {item.area}
            </span>
          )}
        </div>
        <Stars value={item.score} size={14} />
      </Link>

      {item.comment && <p className="text-sm text-base-content/70">{item.comment}</p>}

      {item.fotoDeLaResenia && (
        <img
          src={item.fotoDeLaResenia}
          alt={`La hamburguesa que reseñó @${item.autorUsername}`}
          // Perezosa porque el feed baja de a veinte: cargar veinte fotos que nadie
          // llegó a ver todavía es gastar datos del teléfono de otro.
          loading="lazy"
          className="max-h-80 w-full rounded-lg object-cover"
        />
      )}
    </article>
  )
}
