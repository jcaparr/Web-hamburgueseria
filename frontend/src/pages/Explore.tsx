import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import type { BurgerJoint, PageResponse } from '../types'

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
    <div className="flex flex-col gap-4 p-4">
      <h1 className="text-xl font-semibold">Explorar hamburgueserías</h1>

      <input
        type="search"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Buscar hamburguesería..."
        className="rounded-full border border-neutral-300 px-4 py-2 text-sm outline-none focus:border-amber-500"
      />

      {loading && <p className="text-sm text-neutral-400">Buscando...</p>}

      <ul className="flex flex-col gap-3">
        {items.map((b) => (
          <li key={b.id}>
            <Link
              to={`/burger-joints/${b.id}`}
              className="flex gap-3 rounded-xl border border-neutral-200 p-3 hover:border-amber-400"
            >
              <img
                src={b.photoUrl ?? 'https://placehold.co/80x80?text=%F0%9F%8D%94'}
                alt={b.name}
                className="h-16 w-16 rounded-lg object-cover"
              />
              <div className="flex flex-col justify-center gap-1">
                <span className="font-medium">{b.name}</span>
                <span className="text-xs text-neutral-500">{b.address}</span>
                <span className="text-xs text-amber-600">
                  {b.averageScore ? `★ ${b.averageScore.toFixed(1)} (${b.ratingsCount})` : 'Sin calificaciones'}
                </span>
              </div>
            </Link>
          </li>
        ))}
        {!loading && items.length === 0 && (
          <p className="text-sm text-neutral-400">No encontramos hamburgueserías con ese nombre.</p>
        )}
      </ul>
    </div>
  )
}
