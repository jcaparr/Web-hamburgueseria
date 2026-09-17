import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { Estrellas } from '../components/Estrellas'
import { useAuth } from '../context/AuthContext'
import type { Calificacion, Hamburgueseria, PageResponse } from '../types'

export function DetalleHamburgueseria() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { usuario } = useAuth()

  const [hamburgueseria, setHamburgueseria] = useState<Hamburgueseria | null>(null)
  const [calificaciones, setCalificaciones] = useState<Calificacion[]>([])
  const [puntaje, setPuntaje] = useState(0)
  const [comentario, setComentario] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function cargar() {
    apiClient.get<Hamburgueseria>(`/hamburguesuerias/${id}`).then(({ data }) => setHamburgueseria(data))
    apiClient
      .get<PageResponse<Calificacion>>(`/hamburguesuerias/${id}/calificaciones`)
      .then(({ data }) => setCalificaciones(data.content))
  }

  useEffect(cargar, [id])

  async function toggleWishlist() {
    if (!usuario) return navigate('/login')
    if (!hamburgueseria) return

    if (hamburgueseria.enListaDeseados) {
      await apiClient.delete(`/wishlist/${hamburgueseria.id}`)
    } else {
      await apiClient.post(`/wishlist/${hamburgueseria.id}`)
    }
    cargar()
  }

  async function enviarResena(e: FormEvent) {
    e.preventDefault()
    if (!usuario) return navigate('/login')
    if (puntaje === 0) {
      setError('Elegí una calificación de 1 a 5 estrellas')
      return
    }

    setEnviando(true)
    setError(null)
    try {
      await apiClient.post(`/hamburguesuerias/${id}/calificaciones`, { puntaje, comentario })
      setPuntaje(0)
      setComentario('')
      cargar()
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'No pudimos guardar tu reseña')
    } finally {
      setEnviando(false)
    }
  }

  if (!hamburgueseria) return <p className="p-4 text-sm text-neutral-400">Cargando...</p>

  return (
    <div className="flex flex-col gap-4 p-4">
      <img
        src={hamburgueseria.fotoUrl ?? 'https://placehold.co/600x300?text=%F0%9F%8D%94'}
        alt={hamburgueseria.nombre}
        className="h-48 w-full rounded-xl object-cover"
      />

      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-xl font-semibold">{hamburgueseria.nombre}</h1>
          <p className="text-sm text-neutral-500">{hamburgueseria.direccion}</p>
          <p className="text-sm text-amber-600">
            {hamburgueseria.promedio
              ? `★ ${hamburgueseria.promedio.toFixed(1)} (${hamburgueseria.cantidadCalificaciones} reseñas)`
              : 'Todavía sin calificaciones'}
          </p>
        </div>

        <button
          onClick={toggleWishlist}
          aria-pressed={hamburgueseria.enListaDeseados}
          className="text-2xl"
          title={hamburgueseria.enListaDeseados ? 'Quitar de deseados' : 'Guardar en deseados'}
        >
          {hamburgueseria.enListaDeseados ? '❤️' : '🤍'}
        </button>
      </div>

      <form onSubmit={enviarResena} className="flex flex-col gap-2 rounded-xl border border-neutral-200 p-3">
        <span className="text-sm font-medium">Dejá tu opinión</span>
        <Estrellas valor={puntaje} onChange={setPuntaje} size={28} />
        <textarea
          value={comentario}
          onChange={(e) => setComentario(e.target.value)}
          placeholder="¿Qué te pareció?"
          maxLength={1000}
          className="min-h-20 rounded-lg border border-neutral-300 p-2 text-sm outline-none focus:border-amber-500"
        />
        {error && <p className="text-xs text-red-600">{error}</p>}
        <button
          type="submit"
          disabled={enviando}
          className="rounded-full bg-amber-500 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {enviando ? 'Guardando...' : 'Publicar reseña'}
        </button>
      </form>

      <div className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold">Reseñas ({calificaciones.length})</h2>
        {calificaciones.map((c) => (
          <div key={c.id} className="rounded-lg border border-neutral-200 p-3">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">{c.usuarioNombre}</span>
              <Estrellas valor={c.puntaje} size={14} />
            </div>
            {c.comentario && <p className="mt-1 text-sm text-neutral-600">{c.comentario}</p>}
          </div>
        ))}
        {calificaciones.length === 0 && (
          <p className="text-sm text-neutral-400">Sé el primero en dejar una reseña.</p>
        )}
      </div>
    </div>
  )
}
