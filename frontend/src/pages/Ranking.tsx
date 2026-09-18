import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconPin } from '../components/icons'
import { ScoreBadge } from '../components/ScoreBadge'
import { useAuth } from '../context/AuthContext'
import type { PageResponse, RankingItem } from '../types'
import { mapsUrl } from '../utils/maps'

type Tab = 'general' | 'mine'
type Order = 'score' | 'popularity'

export function Ranking() {
  const { user } = useAuth()
  const [tab, setTab] = useState<Tab>('general')
  const [order, setOrder] = useState<Order>('score')
  const [items, setItems] = useState<RankingItem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    const request =
      tab === 'general'
        ? apiClient.get<PageResponse<RankingItem>>('/ranking/general', { params: { order } })
        : apiClient.get<PageResponse<RankingItem>>('/ranking/mine')

    request.then(({ data }) => setItems(data.content)).finally(() => setLoading(false))
  }, [tab, order])

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-2xl md:p-0">
      <h1 className="font-display text-2xl font-bold">Ranking</h1>

      <div role="tablist" className="flex w-full gap-1 rounded-full border border-base-300 bg-base-100 p-1">
        <button
          role="tab"
          className={`flex-1 rounded-full px-4 py-2 text-sm font-semibold transition-colors ${
            tab === 'general' ? 'bg-neutral text-secondary' : 'text-base-content'
          }`}
          onClick={() => setTab('general')}
        >
          Ranking general
        </button>
        <button
          role="tab"
          className={`flex-1 rounded-full px-4 py-2 text-sm font-semibold transition-colors ${
            tab === 'mine' ? 'bg-neutral text-secondary' : 'text-base-content'
          }`}
          onClick={() => setTab('mine')}
        >
          Mi ranking
        </button>
      </div>

      {tab === 'general' && (
        <div className="flex gap-2 text-xs">
          <button
            onClick={() => setOrder('score')}
            className={`btn btn-xs rounded-full ${order === 'score' ? 'btn-primary' : 'btn-outline'}`}
          >
            Mejor calificadas
          </button>
          <button
            onClick={() => setOrder('popularity')}
            className={`btn btn-xs rounded-full ${order === 'popularity' ? 'btn-primary' : 'btn-outline'}`}
          >
            Más populares
          </button>
        </div>
      )}

      {tab === 'mine' && !user && (
        <p className="text-sm text-base-content/60">Iniciá sesión para ver las hamburgueserías que calificaste.</p>
      )}

      {loading && <p className="text-sm text-base-content/60">Cargando...</p>}

      <ol className="flex flex-col gap-3">
        {items.map((item, index) => (
          <li key={item.burgerJointId} className="rounded-box bg-base-100 ring-1 ring-inset ring-base-content/15">
            <Link to={`/burger-joints/${item.burgerJointId}`} className="flex items-center gap-3 p-3 pb-2">
              <span className="w-5 text-center font-display text-sm font-bold text-base-content/40">{index + 1}</span>
              <img
                src={item.photoUrl ?? 'https://placehold.co/60x60?text=%F0%9F%8D%94'}
                alt={item.name}
                className="h-12 w-12 rounded-lg object-cover"
              />
              <div className="flex flex-1 flex-col overflow-hidden">
                <span className="truncate font-display font-semibold">{item.name}</span>
                <span className="truncate text-xs text-base-content/60">{item.address}</span>
              </div>
              <div className="flex flex-col items-end gap-1 text-right text-sm">
                <ScoreBadge score={tab === 'mine' ? (item.myScore ?? item.averageScore) : item.averageScore} size="sm" />
                {tab === 'general' ? (
                  <div className="text-xs text-base-content/40">{item.ratingsCount} reseñas</div>
                ) : (
                  <div className="text-xs text-base-content/40">general: {item.averageScore.toFixed(1)}</div>
                )}
              </div>
            </Link>
            <a
              href={mapsUrl(item.latitude, item.longitude, `${item.name} ${item.address}`)}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1 p-3 pt-0 pl-11 text-xs font-semibold text-primary"
            >
              <IconPin />
              Ver en Maps
            </a>
          </li>
        ))}
        {!loading && items.length === 0 && (
          <p className="text-sm text-base-content/60">
            {tab === 'mine' ? 'Todavía no calificaste ninguna hamburguesería.' : 'Todavía no hay calificaciones.'}
          </p>
        )}
      </ol>
    </div>
  )
}
