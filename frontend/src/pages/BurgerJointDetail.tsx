import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconHeart, IconPin } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { SelectorDeFoto } from '../components/SelectorDeFoto'
import { JointPhoto } from '../components/JointPhoto'
import { ScoreBadge } from '../components/ScoreBadge'
import { Stars } from '../components/Stars'
import { useAuth } from '../context/AuthContext'
import type { BurgerJoint, PageResponse, Rating } from '../types'
import { isNotFound, isSessionExpired } from '../utils/errors'
import { shortAddress } from '../utils/address'
import { mapsUrl } from '../utils/maps'

export function BurgerJointDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [burgerJoint, setBurgerJoint] = useState<BurgerJoint | null>(null)
  const [ratings, setRatings] = useState<Rating[]>([])
  const [score, setScore] = useState(0)
  const [comment, setComment] = useState('')
  const [foto, setFoto] = useState<File | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<unknown>(null)

  const myRating = user ? ratings.find((r) => r.userId === user.userId) ?? null : null

  function load() {
    // Juntas: si cualquiera de las dos falla, la página no está completa. Antes cada
    // una iba por su lado y sin manejo de error, así que un local inexistente o un
    // servidor caído dejaban "Cargando..." en pantalla para siempre.
    Promise.all([
      apiClient.get<BurgerJoint>(`/burger-joints/${id}`),
      apiClient.get<PageResponse<Rating>>(`/burger-joints/${id}/ratings`),
    ])
      .then(([jointRes, ratingsRes]) => {
        setBurgerJoint(jointRes.data)
        setRatings(ratingsRes.data.content)
        setLoadError(null)
      })
      .catch(setLoadError)
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

  async function quitarFoto() {
    try {
      await apiClient.delete(`/burger-joints/${id}/ratings/foto`)
      setFoto(null)
      load()
    } catch {
      setError('No pudimos quitar la foto')
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
      }

      // La foto va después y en su propia llamada, así un problema con ella —que pese,
      // que tarde, que no se pueda leer— no se lleva puesto lo que ya escribió. Si
      // falla, la reseña quedó guardada y se avisa solo de la foto.
      if (foto) {
        const cuerpo = new FormData()
        cuerpo.append('foto', foto)
        try {
          await apiClient.put(`/burger-joints/${id}/ratings/foto`, cuerpo)
          setFoto(null)
        } catch (err: any) {
          setError(err.response?.data?.error ?? 'Guardamos tu reseña, pero no la foto')
        }
      }

      if (!myRating) {
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

  if (!burgerJoint) {
    if (isNotFound(loadError)) {
      return (
        <div className="flex flex-col items-start gap-3 p-4">
          <p className="text-sm text-base-content/70">No encontramos esta hamburguesería.</p>
          <Link to="/" className="btn btn-sm btn-outline">
            Ver todas
          </Link>
        </div>
      )
    }
    if (loadError) {
      return (
        <div className="p-4">
          <LoadError error={loadError} onRetry={load} />
        </div>
      )
    }
    return <p className="p-4 text-sm text-base-content/60">Cargando...</p>
  }

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-4xl md:grid md:grid-cols-2 md:gap-6 md:p-0">
      <div className="flex flex-col gap-4">
        {loadError !== null && <LoadError error={loadError} onRetry={load} />}
        <JointPhoto
          src={burgerJoint.photoUrl}
          name={burgerJoint.name}
          className="h-48 w-full rounded-box object-cover"
        />

        <div className="flex items-start justify-between">
          <div className="flex flex-col gap-1.5">
            <h1 className="font-display text-2xl font-bold">{burgerJoint.name}</h1>
            <p className="text-sm text-base-content/60">{shortAddress(burgerJoint.address, burgerJoint.area)}</p>
            {burgerJoint.averageScore ? (
              <ScoreBadge score={burgerJoint.averageScore} size="sm" />
            ) : (
              <p className="text-sm text-base-content/50">Todavía sin calificaciones</p>
            )}
            <a
              href={mapsUrl(burgerJoint.placeId, burgerJoint.name, burgerJoint.latitude, burgerJoint.longitude)}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1 text-xs font-semibold text-primary"
            >
              <IconPin />
              Ver en Maps
            </a>
          </div>

          <button
            onClick={toggleWishlist}
            aria-pressed={burgerJoint.inWishlist}
            className={`btn btn-ghost btn-circle ${burgerJoint.inWishlist ? 'text-primary' : 'text-base-content/40'}`}
            title={burgerJoint.inWishlist ? 'Quitar de deseados' : 'Guardar en deseados'}
          >
            <IconHeart size={24} filled={burgerJoint.inWishlist} />
          </button>
        </div>

        <form onSubmit={submitRating} className="rounded-box bg-base-100 ring-1 ring-inset ring-base-content/15 flex flex-col gap-2 p-4">
          <span className="font-display text-sm font-bold">{myRating ? 'Editá tu opinión' : 'Dejá tu opinión'}</span>
          <Stars value={score} onChange={setScore} size={28} />
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="¿Qué te pareció?"
            maxLength={1000}
            className="textarea textarea-bordered min-h-20 focus:border-primary"
          />
          <SelectorDeFoto
            elegida={foto}
            yaSubida={myRating?.photoUrl ?? null}
            onElegir={setFoto}
            onQuitar={quitarFoto}
          />
          {error && <p className="text-xs text-error">{error}</p>}
          <button type="submit" disabled={submitting} className="btn btn-primary">
            {submitting ? 'Guardando...' : myRating ? 'Actualizar reseña' : 'Publicar reseña'}
          </button>
        </form>
      </div>

      <div className="flex flex-col gap-3">
        <h2 className="font-display text-sm font-bold">Reseñas ({ratings.length})</h2>
        {ratings.map((r) => (
          <div key={r.id} className="rounded-box bg-base-100 ring-1 ring-inset ring-base-content/15 p-3">
            <div className="flex items-center justify-between">
              {/* Desde acá se llega a su perfil, que es de donde sale la gente a
                  seguir: alguien que reseñó lo mismo que vos es mejor candidato que
                  cualquiera que encuentres buscando a ciegas. */}
              <Link to={`/u/${r.username}`} className="text-sm font-medium hover:text-primary">
                @{r.username}
                {user && r.userId === user.userId && ' (vos)'}
              </Link>
              <Stars value={r.score} size={14} />
            </div>
            {r.comment && <p className="mt-1 text-sm text-base-content/70">{r.comment}</p>}
            {r.photoUrl && (
              <img
                src={r.photoUrl}
                alt={`La hamburguesa que reseñó @${r.username}`}
                loading="lazy"
                className="mt-2 max-h-72 w-full rounded-lg object-cover"
              />
            )}
          </div>
        ))}
        {ratings.length === 0 && (
          <p className="text-sm text-base-content/60">Sé el primero en dejar una reseña.</p>
        )}
      </div>
    </div>
  )
}
