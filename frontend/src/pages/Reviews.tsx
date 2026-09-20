import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
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

  useEffect(() => {
    // RequireAuth guarantees there is a session by the time this renders.
    apiClient
      .get<MyRating[]>('/profile/ratings')
      .then(({ data }) => setRatings(data))
      .catch((err) => {
        if (isSessionExpired(err)) navigate('/login')
      })
      .finally(() => setLoading(false))
  }, [user, navigate])

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-2xl md:p-0">
      <h1 className="font-display text-2xl font-bold">Mis reseñas</h1>

      {loading && <p className="text-sm text-base-content/60">Cargando...</p>}

      <ul className="flex flex-col gap-3">
        {ratings.map((r) => (
          <li key={r.id} className="rounded-box bg-base-100 p-3 ring-1 ring-inset ring-base-content/15">
            <Link to={`/burger-joints/${r.burgerJointId}`} className="flex items-center gap-3">
              <img
                src={r.photoUrl ?? 'https://placehold.co/48x48?text=%F0%9F%8D%94'}
                alt={r.burgerJointName}
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
        {!loading && ratings.length === 0 && (
          <p className="text-sm text-base-content/60">Todavía no calificaste ninguna hamburguesería.</p>
        )}
      </ul>
    </div>
  )
}
