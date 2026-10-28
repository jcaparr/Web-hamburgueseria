import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
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
import { RESENIA_GUARDADA } from '../context/resenia'
import { useAuth } from '../context/useAuth'
import { useDatosEstructurados, useDescripcion, useNoIndexar } from '../hooks/useMetadatos'
import { useTitulo } from '../hooks/useTitulo'
import type { BurgerJoint, Horario, PageResponse, Rating, ResumenDeResenias } from '../types'
import { anclaDeResenia, reseniaDelAncla } from '../utils/aLaResenia'
import { descripcionDe, restauranteDe } from '../utils/datosEstructurados'
import { isNotFound, isSessionExpired } from '../utils/errors'
import { shortAddress } from '../utils/address'
import { comoLlegarUrl, mapsUrl } from '../utils/maps'

/**
 * Se llega con esto puesto desde "Mis reseñas", para editar sin tener que buscar el
 * formulario en la página. La ventana se abre mientras está, y al cerrarla se saca: si
 * quedara en la URL, volver atrás la abriría de nuevo.
 */
const ABRIR_OPINION = 'opinar'

/** Cómo se marca la reseña a la que se llegó por el ancla (#260), mientras se la busca con la vista. */
const RESALTADA = ['outline-2', 'outline-offset-4', 'outline-primary']

/**
 * @param idDelLocal el id cuando la ficha se abre por su dirección linda (/<slug>), que
 *   ya lo resolvió; sin él, sale de /burger-joints/:id.
 */
export function BurgerJointDetail({ idDelLocal }: { idDelLocal?: string }) {
  const params = useParams()
  const id = idDelLocal ?? params.id
  const llegoPorId = idDelLocal === undefined
  const { user } = useAuth()
  const navigate = useNavigate()
  const [parametros, setParametros] = useSearchParams()
  const [burgerJoint, setBurgerJoint] = useState<BurgerJoint | null>(null)
  const [ratings, setRatings] = useState<Rating[]>([])
  // Tu reseña de este local, pedida aparte (#340): la lista trae solo las primeras veinte
  // y la tuya puede ser más vieja. null = no dejaste; undefined = no se pudo saber.
  const [mia, setMia] = useState<Rating | null | undefined>(undefined)
  // Cuántas hay en total, que no es lo mismo que cuántas se trajeron: la lista viene
  // paginada. Sale de la página y no del resumen para que el número del título cuente
  // lo que se puede ver, y no las que el servidor escondió por un bloqueo.
  const [cuantasResenias, setCuantasResenias] = useState<number | null>(null)
  const [resumen, setResumen] = useState<ResumenDeResenias | null>(null)
  const [horario, setHorario] = useState<Horario | null>(null)
  const [opinando, setOpinando] = useState(false)
  // Cuántas veces se abrió la ventana de reseñar: es su key, para que cada vez se arme
  // de cero con lo que ya tenía la reseña.
  const [aperturas, setAperturas] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<unknown>(null)
  const location = useLocation()
  const objetivo = reseniaDelAncla(location.hash)
  // La reseña del ancla cuando no vino entre las primeras veinte (#260), con el local
  // al que pertenece.
  const [buscada, setBuscada] = useState<{ local: string | undefined; resenia: Rating } | null>(null)

  useTitulo(burgerJoint?.name ?? (isNotFound(loadError) ? 'Hamburguesería no encontrada' : null))
  // Para los buscadores: la descripción y la ficha en schema.org. Un local que no existe
  // no se tiene que guardar, aunque el servidor haya contestado 200 con la app.
  useDescripcion(burgerJoint ? descripcionDe(burgerJoint) : null)
  useDatosEstructurados(burgerJoint ? restauranteDe(burgerJoint, horario, window.location.origin) : null)
  useNoIndexar(isNotFound(loadError))

  // Un enlace viejo (con el id) pasa a la dirección linda, con lo que traía (#305).
  // replace: "atrás" no vuelve al enlace viejo ni queda en un bucle.
  useEffect(() => {
    if (llegoPorId && burgerJoint?.slug) {
      navigate(`/${burgerJoint.slug}${location.search}${location.hash}`, { replace: true })
    }
  }, [llegoPorId, burgerJoint, navigate, location.search, location.hash])

  // La reseña escrita desde el lápiz mientras se mira esta misma ficha: navegar a la misma
  // dirección no la recarga, así que se escucha el aviso.
  useEffect(() => {
    window.addEventListener(RESENIA_GUARDADA, load)
    return () => window.removeEventListener(RESENIA_GUARDADA, load)
  })

  // Qué carga es la última: al abrir la app `user` llega después y se pide dos veces; si la
  // anónima contestara última, pisaría tu reseña con "no dejaste ninguna".
  const cargaVigente = useRef(0)

  function load() {
    const esta = ++cargaVigente.current
    // Juntas: si cualquiera de las dos falla, la página no está completa. Antes cada
    // una iba por su lado y sin manejo de error, así que un local inexistente o un
    // servidor caído dejaban "Cargando..." en pantalla para siempre.
    Promise.all([
      apiClient.get<BurgerJoint>(`/burger-joints/${id}`),
      apiClient.get<PageResponse<Rating>>(`/burger-joints/${id}/ratings`),
      apiClient.get<ResumenDeResenias>(`/burger-joints/${id}/ratings/resumen`),
      // Sin sesión no hay reseña propia. Si falla no tumba la ficha: se cae a buscarla
      // en la lista, como antes.
      user
        ? apiClient
            .get<Rating>(`/burger-joints/${id}/ratings/mine`)
            .then(({ data, status }) => (status === 200 && data ? data : null))
            .catch(() => undefined)
        : Promise.resolve(null),
    ])
      .then(([jointRes, ratingsRes, resumenRes, miaRes]) => {
        if (cargaVigente.current !== esta) return
        setMia(miaRes)
        setBurgerJoint(jointRes.data)
        setRatings(ratingsRes.data.content)
        setCuantasResenias(ratingsRes.data.page.totalElements)
        setResumen(resumenRes.data)
        setLoadError(null)

        // La reseña que pide el ancla, si no está entre las que trajo la lista (#260):
        // la ficha trae las primeras veinte, y una más vieja hay que pedirla aparte. Se
        // vuelve a pedir con cada carga, así que después de editarla se ve lo nuevo. Si
        // no se puede ver (la borraron, hay un bloqueo), la ficha queda arriba, como antes.
        if (objetivo === null || ratingsRes.data.content.some((r) => r.id === objetivo)) {
          setBuscada(null)
          return
        }
        const local = id
        apiClient
          .get<Rating>(`/burger-joints/${local}/ratings/${objetivo}`)
          .then(({ data }) => setBuscada({ local, resenia: data }))
          .catch(() => setBuscada(null))
      })
      .catch((err) => {
        if (cargaVigente.current === esta) setLoadError(err)
      })
  }

  // Con el ancla también: otra reseña del mismo local hay que buscarla (#260).
  useEffect(() => {
    // Otro local u otra persona: la reseña de antes no vale mientras llega la nueva.
    setMia(undefined)
    load()
  }, [id, objetivo, user?.userId])

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

  // Primero la buscada y después las demás, sin repetirla. Solo si es de este local:
  // la de la ficha anterior no se cuela al pasar a otra.
  const estaEnLaLista = objetivo !== null && ratings.some((r) => r.id === objetivo)
  const lista =
    buscada && buscada.local === id && buscada.resenia.id === objetivo && !estaEnLaLista
      ? [buscada.resenia, ...ratings]
      : ratings
  // La que dice el servidor (#340). Solo si no se pudo saber se busca en la lista, que
  // tiene las primeras veinte y la buscada.
  const myRating = mia !== undefined ? mia : (lista.find((r) => r.userId === user?.userId) ?? null)

  // Salta hasta la reseña, le pasa el foco y la marca un momento, una sola vez por
  // ancla: después de guardar una reseña la lista se vuelve a armar, y no tiene que
  // volver a saltar.
  //
  // De una, como cualquier ancla, y no deslizándose: con las fotos y el horario todavía
  // llegando, el deslizamiento a veces ni arrancaba y la página quedaba arriba. El foco
  // es para el lector de pantalla, que sin él no se entera de a dónde llegó, y para que
  // el próximo Tab siga desde ahí.
  const yaSeMostro = useRef<number | null>(null)
  const sacarMarca = useRef<ReturnType<typeof setTimeout>>(undefined)
  useEffect(() => {
    if (objetivo === null || yaSeMostro.current === objetivo) return
    const elemento = document.getElementById(anclaDeResenia(objetivo))
    if (!elemento) return
    yaSeMostro.current = objetivo
    elemento.scrollIntoView({ block: 'start' })
    elemento.focus({ preventScroll: true })
    elemento.classList.add(...RESALTADA)
    sacarMarca.current = setTimeout(() => elemento.classList.remove(...RESALTADA), 2500)
  }, [objetivo, ratings, buscada])
  useEffect(() => () => clearTimeout(sacarMarca.current), [])

  // Abierta por el enlace mientras el parámetro esté y haya sesión. Se deduce en vez de
  // prenderla desde un efecto: un dibujo menos (#109), y no hace falta esperar a saber
  // quién sos para no gastar el parámetro antes de tiempo. Sin sesión no se abre:
  // primero hay que entrar, y para eso está el botón.
  const abiertaPorEnlace = parametros.has(ABRIR_OPINION) && user !== null

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
    setAperturas((n) => n + 1)
    setOpinando(true)
  }

  /**
   * Vuelve a pedir la ficha y, si la reseña es nueva, baja hasta ella: es lo que promete
   * el "Ver mi reseña" de la pantalla de gracias (#365).
   */
  function alGuardar(_localId: number | string, ratingId?: number) {
    load()
    if (ratingId) {
      navigate({ pathname: location.pathname, search: location.search, hash: `#${anclaDeResenia(ratingId)}` })
    }
  }

  function cerrarOpinion() {
    setOpinando(false)
    if (parametros.has(ABRIR_OPINION)) {
      const limpios = new URLSearchParams(parametros)
      limpios.delete(ABRIR_OPINION)
      setParametros(limpios, { replace: true })
    }
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
          <h1 className="font-display text-2xl font-extrabold leading-tight text-balance md:text-3xl">
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
      <section aria-label="Horario y dirección" className="flex flex-col divide-y divide-base-content/10 tarjeta">
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

      {/* Sin reseñas, el bloque de calificaciones ya invita a escribir la primera: un
          "Reseñas (0)" abajo repetiría lo mismo. */}
      {lista.length > 0 && (
        <section id="resenias" className="flex scroll-mt-20 flex-col gap-3">
          <h2 className="font-display text-lg font-bold">
            {cuantasResenias !== null ? `Reseñas (${cuantasResenias})` : 'Reseñas'}
          </h2>
          {lista.map((r) => {
            const esMia = r.userId === user?.userId
            return (
              <div key={r.id} id={anclaDeResenia(r.id)} tabIndex={-1} className="scroll-mt-24 rounded-box">
                <TarjetaDeResenia
                  resenia={r}
                  esMia={esMia}
                  puedeReaccionar={user !== null && !esMia}
                  onEditar={esMia ? abrirOpinion : undefined}
                  local={{ nombre: burgerJoint.name, barrio: burgerJoint.area }}
                />
              </div>
            )
          })}
        </section>
      )}

      <MasEnElBarrio barrio={burgerJoint.area} sinEste={burgerJoint.id} />

      {/* La key cambia al abrirla y cuando cambia la reseña propia —que llega con la
          sesión, o al publicar la primera—: así siempre arranca con lo que corresponde. */}
      <ModalDeResenia
        key={`${aperturas}-${myRating?.id ?? 'nueva'}`}
        abierto={opinando || abiertaPorEnlace}
        localId={burgerJoint.id}
        nombreDelLocal={burgerJoint.name}
        miResenia={myRating}
        onCerrar={cerrarOpinion}
        onGuardada={alGuardar}
        onSesionVencida={() => navigate('/login')}
      />
    </div>
  )
}
