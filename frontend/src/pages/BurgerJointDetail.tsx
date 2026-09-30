import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconHeart, IconPencil, IconPin } from '../components/icons'
import { DistribucionDeNotas } from '../components/DistribucionDeNotas'
import { LoadError } from '../components/LoadError'
import { ModalDeResenia } from '../components/ModalDeResenia'
import { JointPhoto } from '../components/JointPhoto'
import { ScoreBadge } from '../components/ScoreBadge'
import { Stars } from '../components/Stars'
import { TarjetaDeResenia } from '../components/TarjetaDeResenia'
import { useAuth } from '../context/AuthContext'
import type { BurgerJoint, PageResponse, Rating, ResumenDeResenias } from '../types'
import { isNotFound, isSessionExpired } from '../utils/errors'
import { shortAddress } from '../utils/address'
import { mapsUrl } from '../utils/maps'

/**
 * Se llega con esto puesto desde "Mis reseñas", para editar sin tener que buscar el
 * formulario en la página. Se limpia en cuanto se usa: si quedara en la URL, volver
 * atrás abriría la ventana de nuevo.
 */
const ABRIR_OPINION = 'opinar'

export function BurgerJointDetail() {
  const { id } = useParams()
  const { user, loading: cargandoSesion } = useAuth()
  const navigate = useNavigate()
  const [parametros, setParametros] = useSearchParams()
  const [burgerJoint, setBurgerJoint] = useState<BurgerJoint | null>(null)
  const [ratings, setRatings] = useState<Rating[]>([])
  // Cuántas hay en total, que no es lo mismo que cuántas se trajeron: la lista viene
  // paginada. Sale de la página y no del resumen para que el número del título cuente
  // lo que se puede ver, y no las que el servidor escondió por un bloqueo.
  const [cuantasResenias, setCuantasResenias] = useState<number | null>(null)
  const [resumen, setResumen] = useState<ResumenDeResenias | null>(null)
  const [opinando, setOpinando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<unknown>(null)

  const myRating = ratings.find((r) => r.userId === user?.userId) ?? null

  function load() {
    // Juntas: si cualquiera de las dos falla, la página no está completa. Antes cada
    // una iba por su lado y sin manejo de error, así que un local inexistente o un
    // servidor caído dejaban "Cargando..." en pantalla para siempre.
    Promise.all([
      apiClient.get<BurgerJoint>(`/burger-joints/${id}`),
      apiClient.get<PageResponse<Rating>>(`/burger-joints/${id}/ratings`),
      apiClient.get<ResumenDeResenias>(`/burger-joints/${id}/ratings/resumen`),
    ])
      .then(([jointRes, ratingsRes, resumenRes]) => {
        setBurgerJoint(jointRes.data)
        setRatings(ratingsRes.data.content)
        setCuantasResenias(ratingsRes.data.totalElements)
        setResumen(resumenRes.data)
        setLoadError(null)
      })
      .catch(setLoadError)
  }

  useEffect(load, [id])

  // Se espera a saber si hay sesión antes de tocar el parámetro. Al entrar todavía no se
  // sabe quién sos, y consumirlo en ese momento era gastarlo sin abrir nada: cuando la
  // sesión llegaba, el parámetro ya no estaba.
  useEffect(() => {
    if (cargandoSesion || !parametros.has(ABRIR_OPINION)) return
    // Sin sesión no se abre: primero hay que entrar, y para eso está el botón.
    if (user) setOpinando(true)

    const limpios = new URLSearchParams(parametros)
    limpios.delete(ABRIR_OPINION)
    setParametros(limpios, { replace: true })
  }, [cargandoSesion, parametros, user, setParametros])

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

  function abrirOpinion() {
    if (!user) return navigate('/login')
    setOpinando(true)
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
    // La ficha arriba a todo el ancho y las reseñas abajo, también a todo el ancho.
    // Antes eran dos columnas con el formulario en la izquierda; sacado el formulario,
    // esa columna quedaba un muñón al lado de una lista larga.
    <div className="flex flex-col gap-6 p-4 md:mx-auto md:max-w-3xl md:p-0 md:pt-6">
      {loadError !== null && <LoadError error={loadError} onRetry={load} />}

      <section className="flex flex-col gap-4 md:flex-row md:gap-6">
        <JointPhoto
          src={burgerJoint.photoUrl}
          name={burgerJoint.name}
          className="h-48 w-full rounded-box object-cover md:h-60 md:w-80 md:flex-none"
        />

        <div className="flex flex-1 flex-col gap-3">
          <div className="flex items-start justify-between gap-2">
            <div className="flex min-w-0 flex-col gap-1.5">
              <h1 className="font-display text-2xl font-bold">{burgerJoint.name}</h1>
              <p className="text-sm text-base-content/60">
                {shortAddress(burgerJoint.address, burgerJoint.area)}
              </p>
              {burgerJoint.averageScore ? (
                <ScoreBadge score={burgerJoint.averageScore} size="sm" />
              ) : (
                <p className="text-sm text-base-content/50">Todavía sin calificaciones</p>
              )}
              <a
                href={mapsUrl(
                  burgerJoint.placeId,
                  burgerJoint.name,
                  burgerJoint.latitude,
                  burgerJoint.longitude
                )}
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
              className={`btn btn-ghost btn-circle flex-none ${burgerJoint.inWishlist ? 'text-primary' : 'text-base-content/40'}`}
              title={burgerJoint.inWishlist ? 'Quitar de deseados' : 'Guardar en deseados'}
            >
              <IconHeart size={24} filled={burgerJoint.inWishlist} />
            </button>
          </div>

          {/* Lo que el promedio no dice: si las opiniones coinciden o se reparten. */}
          {resumen && <DistribucionDeNotas distribucion={resumen.distribucion} />}

          {error && <p className="text-xs text-error">{error}</p>}

          {/* La acción principal de la pantalla. Si ya reseñaste, el botón no reemplaza
              esa información sino que la acompaña: sin las estrellas al lado, un local
              que ya calificaste se vería igual que uno donde nunca fuiste. */}
          {myRating ? (
            <div className="mt-auto flex items-center gap-3 rounded-box bg-base-100 px-4 py-3 ring-1 ring-inset ring-base-content/15">
              <div className="flex min-w-0 flex-col gap-1">
                <span className="text-xs font-semibold uppercase tracking-wide text-base-content/50">
                  Tu puntaje
                </span>
                <Stars value={myRating.score} size={18} />
              </div>
              <button
                type="button"
                onClick={abrirOpinion}
                className="btn btn-outline btn-sm ml-auto flex-none gap-1.5"
              >
                <IconPencil size={15} />
                Editar
              </button>
            </div>
          ) : (
            <button type="button" onClick={abrirOpinion} className="btn btn-primary mt-auto">
              Dejá tu opinión
            </button>
          )}
        </div>
      </section>

      {/* Asomadas arriba de la lista: de veinte reseñas, la de alguien que te importa
          puede caer en la página tres, y ahí no la ve nadie. Siguen estando abajo con
          todas las demás, esto es un atajo y no otra lista. */}
      {resumen && resumen.deQuienesSigo.length > 0 && (
        <section className="flex flex-col gap-3">
          <h2 className="font-display text-sm font-bold">Lo que dijeron los que seguís</h2>
          {resumen.deQuienesSigo.map((r) => (
            <TarjetaDeResenia key={r.id} resenia={r} esMia={r.userId === user?.userId} />
          ))}
        </section>
      )}

      <section className="flex flex-col gap-3">
        <h2 className="font-display text-sm font-bold">
          {cuantasResenias !== null ? `Reseñas (${cuantasResenias})` : 'Reseñas'}
        </h2>
        {ratings.map((r) => {
          const esMia = r.userId === user?.userId
          return (
            <TarjetaDeResenia
              key={r.id}
              resenia={r}
              esMia={esMia}
              onEditar={esMia ? abrirOpinion : undefined}
            />
          )
        })}
        {ratings.length === 0 && (
          <p className="text-sm text-base-content/60">Sé el primero en dejar una reseña.</p>
        )}
      </section>

      <ModalDeResenia
        abierto={opinando}
        localId={burgerJoint.id}
        nombreDelLocal={burgerJoint.name}
        miResenia={myRating}
        onCerrar={() => setOpinando(false)}
        onGuardada={load}
        onSesionVencida={() => navigate('/login')}
      />
    </div>
  )
}
