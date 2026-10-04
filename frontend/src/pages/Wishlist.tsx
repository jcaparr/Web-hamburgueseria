import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconHeart, IconPin } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { JointPhoto } from '../components/JointPhoto'
import { useAuth } from '../context/AuthContext'
import type { BurgerJoint } from '../types'
import { isSessionExpired } from '../utils/errors'
import { shortAddress } from '../utils/address'
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
      <h1 className="font-display text-2xl font-bold leading-tight md:text-3xl">Guardadas</h1>

      {loading && <p className="text-sm text-base-content/70">Cargando…</p>}
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
                <JointPhoto
                  src={b.photoUrl}
                  name={b.name}
                  className="h-14 w-14 flex-none rounded-lg object-cover"
                />
                <div className="flex flex-col overflow-hidden">
                  <span className="truncate font-display font-semibold">{b.name}</span>
                  <span className="truncate text-xs text-base-content/70">{shortAddress(b.address, b.area)}</span>
                </div>
              </Link>
              <button
                onClick={() => remove(b.id)}
                className="btn btn-ghost btn-circle text-primary"
                aria-label={`Quitar ${b.name} de guardadas`}
                title="Quitar de guardadas"
              >
                <IconHeart size={20} filled />
              </button>
            </div>
            <a
              href={mapsUrl(b.placeId, b.name, b.latitude, b.longitude)}
              target="_blank"
              rel="noopener noreferrer"
              className="flex w-fit items-center gap-1 p-3 pt-2 text-xs font-semibold text-primary hover:underline"
            >
              <IconPin />
              Ver en Maps
            </a>
          </li>
        ))}
      </ul>

      {!loading && !error && items.length === 0 && (
        <div className="flex flex-col items-start gap-3">
          <p className="text-sm text-base-content/70">
            Todavía no guardaste ninguna. Tocá el corazón de las que quieras probar y
            aparecen acá.
          </p>
          <Link to="/" className="btn btn-primary btn-sm">
            Explorar hamburgueserías
          </Link>
        </div>
      )}
    </div>
  )
}
