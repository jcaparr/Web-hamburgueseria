import { Link } from 'react-router-dom'
import { AvatarDeUsuario } from './AvatarDeUsuario'
import { BotonSeguir } from './BotonSeguir'
import type { UsuarioBuscado } from '../types'

/**
 * Una persona en una lista: su hamburguesa, su nombre, cuántas reseñas tiene y el
 * botón para seguirla.
 *
 * La usan el buscador y las listas de seguidores (#183). Es el mismo renglón en los
 * dos lados a propósito: quien aprendió a leerlo en uno ya lo sabe leer en el otro.
 *
 * @param esYo sin botón: uno no se sigue a sí mismo, y en la lista de seguidores de
 *   otro podés estar vos.
 */
export function FilaDePersona({
  persona,
  esYo = false,
  onCambio,
}: {
  persona: UsuarioBuscado
  esYo?: boolean
  onCambio: (loSigo: boolean) => void
}) {
  return (
    <li className="flex items-center gap-3 tarjeta p-3">
      <Link to={`/u/${persona.username}`} className="flex min-w-0 flex-1 items-center gap-3">
        <AvatarDeUsuario username={persona.username} hamburguesa={persona.hamburguesa} size={40} />
        <div className="flex min-w-0 flex-col">
          <span className="truncate font-semibold">
            @{persona.username}
            {esYo && <span className="font-normal text-base-content/70"> (vos)</span>}
          </span>
          <span className="text-xs text-base-content/70">
            {persona.resenias === 1 ? '1 reseña' : `${persona.resenias} reseñas`}
          </span>
        </div>
      </Link>
      {!esYo && <BotonSeguir chico username={persona.username} loSigo={persona.loSigo} onCambio={onCambio} />}
    </li>
  )
}
