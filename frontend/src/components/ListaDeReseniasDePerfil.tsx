import { Link } from 'react-router-dom'
import type { ReseniaDePerfil } from '../types'
import { aLaResenia } from '../utils/aLaResenia'
import { fechaYHora } from '../utils/fechaYHora'
import { JointPhoto } from './JointPhoto'
import { ScoreBadge } from './ScoreBadge'

/**
 * Las reseñas de una persona, con lo que escribió y no solo la nota: es lo que dice si
 * vale la pena seguirla. Un 4 sin más es igual al de cualquiera.
 *
 * La misma lista en el perfil, con las últimas, y en la página con todas (#247).
 *
 * Cada una lleva a esa reseña en la ficha del local, no al principio de la ficha (#260).
 */
export function ListaDeReseniasDePerfil({ resenias }: { resenias: ReseniaDePerfil[] }) {
  return (
    <ul className="flex flex-col gap-3">
      {resenias.map((r) => (
        <li key={r.id}>
          <Link
            to={aLaResenia(r.burgerJointId, r.id)}
            className="flex flex-col gap-2 tarjeta p-3 transition-shadow hover:tarjeta-alzada"
          >
            <span className="flex items-center gap-3">
              <JointPhoto
                src={r.photoUrl}
                name={r.burgerJointName}
                className="h-14 w-14 flex-none rounded-lg object-cover"
              />
              <span className="flex min-w-0 flex-1 flex-col">
                <span className="truncate font-semibold">{r.burgerJointName}</span>
                <span className="text-xs text-base-content/70">{fechaYHora(r.createdAt)}</span>
              </span>
              <ScoreBadge score={r.score} size="sm" />
            </span>
            {r.comment && (
              <span className="line-clamp-3 text-sm leading-relaxed text-base-content/80">{r.comment}</span>
            )}
          </Link>
        </li>
      ))}
    </ul>
  )
}
