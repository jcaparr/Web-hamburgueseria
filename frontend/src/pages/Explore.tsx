import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconPin, IconSearch } from '../components/icons'
import { ScoreBadge } from '../components/ScoreBadge'
import type { BurgerJoint, PageResponse } from '../types'
import { mapsUrl } from '../utils/maps'

const PAGE_SIZE = 20

export function Explore() {
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [pageData, setPageData] = useState<PageResponse<BurgerJoint> | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const timeout = setTimeout(() => {
      setLoading(true)
      apiClient
        .get<PageResponse<BurgerJoint>>('/burger-joints', {
          params: { q: query || undefined, page, size: PAGE_SIZE },
        })
        .then(({ data }) => setPageData(data))
        .finally(() => setLoading(false))
    }, 300)

    return () => clearTimeout(timeout)
  }, [query, page])

  const items = pageData?.content ?? []

  return (
    <div className="flex flex-col gap-4 p-4 md:p-0">
      <h1 className="font-display text-2xl font-bold">Explorar hamburgueserías</h1>

      <div className="flex items-center gap-2 rounded-xl border border-base-300 bg-base-100 px-4 py-2.5">
        <IconSearch size={16} className="text-base-content/50" />
        <input
          type="search"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value)
            setPage(0)
          }}
          placeholder="Buscar hamburguesería..."
          className="w-full bg-transparent text-sm outline-none placeholder:text-base-content/50"
        />
      </div>

      {loading && <p className="text-sm text-base-content/60">Buscando...</p>}

      <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((b) => (
          <li key={b.id} className="rounded-box bg-base-100 ring-1 ring-inset ring-base-content/15 overflow-hidden">
            <Link to={`/burger-joints/${b.id}`}>
              <figure className="aspect-[4/3] bg-base-200">
                <img
                  src={b.photoUrl ?? 'https://placehold.co/400x300?text=%F0%9F%8D%94'}
                  alt={b.name}
                  className="h-full w-full object-cover"
                />
              </figure>
              <div className="checker-strip" />
              <div className="flex flex-col gap-1 p-4">
                <h2 className="font-display line-clamp-1 text-base font-bold">{b.name}</h2>
                <p className="line-clamp-2 text-xs text-base-content/60">{b.address}</p>
                <div className="mt-2 flex items-center justify-between">
                  {b.averageScore ? (
                    <ScoreBadge score={b.averageScore} size="sm" />
                  ) : (
                    <span className="text-xs font-medium text-base-content/50">Sin calificaciones</span>
                  )}
                </div>
              </div>
            </Link>
            <div className="px-4 pb-4">
              <a
                href={mapsUrl(b.latitude, b.longitude, `${b.name} ${b.address}`)}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-1 text-xs font-semibold text-primary"
              >
                <IconPin />
                Ver en Maps
              </a>
            </div>
          </li>
        ))}
        {!loading && items.length === 0 && (
          <p className="text-sm text-base-content/60">No encontramos hamburgueserías con ese nombre.</p>
        )}
      </ul>

      {pageData && pageData.totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 py-2">
          <div className="join">
            <button
              className="btn join-item btn-sm"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              «
            </button>
            <span className="btn join-item btn-sm btn-disabled bg-base-100">
              Página {pageData.number + 1} de {pageData.totalPages}
            </span>
            <button
              className="btn join-item btn-sm"
              disabled={pageData.last}
              onClick={() => setPage((p) => p + 1)}
            >
              »
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
