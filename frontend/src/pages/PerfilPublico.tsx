import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { BotonBloquear } from '../components/BotonBloquear'
import { BotonSeguir } from '../components/BotonSeguir'
import { IconUser } from '../components/icons'
import { JointPhoto } from '../components/JointPhoto'
import { LoadError } from '../components/LoadError'
import { ScoreBadge } from '../components/ScoreBadge'
import { relativeDate } from '../utils/relativeDate'
import type { PerfilPublico as Perfil } from '../types'

/**
 * El perfil de otra persona.
 *
 * Muestra sus reseñas y sus números, que es lo que hace falta para decidir si vale la
 * pena seguirla. Sus favoritos no: marcar una hamburguesería para ir algún día es una
 * intención, no una opinión publicada.
 */
export function PerfilPublico() {
  const { username = '' } = useParams()
  const navigate = useNavigate()
  const [perfil, setPerfil] = useState<Perfil | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [cargando, setCargando] = useState(true)

  const cargar = useCallback(() => {
    setCargando(true)
    apiClient
      .get<Perfil>(`/usuarios/${username}`)
      .then(({ data }) => {
        setPerfil(data)
        setError(null)
      })
      .catch(setError)
      .finally(() => setCargando(false))
  }, [username])

  useEffect(cargar, [cargar])

  // El servidor manda los contadores; al seguir o dejar de seguir se corrigen acá, para
  // que el número no quede contradiciendo al botón que se acaba de tocar.
  function cambioDeSeguimiento(loSigo: boolean) {
    setPerfil((previo) =>
      previo === null
        ? previo
        : { ...previo, loSigo, seguidores: previo.seguidores + (loSigo ? 1 : -1) },
    )
  }

  if (error) {
    return (
      <div className="p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
        <LoadError error={error} onRetry={cargar} />
      </div>
    )
  }

  if (cargando || !perfil) {
    return <p className="p-4 text-sm text-base-content/70">Cargando...</p>
  }

  return (
    <div className="flex flex-col gap-6 p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
      <div className="flex items-center gap-4">
        <div className="flex h-16 w-16 flex-none items-center justify-center rounded-full bg-neutral text-secondary">
          <IconUser size={30} />
        </div>
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <span className="font-display truncate text-lg font-bold">@{perfil.username}</span>
          <span className="text-sm text-base-content/70">
            {perfil.seguidores === 1 ? '1 seguidor' : `${perfil.seguidores} seguidores`}
            {' · '}
            sigue a {perfil.siguiendo}
          </span>
        </div>
        {!perfil.soyYo && (
          <BotonSeguir
            username={perfil.username}
            loSigo={perfil.loSigo}
            onCambio={cambioDeSeguimiento}
          />
        )}
      </div>

      <div className="grid grid-cols-2 gap-3 rounded-box bg-base-100 p-4 ring-1 ring-inset ring-base-content/15">
        <div className="flex flex-col items-center gap-1 text-center">
          <span className="font-display text-xl font-bold">{perfil.resenias}</span>
          <span className="text-xs text-base-content/70">Reseñas</span>
        </div>
        <div className="flex flex-col items-center gap-1 text-center">
          <span className="font-display text-xl font-bold">
            {perfil.promedio ? perfil.promedio.toFixed(1) : '—'}
          </span>
          <span className="text-xs text-base-content/70">Puntaje promedio</span>
        </div>
      </div>

      {!perfil.soyYo && (
        <BotonBloquear
          username={perfil.username}
          // Bloqueado, este perfil ya no se puede pedir: quedarse acá mostraría un
          // error donde hace un segundo había una persona.
          onBloqueado={() => navigate('/feed')}
        />
      )}

      <section className="flex flex-col gap-3">
        <h2 className="font-display text-sm font-bold">Últimas reseñas</h2>
        <div className="flex flex-col gap-2">
          {perfil.ultimasResenias.map((r) => (
            <Link
              key={r.id}
              to={`/burger-joints/${r.burgerJointId}`}
              className="flex items-center gap-3 rounded-box bg-base-100 p-3 ring-1 ring-inset ring-base-content/15"
            >
              <JointPhoto
                src={r.photoUrl}
                name={r.burgerJointName}
                className="h-12 w-12 flex-none rounded-lg object-cover"
              />
              <div className="flex flex-1 flex-col overflow-hidden">
                <span className="truncate font-medium">{r.burgerJointName}</span>
                <span className="text-xs text-base-content/70">{relativeDate(r.createdAt)}</span>
              </div>
              <ScoreBadge score={r.score} size="sm" />
            </Link>
          ))}
          {perfil.ultimasResenias.length === 0 && (
            <p className="text-sm text-base-content/70">
              {perfil.soyYo
                ? 'Todavía no calificaste ninguna hamburguesería.'
                : 'Todavía no calificó ninguna hamburguesería.'}
            </p>
          )}
        </div>
      </section>
    </div>
  )
}
