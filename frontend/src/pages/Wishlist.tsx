import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { useAuth } from '../context/AuthContext'
import type { Hamburgueseria } from '../types'

export function Wishlist() {
  const { usuario } = useAuth()
  const navigate = useNavigate()
  const [items, setItems] = useState<Hamburgueseria[]>([])
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    if (!usuario) {
      navigate('/login')
      return
    }
    apiClient
      .get<Hamburgueseria[]>('/wishlist')
      .then(({ data }) => setItems(data))
      .finally(() => setCargando(false))
  }, [usuario, navigate])

  async function quitar(id: number) {
    await apiClient.delete(`/wishlist/${id}`)
    setItems((prev) => prev.filter((h) => h.id !== id))
  }

  return (
    <div className="flex flex-col gap-4 p-4">
      <h1 className="text-xl font-semibold">Lista de deseados</h1>

      {cargando && <p className="text-sm text-neutral-400">Cargando...</p>}

      <ul className="flex flex-col gap-3">
        {items.map((h) => (
          <li key={h.id} className="flex items-center gap-3 rounded-xl border border-neutral-200 p-3">
            <Link to={`/hamburguesuerias/${h.id}`} className="flex flex-1 items-center gap-3">
              <img
                src={h.fotoUrl ?? 'https://placehold.co/64x64?text=%F0%9F%8D%94'}
                alt={h.nombre}
                className="h-14 w-14 rounded-lg object-cover"
              />
              <div className="flex flex-col">
                <span className="font-medium">{h.nombre}</span>
                <span className="text-xs text-neutral-500">{h.direccion}</span>
              </div>
            </Link>
            <button onClick={() => quitar(h.id)} className="text-xl" title="Quitar de deseados">
              ❤️
            </button>
          </li>
        ))}
        {!cargando && items.length === 0 && (
          <p className="text-sm text-neutral-400">Todavía no guardaste ninguna hamburguesería.</p>
        )}
      </ul>
    </div>
  )
}
