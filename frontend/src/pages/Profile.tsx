import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { LoadError } from '../components/LoadError'
import { JointPhoto } from '../components/JointPhoto'
import { IconChevronRight, IconMedal, IconSettings, IconUser } from '../components/icons'
import { SavedTourCard } from '../components/SavedTourCard'
import { ScoreBadge } from '../components/ScoreBadge'
import { useAuth } from '../context/AuthContext'
import type { BurgerJoint, ReseniaDePerfil, ProfileStats, SavedTour } from '../types'
import { isSessionExpired } from '../utils/errors'
import { relativeDate } from '../utils/relativeDate'

export function Profile() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [stats, setStats] = useState<ProfileStats | null>(null)
  const [ratings, setRatings] = useState<ReseniaDePerfil[]>([])
  const [favorites, setFavorites] = useState<BurgerJoint[]>([])
  const [tours, setTours] = useState<SavedTour[]>([])
  const [borrando, setBorrando] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    // RequireAuth guarantees there is a session by the time this renders.
    Promise.all([
      apiClient.get<ProfileStats>('/profile/stats'),
      apiClient.get<ReseniaDePerfil[]>('/profile/ratings'),
      apiClient.get<BurgerJoint[]>('/wishlist'),
      apiClient.get<SavedTour[]>('/tours/mios'),
    ])
      .then(([statsRes, ratingsRes, wishlistRes, toursRes]) => {
        setStats(statsRes.data)
        setRatings(ratingsRes.data)
        setFavorites(wishlistRes.data.slice(0, 5))
        setTours(toursRes.data)
      })
      .catch((err) => {
        if (isSessionExpired(err)) navigate('/login')
        else setError(err)
      })
      .finally(() => setLoading(false))
  }, [user, navigate, attempt])

  const recentRatings = ratings.slice(0, 3)

  function borrarTour(id: number) {
    setBorrando(id)
    apiClient
      .delete(`/tours/${id}`)
      // Se saca de la lista acá en vez de volver a pedirla: es una fila menos, y pedir
      // todo el perfil de nuevo haría parpadear lo que no cambió.
      .then(() => setTours((previos) => previos.filter((tour) => tour.id !== id)))
      .catch((err) => {
        if (isSessionExpired(err)) navigate('/login')
        else setError(err)
      })
      .finally(() => setBorrando(null))
  }

  // RequireAuth already guarantees this, but the compiler cannot see through it and
  // the name is read below.
  if (!user) return null

  return (
    <div className="flex flex-col gap-6 p-4 md:mx-auto md:max-w-3xl md:p-0">
      {error !== null && <LoadError error={error} onRetry={() => {
          setError(null)
          setLoading(true)
          setAttempt((n) => n + 1)
        }} />}
      <div className="flex items-center justify-between">
        <h1 className="font-display text-2xl font-bold">Mi perfil</h1>
        <button
          className="btn btn-ghost btn-circle text-base-content/50"
          title="Configuración (próximamente)"
          disabled
        >
          <IconSettings size={20} />
        </button>
      </div>

      <div className="flex items-center gap-4">
        <div className="flex h-16 w-16 flex-none items-center justify-center rounded-full bg-neutral text-secondary">
          <IconUser size={30} />
        </div>
        <div className="flex min-w-0 flex-col gap-1.5">
          <span className="font-display truncate text-lg font-bold">@{user.username}</span>
          <span className="inline-flex w-fit items-center gap-1.5 rounded-full bg-secondary/30 px-3 py-1 text-xs font-semibold text-neutral">
            <IconMedal size={14} />
            Nivel hamburguesero: próximamente
          </span>
        </div>
      </div>

      <section className="flex flex-col gap-3">
        <h2 className="font-display text-sm font-bold">Mis estadísticas</h2>
        <div className="grid grid-cols-3 gap-3 rounded-box bg-base-100 p-4 ring-1 ring-inset ring-base-content/15">
          <div className="flex flex-col items-center gap-1 text-center">
            <span className="font-display text-xl font-bold">{stats?.ratingsCount ?? '—'}</span>
            <span className="text-xs text-base-content/60">Reseñas</span>
          </div>
          <div className="flex flex-col items-center gap-1 text-center">
            <span className="font-display text-xl font-bold">
              {stats?.averageScore ? stats.averageScore.toFixed(1) : '—'}
            </span>
            <span className="text-xs text-base-content/60">Puntaje promedio</span>
          </div>
          <div className="flex flex-col items-center gap-1 text-center">
            <span className="font-display text-xl font-bold text-base-content/40">—</span>
            <span className="text-xs text-base-content/60">Nivel (próximamente)</span>
          </div>
        </div>
      </section>

      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-sm font-bold">Historial</h2>
          {ratings.length > 0 && (
            <Link to="/reviews" className="flex items-center gap-1 text-xs font-semibold text-primary">
              Ver todo
              <IconChevronRight size={14} />
            </Link>
          )}
        </div>
        {loading && <p className="text-sm text-base-content/60">Cargando...</p>}
        <div className="flex flex-col gap-2">
          {recentRatings.map((r) => (
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
                <span className="text-xs text-base-content/50">{relativeDate(r.createdAt)}</span>
              </div>
              <ScoreBadge score={r.score} size="sm" />
            </Link>
          ))}
          {!loading && !error && recentRatings.length === 0 && (
            <p className="text-sm text-base-content/60">Todavía no calificaste ninguna hamburguesería.</p>
          )}
        </div>
      </section>

      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-sm font-bold">Favoritos</h2>
          <Link to="/wishlist" className="flex items-center gap-1 text-xs font-semibold text-primary">
            Ver todo
            <IconChevronRight size={14} />
          </Link>
        </div>
        {favorites.length > 0 ? (
          <div className="flex gap-3 overflow-x-auto pb-1">
            {favorites.map((b) => (
              <Link
                key={b.id}
                to={`/burger-joints/${b.id}`}
                className="w-40 flex-none overflow-hidden rounded-box bg-base-100 ring-1 ring-inset ring-base-content/15"
              >
                <JointPhoto src={b.photoUrl} name={b.name} className="h-24 w-full object-cover" />
                <div className="flex flex-col gap-1 p-2.5">
                  <span className="truncate text-sm font-semibold">{b.name}</span>
                  {b.averageScore ? (
                    <ScoreBadge score={b.averageScore} size="sm" />
                  ) : (
                    <span className="text-xs text-base-content/50">Sin calificaciones</span>
                  )}
                </div>
              </Link>
            ))}
          </div>
        ) : (
          !loading && !error && <p className="text-sm text-base-content/60">Todavía no guardaste ninguna hamburguesería.</p>
        )}
      </section>

      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-sm font-bold">Mis recorridos</h2>
          <Link to="/tour" className="flex items-center gap-1 text-xs font-semibold text-primary">
            Armar otro
            <IconChevronRight size={14} />
          </Link>
        </div>
        <div className="flex flex-col gap-3">
          {tours.map((tour) => (
            <SavedTourCard
              key={tour.id}
              tour={tour}
              onBorrar={borrarTour}
              borrando={borrando === tour.id}
            />
          ))}
          {!loading && tours.length === 0 && (
            <p className="text-sm text-base-content/60">
              Todavía no guardaste ningún recorrido.
            </p>
          )}
        </div>
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="font-display text-sm font-bold">Logros</h2>
        <div className="rounded-box bg-base-100 p-4 ring-1 ring-inset ring-base-content/15">
          <p className="text-sm text-base-content/60">
            Muy pronto vas a poder desbloquear logros a medida que calificás y descubrís hamburgueserías.
          </p>
        </div>
      </section>
    </div>
  )
}
