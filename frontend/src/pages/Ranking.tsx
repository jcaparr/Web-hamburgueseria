import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import { useAuth } from '../context/AuthContext'
import type { PageResponse, RankingItem } from '../types'

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
    <div className="flex flex-col gap-4 p-4">
      <h1 className="text-xl font-semibold">Ranking</h1>

      <div className="flex rounded-full bg-neutral-100 p-1 text-sm">
        <button
          onClick={() => setTab('general')}
          className={`flex-1 rounded-full py-1.5 ${tab === 'general' ? 'bg-white font-semibold shadow' : 'text-neutral-500'}`}
        >
          Rankeadas por la gente
        </button>
        <button
          onClick={() => setTab('mine')}
          className={`flex-1 rounded-full py-1.5 ${tab === 'mine' ? 'bg-white font-semibold shadow' : 'text-neutral-500'}`}
        >
          Mi ranking
        </button>
      </div>

      {tab === 'general' && (
        <div className="flex gap-2 text-xs">
          <button
            onClick={() => setOrder('score')}
            className={`rounded-full border px-3 py-1 ${order === 'score' ? 'border-amber-500 text-amber-600' : 'border-neutral-300 text-neutral-500'}`}
          >
            Mejor calificadas
          </button>
          <button
            onClick={() => setOrder('popularity')}
            className={`rounded-full border px-3 py-1 ${order === 'popularity' ? 'border-amber-500 text-amber-600' : 'border-neutral-300 text-neutral-500'}`}
          >
            Más populares
          </button>
        </div>
      )}

      {tab === 'mine' && !user && (
        <p className="text-sm text-neutral-400">Iniciá sesión para ver las hamburgueserías que calificaste.</p>
      )}

      {loading && <p className="text-sm text-neutral-400">Cargando...</p>}

      <ol className="flex flex-col gap-3">
        {items.map((item, index) => (
          <li key={item.burgerJointId}>
            <Link
              to={`/burger-joints/${item.burgerJointId}`}
              className="flex items-center gap-3 rounded-xl border border-neutral-200 p-3"
            >
              <span className="w-5 text-center text-sm font-semibold text-neutral-400">{index + 1}</span>
              <img
                src={item.photoUrl ?? 'https://placehold.co/60x60?text=%F0%9F%8D%94'}
                alt={item.name}
                className="h-12 w-12 rounded-lg object-cover"
              />
              <div className="flex flex-1 flex-col">
                <span className="font-medium">{item.name}</span>
                <span className="text-xs text-neutral-500">{item.address}</span>
              </div>
              <div className="text-right text-sm">
                <div className="text-amber-600">★ {item.averageScore.toFixed(1)}</div>
                {tab === 'general' ? (
                  <div className="text-xs text-neutral-400">{item.ratingsCount} reseñas</div>
                ) : (
                  item.myScore != null && (
                    <div className="text-xs text-neutral-400">vos: {item.myScore}★</div>
                  )
                )}
              </div>
            </Link>
          </li>
        ))}
        {!loading && items.length === 0 && (
          <p className="text-sm text-neutral-400">
            {tab === 'mine' ? 'Todavía no calificaste ninguna hamburguesería.' : 'Todavía no hay calificaciones.'}
          </p>
        )}
      </ol>
    </div>
  )
}
