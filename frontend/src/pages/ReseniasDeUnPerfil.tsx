import { Link, useParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconArrowLeft } from '../components/icons'
import { ListaDeReseniasDePerfil } from '../components/ListaDeReseniasDePerfil'
import { LoadError } from '../components/LoadError'
import { AvisoVacio } from '../components/Seccion'
import { useAuth } from '../context/useAuth'
import { usePedido } from '../hooks/usePedido'
import { useTitulo } from '../hooks/useTitulo'
import type { ReseniaDePerfil } from '../types'
import { isNotFound } from '../utils/errors'

/**
 * Todas las reseñas de alguien (#247), a las que lleva la cifra de reseñas de su perfil,
 * igual que la de seguidores lleva a quiénes son. El perfil muestra solo las últimas.
 */
export function ReseniasDeUnPerfil() {
  const { username = '' } = useParams()
  const { user } = useAuth()
  const soyYo = user?.username === username.toLowerCase()

  const titulo = soyYo ? 'Tus reseñas' : `Reseñas de @${username}`
  useTitulo(titulo)

  const pedido = usePedido(`resenias:${username}`, () =>
    apiClient.get<ReseniaDePerfil[]>(`/usuarios/${username}/resenias`).then(({ data }) => data),
  )
  const resenias = pedido.datos

  if (isNotFound(pedido.error)) {
    return (
      <div className="flex flex-col items-start gap-3 p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
        <h1 className="titulo-pagina">No encontramos a @{username}</h1>
        <p className="text-sm text-base-content/70">
          Puede que el enlace esté mal escrito o que esa cuenta ya no exista.
        </p>
        <Link to="/feed" className="btn btn-primary btn-sm">
          Buscar gente
        </Link>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
      <div className="flex items-center gap-2">
        <Link
          to={soyYo ? '/profile' : `/u/${username}`}
          aria-label="Volver al perfil"
          className="btn btn-ghost btn-square -ml-2 flex-none"
        >
          <IconArrowLeft size={18} />
        </Link>
        <h1 className="titulo-pagina min-w-0 truncate">{titulo}</h1>
      </div>

      {pedido.error ? (
        <LoadError error={pedido.error} onRetry={pedido.reintentar} />
      ) : !resenias ? (
        <p role="status" className="text-sm text-base-content/70">
          Cargando…
        </p>
      ) : resenias.length === 0 ? (
        <AvisoVacio>
          {soyYo ? 'Todavía no calificaste ninguna hamburguesería.' : 'Todavía no calificó ninguna hamburguesería.'}
        </AvisoVacio>
      ) : (
        <ListaDeReseniasDePerfil resenias={resenias} />
      )}
    </div>
  )
}
