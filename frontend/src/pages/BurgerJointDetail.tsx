import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { Stars } from '../components/Stars'
import { useAuth } from '../context/AuthContext'
import type { BurgerJoint, PageResponse, Rating } from '../types'
import { isSessionExpired } from '../utils/errors'
import { mapsUrl } from '../utils/maps'

export function BurgerJointDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [burgerJoint, setBurgerJoint] = useState<BurgerJoint | null>(null)
  const [ratings, setRatings] = useState<Rating[]>([])
  const [score, setScore] = useState(0)
  const [comment, setComment] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const myRating = user ? ratings.find((r) => r.userId === user.userId) ?? null : null

  function load() {
    apiClient.get<BurgerJoint>(`/burger-joints/${id}`).then(({ data }) => setBurgerJoint(data))
    apiClient
      .get<PageResponse<Rating>>(`/burger-joints/${id}/ratings`)
      .then(({ data }) => setRatings(data.content))
  }

  useEffect(load, [id])

  useEffect(() => {
    if (myRating) {
      setScore(myRating.score)
      setComment(myRating.comment ?? '')
    }
  }, [myRating?.id])

  async function toggleWishlist() {
    if (!user) return navigate('/login')
    if (!burgerJoint) return

    try {
      if (burgerJoint.inWishlist) {
        await apiClient.delete(`/wishlist/${burgerJoint.id}`)
      } else {
        await apiClient.post(`/wishlist/${burgerJoint.id}`)
      }
      load()
    } catch (err) {
      if (isSessionExpired(err)) {
        navigate('/login')
      } else {
        setError('No pudimos actualizar tu lista de deseados')
      }
    }
  }

  async function submitRating(e: FormEvent) {
    e.preventDefault()
    if (!user) return navigate('/login')
    if (score === 0) {
      setError('Elegí una calificación de 1 a 5 estrellas')
      return
    }

    setSubmitting(true)
    setError(null)
    try {
      if (myRating) {
        await apiClient.put(`/burger-joints/${id}/ratings`, { score, comment })
      } else {
        await apiClient.post(`/burger-joints/${id}/ratings`, { score, comment })
        setScore(0)
        setComment('')
      }
      load()
    } catch (err) {
      if (isSessionExpired(err)) {
        navigate('/login')
      } else {
        setError('No pudimos guardar tu reseña')
      }
    } finally {
      setSubmitting(false)
    }
  }

  if (!burgerJoint) return <p className="p-4 text-sm text-base-content/60">Cargando...</p>

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-4xl md:grid md:grid-cols-2 md:gap-6 md:p-0">
      <div className="flex flex-col gap-4">
        <img
          src={burgerJoint.photoUrl ?? 'https://placehold.co/600x300?text=%F0%9F%8D%94'}
          alt={burgerJoint.name}
          className="h-48 w-full rounded-xl object-cover"
        />

        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-xl font-semibold">{burgerJoint.name}</h1>
            <p className="text-sm text-base-content/60">{burgerJoint.address}</p>
            <p className="text-sm text-primary">
              {burgerJoint.averageScore
                ? `★ ${burgerJoint.averageScore.toFixed(1)} (${burgerJoint.ratingsCount} reseñas)`
                : 'Todavía sin calificaciones'}
            </p>
            <a
              href={mapsUrl(burgerJoint.latitude, burgerJoint.longitude, `${burgerJoint.name} ${burgerJoint.address}`)}
              target="_blank"
              rel="noopener noreferrer"
              className="link link-hover text-xs text-info"
            >
              📍 Ver en Maps
            </a>
          </div>

          <button
            onClick={toggleWishlist}
            aria-pressed={burgerJoint.inWishlist}
            className="btn btn-ghost btn-circle text-2xl"
            title={burgerJoint.inWishlist ? 'Quitar de deseados' : 'Guardar en deseados'}
          >
            {burgerJoint.inWishlist ? '❤️' : '🤍'}
          </button>
        </div>

        <form onSubmit={submitRating} className="card card-border flex flex-col gap-2 p-3">
          <span className="text-sm font-medium">{myRating ? 'Editá tu opinión' : 'Dejá tu opinión'}</span>
          <Stars value={score} onChange={setScore} size={28} />
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="¿Qué te pareció?"
            maxLength={1000}
            className="textarea textarea-bordered min-h-20 focus:border-primary"
          />
          {error && <p className="text-xs text-error">{error}</p>}
          <button type="submit" disabled={submitting} className="btn btn-primary rounded-full">
            {submitting ? 'Guardando...' : myRating ? 'Actualizar reseña' : 'Publicar reseña'}
          </button>
        </form>
      </div>

      <div className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold">Reseñas ({ratings.length})</h2>
        {ratings.map((r) => (
          <div key={r.id} className="card card-border p-3">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">
                {r.userName}
                {user && r.userId === user.userId && ' (vos)'}
              </span>
              <Stars value={r.score} size={14} />
            </div>
            {r.comment && <p className="mt-1 text-sm text-base-content/70">{r.comment}</p>}
          </div>
        ))}
        {ratings.length === 0 && (
          <p className="text-sm text-base-content/60">Sé el primero en dejar una reseña.</p>
        )}
      </div>
    </div>
  )
}
