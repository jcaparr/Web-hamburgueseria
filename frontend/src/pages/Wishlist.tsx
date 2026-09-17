import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { useAuth } from '../context/AuthContext'
import type { BurgerJoint } from '../types'

export function Wishlist() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [items, setItems] = useState<BurgerJoint[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) {
      navigate('/login')
      return
    }
    apiClient
      .get<BurgerJoint[]>('/wishlist')
      .then(({ data }) => setItems(data))
      .finally(() => setLoading(false))
  }, [user, navigate])

  async function remove(id: number) {
    await apiClient.delete(`/wishlist/${id}`)
    setItems((prev) => prev.filter((b) => b.id !== id))
  }

  return (
    <div className="flex flex-col gap-4 p-4">
      <h1 className="text-xl font-semibold">Lista de deseados</h1>

      {loading && <p className="text-sm text-neutral-400">Cargando...</p>}

      <ul className="flex flex-col gap-3">
        {items.map((b) => (
          <li key={b.id} className="flex items-center gap-3 rounded-xl border border-neutral-200 p-3">
            <Link to={`/burger-joints/${b.id}`} className="flex flex-1 items-center gap-3">
              <img
                src={b.photoUrl ?? 'https://placehold.co/64x64?text=%F0%9F%8D%94'}
                alt={b.name}
                className="h-14 w-14 rounded-lg object-cover"
              />
              <div className="flex flex-col">
                <span className="font-medium">{b.name}</span>
                <span className="text-xs text-neutral-500">{b.address}</span>
              </div>
            </Link>
            <button onClick={() => remove(b.id)} className="text-xl" title="Quitar de deseados">
              ❤️
            </button>
          </li>
        ))}
        {!loading && items.length === 0 && (
          <p className="text-sm text-neutral-400">Todavía no guardaste ninguna hamburguesería.</p>
        )}
      </ul>
    </div>
  )
}
