import { Link } from 'react-router-dom'
import { AvatarDeUsuario } from './AvatarDeUsuario'
import { FotosDeResenia } from './FotosDeResenia'
import { IconPin } from './icons'
import { ReaccionesDeResenia } from './ReaccionesDeResenia'
import { ScoreBadge } from './ScoreBadge'
import { Stars } from './Stars'
import { fechaYHora } from '../utils/fechaYHora'
import type { ItemDeFeed } from '../types'

/**
 * Una reseña en el feed, como una publicación.
 *
 * Arriba, en un solo renglón, quién y dónde: "@juanca en Café Martínez". Es como se
 * cuenta una salida —fui a tal lado— y como lo muestran las redes que la gente ya usa.
 * Antes el lugar iba en una franja propia debajo de la foto, y había que pasar la foto
 * entera para saber de qué local se estaba hablando (#187).
 *
 * Después la foto, que es lo que hace parar el scroll, y debajo la nota y lo que dijo.
 *
 * Los enlaces son dos y separados: al perfil de quien la escribió y a la
 * hamburguesería. Son las dos cosas que uno quiere hacer después de leerla, y una
 * tarjeta que lleve a un solo lado obliga a volver atrás para la otra.
 */
export function TarjetaDeFeed({ item, esMia }: { item: ItemDeFeed; esMia: boolean }) {
  const aLaFicha = `/burger-joints/${item.burgerJointId}`
  const alPerfil = `/u/${item.autorUsername}`

  return (
    <article className="overflow-hidden tarjeta">
      <header className="flex items-center gap-3 px-4 py-3">
        {/* Lleva al mismo perfil que el nombre de al lado: fuera del Tab y del lector de
            pantalla, para no pasar dos veces por el mismo enlace. */}
        <Link to={alPerfil} tabIndex={-1} aria-hidden="true" className="flex-none rounded-full">
          <AvatarDeUsuario username={item.autorUsername} hamburguesa={item.autorHamburguesa} size={38} />
        </Link>
        <div className="flex min-w-0 flex-1 flex-col">
          {/* Un renglón que se corta al final con puntos suspensivos: con un nombre de
              local largo se pierde la cola del local, que es lo que menos falta hace
              para reconocerlo. */}
          <p className="truncate text-sm">
            <Link to={alPerfil} className="font-semibold hover:text-primary">
              @{item.autorUsername}
            </Link>
            {esMia && <span className="text-base-content/70"> (vos)</span>}
            <span className="text-base-content/70"> en </span>
            <Link to={aLaFicha} className="font-semibold hover:text-primary">
              {item.burgerJointName}
            </Link>
          </p>
          <span className="flex min-w-0 items-center gap-2 text-xs text-base-content/70">
            <span className="flex-none">
              {fechaYHora(item.createdAt)}
              {/* La fecha sigue siendo la de cuando se escribió: esto solo avisa que lo
                  que se está leyendo ya no es lo de ese día. */}
              {item.editada && ' (editada)'}
            </span>
            {item.area && (
              <span className="flex min-w-0 items-center gap-0.5">
                <IconPin size={11} className="flex-none" />
                <span className="truncate">{item.area}</span>
              </span>
            )}
          </span>
        </div>
      </header>

      {/* Tocar una foto lleva al mismo lugar que el nombre del local de arriba. */}
      <FotosDeResenia fotos={item.fotosDeLaResenia} autorUsername={item.autorUsername} enlace={aLaFicha} />

      <div className="flex flex-col gap-2 px-4 py-3">
        <div className="flex items-center justify-between gap-3">
          {/* Las estrellas y no el ScoreBadge: acá la nota es la que puso esta persona,
              y el badge se usa en toda la app para el promedio de la hamburguesería.
              El mismo dibujo para dos cosas distintas se lee como la equivocada. */}
          <Stars value={item.score} size={17} />
          {/* El promedio del local, enfrente de la nota de esta persona: juntos dejan
              ver si lo que se está leyendo se sale de la norma. */}
          {item.promedioDelLocal !== null && (
            <span className="flex items-center gap-2 text-xs text-base-content/70">
              Promedio del local
              <ScoreBadge score={item.promedioDelLocal} size="sm" />
            </span>
          )}
        </div>

        {item.comment && (
          <p className="whitespace-pre-line text-sm leading-relaxed text-base-content/80">
            {item.comment}
          </p>
        )}

        {/* Al final, como en cualquier publicación: se reacciona después de leer. */}
        <ReaccionesDeResenia ratingId={item.ratingId} reacciones={item.reacciones} puedeReaccionar={!esMia} />
      </div>
    </article>
  )
}
