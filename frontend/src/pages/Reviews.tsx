import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { LoadError } from '../components/LoadError'
import { JointPhoto } from '../components/JointPhoto'
import { ScoreBadge } from '../components/ScoreBadge'
import { useAuth } from '../context/AuthContext'
import type { MyRating } from '../types'
import { isSessionExpired } from '../utils/errors'
import { relativeDate } from '../utils/relativeDate'

export function Reviews() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [ratings, setRatings] = useState<MyRating[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    // RequireAuth guarantees there is a session by the time this renders.
    apiClient
      .get<MyRating[]>('/profile/ratings')
      .then(({ data }) => setRatings(data))
      .catch((err) => {
        if (isSessionExpired(err)) navigate('/login')
        else setError(err)
      })
      .finally(() => setLoading(false))
  }, [user, navigate, attempt])

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-2xl md:p-0">
      <h1 className="font-display text-2xl font-bold">Mis reseñas</h1>

      {loading && <p className="text-sm text-base-content/60">Cargando...</p>}
      {error !== null && <LoadError error={error} onRetry={() => {
          setError(null)
          setLoading(true)
          setAttempt((n) => n + 1)
        }} />}

      <ul className="flex flex-col gap-3">
        {ratings.map((r) => (
          <li key={r.id} className="rounded-box bg-base-100 p-3 ring-1 ring-inset ring-base-content/15">
            <Link to={`/burger-joints/${r.burgerJointId}`} className="flex items-center gap-3">
              <JointPhoto
                src={r.photoUrl}
                name={r.burgerJointName}
                className="h-12 w-12 flex-none rounded-lg object-cover"
              />
              <div className="flex flex-1 flex-col overflow-hidden">
                <span className="truncate font-medium">{r.burgerJointName}</span>
                <span className="text-xs text-base-content/50">{relativeDate(r.createdAt)}</span>
              </div>
              <ScoreBadge score={r.score} size="sm" />
            </Link>
            {r.comment && <p className="mt-2 text-sm text-base-content/70">{r.comment}</p>}
          </li>
        ))}
        {!loading && !error && ratings.length === 0 && (
          <p className="text-sm text-base-content/60">Todavía no calificaste ninguna hamburguesería.</p>
        )}
      </ul>
    </div>
  )
}
