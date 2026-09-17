import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import type { BurgerJoint, PageResponse } from '../types'
import { mapsUrl } from '../utils/maps'

export function Explore() {
  const [query, setQuery] = useState('')
  const [items, setItems] = useState<BurgerJoint[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const timeout = setTimeout(() => {
      setLoading(true)
      apiClient
        .get<PageResponse<BurgerJoint>>('/burger-joints', { params: { q: query || undefined } })
        .then(({ data }) => setItems(data.content))
        .finally(() => setLoading(false))
    }, 300)

    return () => clearTimeout(timeout)
  }, [query])

  return (
    <div className="flex flex-col gap-4 p-4 md:p-0">
      <h1 className="text-xl font-semibold">Explorar hamburgueserías</h1>

      <input
        type="search"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Buscar hamburguesería..."
        className="input input-bordered w-full rounded-full focus:border-primary"
      />

      {loading && <p className="text-sm text-base-content/60">Buscando...</p>}

      <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((b) => (
          <li key={b.id} className="card card-border hover:border-primary">
            <Link to={`/burger-joints/${b.id}`} className="flex flex-1 gap-3 p-3">
              <img
                src={b.photoUrl ?? 'https://placehold.co/80x80?text=%F0%9F%8D%94'}
                alt={b.name}
                className="h-16 w-16 rounded-lg object-cover"
              />
              <div className="flex flex-col justify-center gap-1 overflow-hidden">
                <span className="truncate font-medium">{b.name}</span>
                <span className="truncate text-xs text-base-content/60">{b.address}</span>
                <span className="text-xs text-primary">
                  {b.averageScore ? `★ ${b.averageScore.toFixed(1)} (${b.ratingsCount})` : 'Sin calificaciones'}
                </span>
              </div>
            </Link>
            <a
              href={mapsUrl(b.latitude, b.longitude, `${b.name} ${b.address}`)}
              target="_blank"
              rel="noopener noreferrer"
              className="link link-hover self-end pr-3 pb-3 text-xs text-info"
            >
              📍 Ver en Maps
            </a>
          </li>
        ))}
        {!loading && items.length === 0 && (
          <p className="text-sm text-base-content/60">No encontramos hamburgueserías con ese nombre.</p>
        )}
      </ul>
    </div>
  )
}
