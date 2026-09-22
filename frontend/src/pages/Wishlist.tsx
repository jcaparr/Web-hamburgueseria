import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconHeart, IconPin } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { useAuth } from '../context/AuthContext'
import type { BurgerJoint } from '../types'
import { isSessionExpired } from '../utils/errors'
import { mapsUrl } from '../utils/maps'

export function Wishlist() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [items, setItems] = useState<BurgerJoint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    // RequireAuth guarantees there is a session by the time this renders.
    apiClient
      .get<BurgerJoint[]>('/wishlist')
      .then(({ data }) => setItems(data))
      .catch((err) => {
        if (isSessionExpired(err)) navigate('/login')
        else setError(err)
      })
      .finally(() => setLoading(false))
  }, [user, navigate, attempt])

  async function remove(id: number) {
    try {
      await apiClient.delete(`/wishlist/${id}`)
      setItems((prev) => prev.filter((b) => b.id !== id))
    } catch (err) {
      if (isSessionExpired(err)) navigate('/login')
    }
  }

  return (
    <div className="flex flex-col gap-4 p-4 md:p-0">
      <h1 className="font-display text-2xl font-bold">Lista de deseados</h1>

      {loading && <p className="text-sm text-base-content/60">Cargando...</p>}
      {error !== null && <LoadError error={error} onRetry={() => {
          setError(null)
          setLoading(true)
          setAttempt((n) => n + 1)
        }} />}

      <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((b) => (
          <li key={b.id} className="rounded-box bg-base-100 ring-1 ring-inset ring-base-content/15">
            <div className="flex items-center gap-3 p-3 pb-0">
              <Link to={`/burger-joints/${b.id}`} className="flex flex-1 items-center gap-3 overflow-hidden">
                <img
                  src={b.photoUrl ?? 'https://placehold.co/64x64?text=%F0%9F%8D%94'}
                  alt={b.name}
                  className="h-14 w-14 rounded-lg object-cover"
                />
                <div className="flex flex-col overflow-hidden">
                  <span className="truncate font-display font-semibold">{b.name}</span>
                  <span className="truncate text-xs text-base-content/60">{b.address}</span>
                </div>
              </Link>
              <button
                onClick={() => remove(b.id)}
                className="btn btn-ghost btn-circle text-primary"
                title="Quitar de deseados"
              >
                <IconHeart size={20} filled />
              </button>
            </div>
            <a
              href={mapsUrl(b.latitude, b.longitude, `${b.name} ${b.address}`)}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1 p-3 pt-2 text-xs font-semibold text-primary"
            >
              <IconPin />
              Ver en Maps
            </a>
          </li>
        ))}
        {!loading && !error && items.length === 0 && (
          <p className="text-sm text-base-content/60">Todavía no guardaste ninguna hamburguesería.</p>
        )}
      </ul>
    </div>
  )
}
