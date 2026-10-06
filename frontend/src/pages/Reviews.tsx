import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconPencil } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { JointPhoto } from '../components/JointPhoto'
import { ScoreBadge } from '../components/ScoreBadge'
import { useAuth } from '../context/useAuth'
import { useTitulo } from '../hooks/useTitulo'
import type { ReseniaDePerfil } from '../types'
import { isSessionExpired } from '../utils/errors'
import { relativeDate } from '../utils/relativeDate'

export function Reviews() {
  const { user } = useAuth()
  useTitulo('Mis reseñas')
  const navigate = useNavigate()
  const [ratings, setRatings] = useState<ReseniaDePerfil[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    // RequireAuth guarantees there is a session by the time this renders.
    apiClient
      .get<ReseniaDePerfil[]>('/profile/ratings')
      .then(({ data }) => setRatings(data))
      .catch((err) => {
        if (isSessionExpired(err)) navigate('/login')
        else setError(err)
      })
      .finally(() => setLoading(false))
  }, [user, navigate, attempt])

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-2xl md:p-0">
      <h1 className="titulo-pagina">Mis reseñas</h1>

      {loading && <p role="status" className="text-sm text-base-content/70">Cargando…</p>}
      {error !== null && <LoadError error={error} onRetry={() => {
          setError(null)
          setLoading(true)
          setAttempt((n) => n + 1)
        }} />}

      <ul className="flex flex-col gap-3">
        {ratings.map((r) => (
          <li key={r.id} className="tarjeta p-3">
            <Link to={`/burger-joints/${r.burgerJointId}`} className="flex items-center gap-3">
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
            {r.comment && <p className="mt-2 text-sm text-base-content/70">{r.comment}</p>}
            {/* Lleva a la ficha del local con la ventana de reseñar ya abierta. Editar
                era: entrar al local y bajar a buscar el formulario. */}
            <Link
              to={`/burger-joints/${r.burgerJointId}?opinar=1`}
              className="btn btn-ghost btn-sm mt-2 gap-1.5 text-base-content/70"
            >
              <IconPencil size={14} />
              Editar reseña
              <span className="sr-only"> de {r.burgerJointName}</span>
            </Link>
          </li>
        ))}
      </ul>

      {!loading && !error && ratings.length === 0 && (
        <div className="flex flex-col items-start gap-3">
          <p className="text-sm text-base-content/70">
            Todavía no calificaste ninguna hamburguesería.
          </p>
          <Link to="/" className="btn btn-primary btn-sm">
            Buscar una para calificar
          </Link>
        </div>
      )}
    </div>
  )
}
