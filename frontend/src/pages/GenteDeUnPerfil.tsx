import { Link, useParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { FilaDePersona } from '../components/FilaDePersona'
import { IconArrowLeft } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { AvisoVacio } from '../components/Seccion'
import { useAuth } from '../context/useAuth'
import { usePedido } from '../hooks/usePedido'
import { useTitulo } from '../hooks/useTitulo'
import type { UsuarioBuscado } from '../types'
import { isNotFound } from '../utils/errors'

type Lista = 'seguidores' | 'siguiendo'

/**
 * Quiénes siguen a alguien y a quiénes sigue (#183).
 *
 * Las dos listas en la misma pantalla, con pestañas: quien entra por una casi siempre
 * quiere mirar la otra, y son la misma gente vista desde los dos lados. La pestaña va
 * en la dirección, así el número del perfil lleva directo a la que corresponde.
 *
 * Cada persona con su botón de seguir: una lista de seguidores es de los mejores
 * lugares para encontrar a quién seguir.
 */
export function GenteDeUnPerfil({ lista }: { lista: Lista }) {
  const { username = '' } = useParams()
  const { user } = useAuth()
  const soyYo = user?.username === username.toLowerCase()

  const titulo = soyYo
    ? lista === 'seguidores' ? 'Tus seguidores' : 'A quiénes seguís'
    : lista === 'seguidores' ? `Seguidores de @${username}` : `A quiénes sigue @${username}`
  useTitulo(titulo)

  const pedido = usePedido(`${lista}:${username}`, () =>
    apiClient.get<UsuarioBuscado[]>(`/usuarios/${username}/${lista}`).then(({ data }) => data),
  )
  const gente = pedido.datos

  function cambioDeSeguimiento(otro: string, loSigo: boolean) {
    pedido.actualizar((previos) => previos.map((u) => (u.username === otro ? { ...u, loSigo } : u)))
  }

  // Volver lleva al perfil del que se vino: el propio, con sus pestañas, o el de otro.
  const alPerfil = soyYo ? '/profile' : `/u/${username}`

  if (isNotFound(pedido.error)) {
    return (
      <div className="flex flex-col items-start gap-3 p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
        <h1 className="titulo-pagina">No encontramos a @{username}</h1>
        <p className="text-sm text-base-content/70">
          Puede que el enlace esté mal escrito o que esa cuenta ya no exista.
        </p>
        <Link to="/buscar" className="btn btn-primary btn-sm">
          Buscar gente
        </Link>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
      <div className="flex items-center gap-2">
        <Link to={alPerfil} aria-label="Volver al perfil" className="btn btn-ghost btn-square -ml-2 flex-none">
          <IconArrowLeft size={18} />
        </Link>
        <h1 className="titulo-pagina min-w-0 truncate">{titulo}</h1>
      </div>

      {/* Enlaces y no botones: cada lista tiene su dirección, y "atrás" tiene que salir
          de acá, no recorrer las pestañas. Por eso reemplazan la entrada del historial. */}
      <nav aria-label="Qué lista ver" className="tabs tabs-box">
        {(['seguidores', 'siguiendo'] as const).map((cual) => (
          <Link
            key={cual}
            to={`/u/${username}/${cual}`}
            replace
            aria-current={lista === cual ? 'page' : undefined}
            className={`tab flex-1 ${lista === cual ? 'tab-active' : 'text-base-content/70'}`}
          >
            {cual === 'seguidores' ? 'Seguidores' : 'Siguiendo'}
          </Link>
        ))}
      </nav>

      {pedido.error ? (
        <LoadError error={pedido.error} onRetry={pedido.reintentar} />
      ) : !gente ? (
        <p role="status" className="text-sm text-base-content/70">
          Cargando…
        </p>
      ) : gente.length === 0 ? (
        <AvisoVacio accion={soyYo ? { texto: 'Buscar gente', a: '/buscar' } : undefined}>
          {lista === 'seguidores'
            ? soyYo ? 'Todavía no tenés seguidores.' : 'Todavía no tiene seguidores.'
            : soyYo ? 'Todavía no seguís a nadie.' : 'Todavía no sigue a nadie.'}
        </AvisoVacio>
      ) : (
        <ul className="flex flex-col gap-2">
          {gente.map((persona) => (
            <FilaDePersona
              key={persona.userId}
              persona={persona}
              esYo={persona.userId === user?.userId}
              onCambio={(loSigo) => cambioDeSeguimiento(persona.username, loSigo)}
            />
          ))}
        </ul>
      )}
    </div>
  )
}
