import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconClock, IconExternal, IconPin, IconRoute } from '../components/icons'
import { HorarioDelLocal } from '../components/HorarioDelLocal'
import { LoadError } from '../components/LoadError'
import { MasEnElBarrio } from '../components/MasEnElBarrio'
import { ModalDeResenia } from '../components/ModalDeResenia'
import { PortadaDelLocal } from '../components/PortadaDelLocal'
import { ResumenDeCalificaciones } from '../components/ResumenDeCalificaciones'
import { ScoreBadge } from '../components/ScoreBadge'
import { TarjetaDeResenia } from '../components/TarjetaDeResenia'
import { useAuth } from '../context/AuthContext'
import { useTitulo } from '../hooks/useTitulo'
import type { BurgerJoint, Horario, PageResponse, Rating, ResumenDeResenias } from '../types'
import { isNotFound, isSessionExpired } from '../utils/errors'
import { shortAddress } from '../utils/address'
import { comoLlegarUrl, mapsUrl } from '../utils/maps'

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
  const [horario, setHorario] = useState<Horario | null>(null)
  const [opinando, setOpinando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<unknown>(null)

  const myRating = ratings.find((r) => r.userId === user?.userId) ?? null
  useTitulo(burgerJoint?.name ?? (isNotFound(loadError) ? 'Hamburguesería no encontrada' : null))

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
        setCuantasResenias(ratingsRes.data.page.totalElements)
        setResumen(resumenRes.data)
        setLoadError(null)
      })
      .catch(setLoadError)
  }

  useEffect(load, [id])

  // Cada ficha empieza arriba. Al pasar de un local a otro desde "Más hamburgueserías
  // en…", que está al final, la página nueva aparecía scrolleada hasta abajo.
  useEffect(() => {
    window.scrollTo({ top: 0 })
  }, [id])

  // Aparte de las demás y sin cartel de error: el horario acompaña a la ficha, no la
  // completa. Si no llega, el local se ve igual que uno del que Google no tiene horario.
  // Y no se vuelve a pedir al guardar una reseña o un deseado, que no lo cambian.
  useEffect(() => {
    apiClient
      .get<Horario>(`/burger-joints/${id}/horario`)
      .then(({ data }) => setHorario(data))
      .catch(() => setHorario(null))
  }, [id])

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
        setError('No pudimos actualizar tus guardadas. Probá de nuevo.')
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
          <p className="text-sm text-base-content/70">
            No encontramos esta hamburguesería. Puede que el enlace esté mal o que ya no
            esté en la app.
          </p>
          <Link to="/" className="btn btn-sm btn-outline">
            Ver todas las hamburgueserías
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
    return (
      <p role="status" className="p-4 text-sm text-base-content/70">
        Cargando…
      </p>
    )
  }

  const comoLlegar = comoLlegarUrl(burgerJoint)

  return (
    <div className="flex flex-col gap-6 p-4 md:mx-auto md:max-w-3xl md:p-0 md:pt-2">
      {loadError !== null && <LoadError error={loadError} onRetry={load} />}

      <div className="flex flex-col">
        <PortadaDelLocal local={burgerJoint} onGuardar={toggleWishlist} />

        {/* Nombre, nota y barrio: lo que se lee primero para saber dónde se está. */}
        <header className="flex flex-col gap-2 pt-4">
          <h1 className="font-display text-3xl font-bold leading-tight text-balance">
            {burgerJoint.name}
          </h1>
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-base-content/70">
            {burgerJoint.averageScore ? (
              <>
                <ScoreBadge score={burgerJoint.averageScore} size="sm" />
                <a href="#resenias" className="-my-3 rounded-field py-3 hover:text-primary hover:underline">
                  {burgerJoint.ratingsCount === 1 ? '1 reseña' : `${burgerJoint.ratingsCount} reseñas`}
                </a>
              </>
            ) : (
              <span>Sin calificaciones todavía</span>
            )}
            {burgerJoint.area && <span className="font-medium">{burgerJoint.area}</span>}
          </div>
          {error && <p role="alert" className="text-sm text-error">{error}</p>}
        </header>
      </div>

      {/* Las dos cosas que se vienen a hacer a una ficha: ir, y contar cómo estuvo.
          Reseñar es la acción principal de la app, así que va en rojo; ir, al lado y
          del mismo tamaño, porque en el teléfono es la que más se usa. */}
      <div className="-mt-2 grid grid-cols-2 gap-3 md:flex">
        {comoLlegar && (
          <a
            href={comoLlegar}
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-outline gap-2 whitespace-nowrap md:px-6"
          >
            <IconRoute size={18} />
            Cómo llegar
            <span className="sr-only"> (se abre en otra pestaña)</span>
          </a>
        )}
        <button
          type="button"
          onClick={abrirOpinion}
          className={`btn btn-primary whitespace-nowrap md:px-6 ${comoLlegar ? '' : 'col-span-2'}`}
        >
          {myRating ? 'Editar tu reseña' : 'Escribir reseña'}
        </button>
      </div>

      {/* Si está abierto y dónde queda, juntos: es lo que se mira antes de salir. */}
      <section aria-label="Horario y dirección" className="flex flex-col divide-y divide-base-content/10 rounded-box bg-base-100 ring-1 ring-inset ring-base-content/15">
        {horario && horario.franjas.length > 0 && (
          <div className="flex items-start gap-3 px-4 py-3">
            <IconClock size={18} className="mt-0.5 flex-none text-base-content/70" />
            <div className="min-w-0 flex-1">
              <HorarioDelLocal franjas={horario.franjas} />
            </div>
          </div>
        )}
        <a
          href={mapsUrl(burgerJoint.placeId, burgerJoint.name, burgerJoint.latitude, burgerJoint.longitude)}
          target="_blank"
          rel="noopener noreferrer"
          className="group flex items-start gap-3 px-4 py-3 text-sm"
        >
          <IconPin size={18} className="mt-0.5 flex-none text-base-content/70" />
          <span className="min-w-0 flex-1">
            <span className="block">{shortAddress(burgerJoint.address, burgerJoint.area)}</span>
            <span className="text-xs font-semibold text-primary group-hover:underline">
              Ver en Maps
              <span className="sr-only"> (se abre en otra pestaña)</span>
            </span>
          </span>
          <IconExternal size={16} className="mt-0.5 flex-none text-base-content/70" />
        </a>
      </section>

      <ResumenDeCalificaciones
        promedio={burgerJoint.averageScore}
        cuantas={burgerJoint.ratingsCount}
        resumen={resumen}
        miResenia={myRating}
      />

      {/* Asomadas arriba de la lista: de veinte reseñas, la de alguien que te importa
          puede caer en la página tres, y ahí no la ve nadie. Siguen estando abajo con
          todas las demás, esto es un atajo y no otra lista. */}
      {resumen && resumen.deQuienesSigo.length > 0 && (
        <section className="flex flex-col gap-3">
          <h2 className="font-display text-lg font-bold">Lo que dijeron los que seguís</h2>
          {resumen.deQuienesSigo.map((r) => (
            <TarjetaDeResenia key={r.id} resenia={r} esMia={r.userId === user?.userId} />
          ))}
        </section>
      )}

      {/* Sin reseñas, el bloque de calificaciones ya invita a escribir la primera: un
          "Reseñas (0)" abajo repetiría lo mismo. */}
      {ratings.length > 0 && (
        <section id="resenias" className="flex scroll-mt-20 flex-col gap-3">
          <h2 className="font-display text-lg font-bold">
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
        </section>
      )}

      <MasEnElBarrio barrio={burgerJoint.area} sinEste={burgerJoint.id} />

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
