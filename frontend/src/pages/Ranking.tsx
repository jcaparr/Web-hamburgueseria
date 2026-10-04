import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import { JointPhoto } from '../components/JointPhoto'
import { LoadError } from '../components/LoadError'
import { ScoreBadge } from '../components/ScoreBadge'
import { AvisoVacio } from '../components/Seccion'
import { useAuth } from '../context/AuthContext'
import type { PageResponse, RankingItem } from '../types'
import { shortAddress } from '../utils/address'

type Tab = 'general' | 'mine'
type Order = 'score' | 'popularity'

/** Lo que cuenta en cada puesto: la nota que lo puso ahí y una línea que la explica. */
interface Puesto {
  item: RankingItem
  posicion: number
  nota: number
  detalle: string
}

const BAJADAS: Record<Tab, Record<Order, string>> = {
  general: {
    score: 'Las que mejor puntuó la gente, de la primera para abajo.',
    popularity: 'Las que más gente calificó.',
  },
  mine: {
    score: 'Las hamburgueserías ordenadas por las notas que les pusiste vos.',
    popularity: 'Las hamburgueserías ordenadas por las notas que les pusiste vos.',
  },
}

export function Ranking() {
  const { user } = useAuth()
  const [tab, setTab] = useState<Tab>('general')
  const [order, setOrder] = useState<Order>('score')
  const [items, setItems] = useState<RankingItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    // Sin sesión "Mi ranking" no tiene qué mostrar. Antes se pedía igual: volvía 401
    // y en pantalla quedaba la lista del ranking general debajo del aviso de iniciar
    // sesión, como si fuera la tuya.
    if (tab === 'mine' && !user) {
      setItems([])
      setError(null)
      setLoading(false)
      return
    }

    // Evita que una respuesta lenta de la pestaña anterior pise a la actual.
    let cancelled = false
    setLoading(true)
    const request =
      tab === 'general'
        ? apiClient.get<PageResponse<RankingItem>>('/ranking/general', { params: { order } })
        : apiClient.get<PageResponse<RankingItem>>('/ranking/mine')

    request
      .then(({ data }) => {
        if (cancelled) return
        setItems(data.content)
        setError(null)
      })
      .catch((err) => {
        if (!cancelled) setError(err)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [tab, order, user, attempt])

  const puestos: Puesto[] = items.map((item, index) => ({
    item,
    posicion: index + 1,
    nota: tab === 'mine' ? (item.myScore ?? item.averageScore) : item.averageScore,
    detalle:
      tab === 'mine'
        ? `Promedio ${item.averageScore.toFixed(1)}`
        : item.ratingsCount === 1
          ? '1 reseña'
          : `${item.ratingsCount} reseñas`,
  }))
  const [primero, ...resto] = puestos
  const podio = resto.slice(0, 2)
  const siguientes = resto.slice(2)
  const sinSesion = tab === 'mine' && !user
  // La primera vez, o al cambiar de pestaña con la lista vacía: la forma del podio.
  // Al cambiar el orden con una lista ya puesta, queda la anterior hasta que llega la
  // nueva, que es menos salto que vaciarla.
  const cargandoDeCero = loading && items.length === 0

  return (
    <div className="flex flex-col gap-6 p-4 md:mx-auto md:max-w-2xl md:p-0">
      {/* Liviana, como la de Explorar: título, una línea que dice qué se está mirando
          y los controles juntos. La bajada cambia con la pestaña y el orden, porque
          "mejor calificadas" y "más populares" son listas que se leen distinto. */}
      <header className="flex flex-col gap-4 md:pt-2">
        <div className="flex flex-col gap-1">
          <h1 className="font-display text-3xl font-bold leading-tight md:text-4xl">Ranking</h1>
          <p className="text-sm text-base-content/70 md:text-base">{BAJADAS[tab][order]}</p>
        </div>

        <div role="tablist" className="flex w-full gap-1 rounded-full border border-base-300 bg-base-100 p-1">
          <BotonDePestania activa={tab === 'general'} onClick={() => setTab('general')}>
            Ranking general
          </BotonDePestania>
          <BotonDePestania activa={tab === 'mine'} onClick={() => setTab('mine')}>
            Mi ranking
          </BotonDePestania>
        </div>

        {tab === 'general' && (
          <div className="flex gap-2">
            <button
              type="button"
              aria-pressed={order === 'score'}
              onClick={() => setOrder('score')}
              className={`btn btn-sm rounded-full ${order === 'score' ? 'btn-primary' : 'btn-outline'}`}
            >
              Mejor calificadas
            </button>
            <button
              type="button"
              aria-pressed={order === 'popularity'}
              onClick={() => setOrder('popularity')}
              className={`btn btn-sm rounded-full ${order === 'popularity' ? 'btn-primary' : 'btn-outline'}`}
            >
              Más populares
            </button>
          </div>
        )}
      </header>

      {sinSesion ? (
        <AvisoVacio accion={{ texto: 'Iniciar sesión', a: '/login' }}>
          Iniciá sesión para ver tu ranking: se arma solo con las notas que vas poniendo.
        </AvisoVacio>
      ) : error ? (
        <LoadError error={error} onRetry={() => setAttempt((n) => n + 1)} />
      ) : cargandoDeCero ? (
        <PodioCargando />
      ) : !primero ? (
        tab === 'mine' ? (
          <AvisoVacio accion={{ texto: 'Explorar hamburgueserías', a: '/' }}>
            Todavía no calificaste ninguna hamburguesería. Cuando lo hagas, tu ranking se arma
            acá.
          </AvisoVacio>
        ) : (
          <AvisoVacio>Todavía nadie calificó ninguna hamburguesería.</AvisoVacio>
        )
      ) : (
        <section aria-label="Ranking" aria-busy={loading} className="flex flex-col gap-6">
          {/* El podio en grande, con foto: es lo que se viene a ver. Las tres primeras
              son las únicas que tienen foto grande; del cuarto puesto para abajo es una
              lista para recorrer, con la foto chica. */}
          <ol className="grid grid-cols-2 gap-3">
            <li className="col-span-2">
              <PuestoDelPodio puesto={primero} destacado />
            </li>
            {podio.map((puesto) => (
              <li key={puesto.item.burgerJointId}>
                <PuestoDelPodio puesto={puesto} />
              </li>
            ))}
          </ol>

          {siguientes.length > 0 && (
            <ol start={4} className="flex flex-col divide-y divide-base-content/10 overflow-hidden rounded-box bg-base-100 ring-1 ring-inset ring-base-content/15">
              {siguientes.map((puesto) => (
                <li key={puesto.item.burgerJointId}>
                  <FilaDelRanking puesto={puesto} />
                </li>
              ))}
            </ol>
          )}
        </section>
      )}
    </div>
  )
}

function BotonDePestania({
  activa,
  onClick,
  children,
}: {
  activa: boolean
  onClick: () => void
  children: string
}) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={activa}
      className={`flex-1 cursor-pointer rounded-full px-4 py-2.5 text-sm font-semibold transition-colors ${
        activa ? 'bg-neutral text-secondary' : 'text-base-content hover:text-primary'
      }`}
      onClick={onClick}
    >
      {children}
    </button>
  )
}

/**
 * El número del puesto sobre la foto. El primero va en amarillo sobre marrón, los
 * colores de la barra de pestañas, para que se lea como el lugar de honor sin
 * agregar medallas.
 */
function NumeroDelPuesto({ posicion, className = '' }: { posicion: number; className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={`grid size-9 place-items-center rounded-full font-display text-lg font-bold shadow-sm ${
        posicion === 1 ? 'bg-neutral text-secondary' : 'bg-base-100/90 text-base-content'
      } ${className}`}
    >
      {posicion}
    </span>
  )
}

/** Una de las tres primeras: la tarjeta con foto, como en Explorar. */
function PuestoDelPodio({ puesto, destacado = false }: { puesto: Puesto; destacado?: boolean }) {
  const { item, posicion, nota, detalle } = puesto
  return (
    <Link
      to={`/burger-joints/${item.burgerJointId}`}
      className="relative flex h-full flex-col overflow-hidden rounded-box bg-base-100 ring-1 ring-inset ring-base-content/15 transition-shadow hover:shadow-md"
    >
      <JointPhoto
        src={item.photoUrl}
        name={item.name}
        className={`w-full object-cover ${destacado ? 'aspect-[16/9] md:aspect-[21/9]' : 'aspect-[4/3] md:aspect-[16/10]'}`}
      />
      <div className="checker-strip" />
      <NumeroDelPuesto posicion={posicion} className="absolute left-3 top-3" />
      <div className={`flex flex-1 flex-col gap-1 ${destacado ? 'p-4' : 'p-3'}`}>
        <h2
          className={`font-display font-bold ${destacado ? 'line-clamp-1 text-xl' : 'line-clamp-2 text-base leading-snug'}`}
        >
          <span className="sr-only">Puesto {posicion}: </span>
          {item.name}
        </h2>
        <p className="line-clamp-1 text-sm text-base-content/70">
          {destacado ? shortAddress(item.address, item.area) : (item.area ?? shortAddress(item.address, item.area))}
        </p>
        <span className="mt-auto flex flex-wrap items-center gap-x-2 gap-y-1 pt-2">
          <ScoreBadge score={nota} size="sm" />
          <span className="text-xs text-base-content/70">{detalle}</span>
        </span>
      </div>
    </Link>
  )
}

/** Del cuarto puesto para abajo: una fila para recorrer rápido. */
function FilaDelRanking({ puesto }: { puesto: Puesto }) {
  const { item, posicion, nota, detalle } = puesto
  return (
    <Link
      to={`/burger-joints/${item.burgerJointId}`}
      className="flex items-center gap-3 p-3 transition-colors hover:bg-base-200"
    >
      <span className="w-7 flex-none text-center font-display text-base font-bold tabular-nums text-base-content/70">
        {posicion}
      </span>
      <JointPhoto src={item.photoUrl} name={item.name} className="size-14 flex-none rounded-field object-cover" />
      <div className="flex min-w-0 flex-1 flex-col">
        <span className="truncate font-display font-semibold">{item.name}</span>
        <span className="truncate text-sm text-base-content/70">{shortAddress(item.address, item.area)}</span>
      </div>
      <div className="flex flex-none flex-col items-end gap-1">
        <ScoreBadge score={nota} size="sm" />
        <span className="text-xs text-base-content/70">{detalle}</span>
      </div>
    </Link>
  )
}

/** La forma del podio mientras llega, para que la página no salte. */
function PodioCargando() {
  return (
    <div aria-hidden="true" className="grid grid-cols-2 gap-3">
      {[0, 1, 2].map((n) => (
        <div
          key={n}
          className={`overflow-hidden rounded-box ring-1 ring-inset ring-base-content/10 ${n === 0 ? 'col-span-2' : ''}`}
        >
          <div className={`skeleton w-full rounded-none ${n === 0 ? 'aspect-[16/9] md:aspect-[21/9]' : 'aspect-[4/3] md:aspect-[16/10]'}`} />
          <div className="flex flex-col gap-2 p-3">
            <div className="skeleton h-5 w-2/3" />
            <div className="skeleton h-4 w-1/2" />
          </div>
        </div>
      ))}
    </div>
  )
}
