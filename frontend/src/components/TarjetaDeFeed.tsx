import { Link } from 'react-router-dom'
import { AvatarDeUsuario } from './AvatarDeUsuario'
import { FotoDeResenia } from './FotoDeResenia'
import { IconPin } from './icons'
import { Stars } from './Stars'
import { relativeDate } from '../utils/relativeDate'
import type { ItemDeFeed } from '../types'

/**
 * Una reseña en el feed, como una publicación.
 *
 * Cuatro partes, en este orden y separadas a propósito: quién la escribió, la foto,
 * dónde fue y qué puntaje puso, y recién al final lo que dijo.
 *
 * La foto va arriba de todo, apenas abajo del nombre, porque es lo que hace parar el
 * scroll. El lugar y la nota van juntos en una franja propia: son el dato duro de la
 * tarjeta —a dónde ir y si estuvo buena— y mezclados con el comentario se pierden.
 *
 * Los enlaces son dos y separados: al perfil de quien la escribió y a la
 * hamburguesería. Son las dos cosas que uno quiere hacer después de leerla, y una
 * tarjeta que lleve a un solo lado obliga a volver atrás para la otra.
 */
export function TarjetaDeFeed({ item, esMia }: { item: ItemDeFeed; esMia: boolean }) {
  return (
    <article className="overflow-hidden rounded-box bg-base-100 ring-1 ring-inset ring-base-content/10">
      <header className="flex items-center gap-3 px-4 py-3">
        <Link to={`/u/${item.autorUsername}`} className="flex min-w-0 items-center gap-3">
          <AvatarDeUsuario username={item.autorUsername} size={38} />
          <div className="flex min-w-0 flex-col">
            <span className="truncate text-sm font-semibold hover:text-primary">
              @{item.autorUsername}
              {esMia && <span className="font-normal text-base-content/50"> · vos</span>}
            </span>
            <span className="text-xs text-base-content/50">
              {relativeDate(item.createdAt)}
              {/* La fecha sigue siendo la de cuando se escribió: esto solo avisa que lo
                  que se está leyendo ya no es lo de ese día. */}
              {item.editada && ' · editada'}
            </span>
          </div>
        </Link>
      </header>

      {item.fotoDeLaResenia && (
        <Link to={`/burger-joints/${item.burgerJointId}`} className="block">
          <FotoDeResenia src={item.fotoDeLaResenia} autorUsername={item.autorUsername} />
        </Link>
      )}

      <Link
        to={`/burger-joints/${item.burgerJointId}`}
        className="flex items-center gap-3 border-b border-base-content/10 bg-base-200/40 px-4 py-3"
      >
        <div className="flex min-w-0 flex-1 flex-col">
          <span className="truncate font-display font-bold">{item.burgerJointName}</span>
          {item.area && (
            <span className="flex items-center gap-1 text-xs text-base-content/50">
              <IconPin size={12} />
              {item.area}
            </span>
          )}
        </div>
        {/* Las estrellas y no el ScoreBadge: acá la nota es la que puso esta persona,
            y el badge se usa en toda la app para el promedio de la hamburguesería.
            El mismo dibujo para dos cosas distintas se lee como la equivocada. */}
        <Stars value={item.score} size={16} />
      </Link>

      {item.comment && (
        <p className="whitespace-pre-line px-4 py-3 text-sm leading-relaxed text-base-content/80">
          {item.comment}
        </p>
      )}
    </article>
  )
}
