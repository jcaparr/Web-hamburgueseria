import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconHeart, IconPin } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { AvatarDeUsuario } from '../components/AvatarDeUsuario'
import { FotoDeResenia } from '../components/FotoDeResenia'
import { SelectorDeFoto } from '../components/SelectorDeFoto'
import { JointPhoto } from '../components/JointPhoto'
import { ScoreBadge } from '../components/ScoreBadge'
import { Stars } from '../components/Stars'
import { useAuth } from '../context/AuthContext'
import type { BurgerJoint, PageResponse, Rating } from '../types'
import { isNotFound, isSessionExpired } from '../utils/errors'
import { shortAddress } from '../utils/address'
import { mapsUrl } from '../utils/maps'
import { relativeDate } from '../utils/relativeDate'

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

  async function submitRating(e: FormEvent) {
    e.preventDefault()
    if (!user) return navigate('/login')
    if (score === 0) {
      setError('Elegí una calificación de 1 a 5 estrellas')
      return
    }
    // El servidor lo exige igual; avisarlo acá evita mandar el formulario entero para
    // que vuelva rechazado por algo que ya se sabía antes de salir.
    if (!foto && !myRating?.photoUrl) {
      setError('Toda reseña lleva una foto de lo que comiste')
      return
    }

    setSubmitting(true)
    setError(null)
    try {
      // Texto y foto en el mismo pedido: si la foto no sirve, no se guarda una reseña
      // a medias esperando que alguien vuelva a completarla.
      const cuerpo = new FormData()
      cuerpo.append('score', String(score))
      cuerpo.append('comment', comment)
      if (foto) cuerpo.append('foto', foto)

      if (myRating) {
        await apiClient.put(`/burger-joints/${id}/ratings`, cuerpo)
      } else {
        await apiClient.post(`/burger-joints/${id}/ratings`, cuerpo)
      }
      setFoto(null)

      if (!myRating) {
        setScore(0)
        setComment('')
      }
      load()
    } catch (err: any) {
      if (isSessionExpired(err)) {
        navigate('/login')
      } else {
        // El servidor sabe por qué no sirvió esa foto —que no es una imagen, que está
        // dañada, que es enorme— y decirlo es lo único que le permite arreglarlo.
        setError(err.response?.data?.error ?? 'No pudimos guardar tu reseña')
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

        {/* Las tres partes de reseñar, cada una con su lugar: la nota, la foto y lo
            que se quiera contar. Separadas se entiende de un vistazo qué falta. */}
        <form onSubmit={submitRating} className="flex flex-col gap-4 rounded-box bg-base-100 p-4 ring-1 ring-inset ring-base-content/10">
          <span className="font-display text-sm font-bold">
            {myRating ? 'Editá tu opinión' : 'Dejá tu opinión'}
          </span>

          <div className="flex flex-col gap-1.5">
            <span className="text-xs font-semibold uppercase tracking-wide text-base-content/50">
              Tu puntaje
            </span>
            <Stars value={score} onChange={setScore} size={30} />
          </div>

          <div className="flex flex-col gap-1.5">
            <span className="text-xs font-semibold uppercase tracking-wide text-base-content/50">
              La foto
            </span>
            <SelectorDeFoto
              elegida={foto}
              yaSubida={myRating?.photoUrl ?? null}
              onElegir={setFoto}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <span className="text-xs font-semibold uppercase tracking-wide text-base-content/50">
              Tu reseña
            </span>
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="¿Qué te pareció?"
              maxLength={1000}
              className="textarea textarea-bordered min-h-24 focus:border-primary"
            />
          </div>

          {error && <p className="text-xs text-error">{error}</p>}
          <button type="submit" disabled={submitting} className="btn btn-primary">
            {submitting ? 'Guardando...' : myRating ? 'Actualizar reseña' : 'Publicar reseña'}
          </button>
        </form>
      </div>

      <div className="flex flex-col gap-3">
        <h2 className="font-display text-sm font-bold">Reseñas ({ratings.length})</h2>
        {/* Las mismas partes y el mismo orden que en el feed, para que una reseña se lea
            igual acá que allá. Falta el lugar, que sería repetir el título de esta
            pantalla en cada tarjeta. */}
        {ratings.map((r) => (
          <article
            key={r.id}
            className="overflow-hidden rounded-box bg-base-100 ring-1 ring-inset ring-base-content/10"
          >
            <header className="flex items-center gap-3 px-4 py-3">
              {/* Desde acá se llega a su perfil, que es de donde sale la gente a
                  seguir: alguien que reseñó lo mismo que vos es mejor candidato que
                  cualquiera que encuentres buscando a ciegas. */}
              <Link to={`/u/${r.username}`} className="flex min-w-0 items-center gap-3">
                <AvatarDeUsuario username={r.username} size={36} />
                <div className="flex min-w-0 flex-col">
                  <span className="truncate text-sm font-semibold hover:text-primary">
                    @{r.username}
                    {user && r.userId === user.userId && (
                      <span className="font-normal text-base-content/50"> · vos</span>
                    )}
                  </span>
                  <span className="text-xs text-base-content/50">
                    {relativeDate(r.createdAt)}
                  </span>
                </div>
              </Link>
              <div className="ml-auto flex-none">
                <Stars value={r.score} size={15} />
              </div>
            </header>

            {r.photoUrl && <FotoDeResenia src={r.photoUrl} autorUsername={r.username} />}

            {r.comment && (
              <p className="whitespace-pre-line px-4 py-3 text-sm leading-relaxed text-base-content/80">
                {r.comment}
              </p>
            )}
          </article>
        ))}
        {ratings.length === 0 && (
          <p className="text-sm text-base-content/60">Sé el primero en dejar una reseña.</p>
        )}
      </div>
    </div>
  )
}
