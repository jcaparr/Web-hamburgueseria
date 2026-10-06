import { Link, useSearchParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { JointPhoto } from '../components/JointPhoto'
import { LoadError } from '../components/LoadError'
import { MedidorGrande } from '../components/MedidorGrande'
import { ScoreBadge } from '../components/ScoreBadge'
import { AvisoVacio } from '../components/Seccion'
import { useAuth } from '../context/useAuth'
import { usePedido } from '../hooks/usePedido'
import { useTitulo } from '../hooks/useTitulo'
import type { PageResponse, RankingItem } from '../types'
import { shortAddress } from '../utils/address'
import { nota } from '../utils/numeros'

type Vista = 'nota' | 'resenias' | 'mio'

/**
 * Las tres listas, en una sola fila de pestañas. Antes eran dos controles apilados
 * —las pestañas general y mío, y abajo los botones de orden—, cada uno con su color
 * fuerte, y eran lo primero que se veía de la página.
 *
 * Los nombres dicen por qué está cada una en su puesto. "Más populares" ordenaba por
 * cantidad de reseñas, así que ahora se llama así.
 */
const VISTAS: { id: Vista; texto: string; bajada: string }[] = [
  { id: 'nota', texto: 'Mejor nota', bajada: 'Las que mejor puntuó la gente.' },
  { id: 'resenias', texto: 'Más reseñas', bajada: 'Las que más gente calificó.' },
  { id: 'mio', texto: 'Mi ranking', bajada: 'Las hamburgueserías que calificaste, de tu nota más alta a la más baja.' },
]

function esVista(valor: string | null): valor is Vista {
  return VISTAS.some((v) => v.id === valor)
}

/** Lo que cuenta en cada puesto: la nota que lo puso ahí y una línea que la explica. */
interface Puesto {
  item: RankingItem
  posicion: number
  nota: number
  detalle: string
}

export function Ranking() {
  const { user } = useAuth()
  useTitulo('Ranking')
  // La lista que se mira va en la dirección: al volver de una ficha se vuelve a la
  // misma, y se puede pasar el enlace.
  const [parametros, setParametros] = useSearchParams()
  const pedida = parametros.get('vista')
  const vista: Vista = esVista(pedida) ? pedida : 'nota'

  function elegir(nueva: Vista) {
    // replace: cambiar de pestaña no es ir a otra página, y "atrás" tiene que salir
    // del ranking, no recorrer las pestañas que se tocaron.
    setParametros(nueva === 'nota' ? {} : { vista: nueva }, { replace: true })
  }

  // Sin sesión "Mi ranking" no tiene qué pedir. Antes se pedía igual: volvía 401 y en
  // pantalla quedaba la lista del ranking general debajo del aviso de iniciar sesión,
  // como si fuera la tuya.
  const sinSesion = vista === 'mio' && !user
  const pedido = usePedido(
    sinSesion ? null : vista === 'mio' ? `mio:${user?.userId}` : `general:${vista}`,
    () =>
      (vista === 'mio'
        ? apiClient.get<PageResponse<RankingItem>>('/ranking/mine')
        : apiClient.get<PageResponse<RankingItem>>('/ranking/general', {
            params: { order: vista === 'resenias' ? 'popularity' : 'score' },
          })
      ).then(({ data }) => data.content),
  )
  // Al cambiar de pestaña queda la lista anterior hasta que llega la nueva, que es menos
  // salto que vaciarla.
  const items = pedido.datos ?? pedido.datosAnteriores ?? []
  const loading = pedido.cargando
  const error = pedido.error

  const puestos: Puesto[] = items.map((item, index) => ({
    item,
    posicion: index + 1,
    nota: vista === 'mio' ? (item.myScore ?? item.averageScore) : item.averageScore,
    detalle:
      vista === 'mio'
        ? `Promedio ${nota(item.averageScore)}`
        : item.ratingsCount === 1
          ? '1 reseña'
          : `${item.ratingsCount} reseñas`,
  }))
  const [primero, ...siguientes] = puestos
  // La primera vez, o al cambiar de pestaña con la lista vacía: la forma de la lista.
  const cargandoDeCero = loading && items.length === 0
  const actual = VISTAS.find((v) => v.id === vista)!

  return (
    <div className="flex flex-col gap-5 p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-2">
      {/* El título y sus pestañas son un bloque: el nombre de la sección y las tres formas
          de mirarla, sin aire entre medio. */}
      <div className="flex flex-col gap-1.5">
        <h1 className="titulo-pagina">Ranking</h1>
        <div role="tablist" aria-label="Qué ranking ver" className="flex border-b border-base-content/15">
          {VISTAS.map((v) => (
            <button
              key={v.id}
              type="button"
              role="tab"
              id={`pestania-${v.id}`}
              aria-selected={vista === v.id}
              aria-controls="lista-del-ranking"
              onClick={() => elegir(v.id)}
              className={`-mb-px min-h-11 flex-1 cursor-pointer border-b-2 px-2 text-sm font-semibold transition-colors ${
                vista === v.id
                  ? 'border-primary text-base-content'
                  : 'border-transparent text-base-content/70 hover:text-base-content'
              }`}
            >
              {v.texto}
            </button>
          ))}
        </div>
      </div>

      <section
        id="lista-del-ranking"
        role="tabpanel"
        aria-labelledby={`pestania-${vista}`}
        aria-busy={loading}
        className="flex flex-col gap-4"
      >
        {sinSesion ? (
          <AvisoVacio accion={{ texto: 'Iniciar sesión', a: '/login' }}>
            Tu ranking se arma solo con las notas que vas poniendo. Iniciá sesión para verlo.
          </AvisoVacio>
        ) : error ? (
          <LoadError error={error} onRetry={pedido.reintentar} />
        ) : cargandoDeCero ? (
          <ListaCargando />
        ) : !primero ? (
          vista === 'mio' ? (
            <AvisoVacio accion={{ texto: 'Explorar hamburgueserías', a: '/' }}>
              Todavía no calificaste ninguna hamburguesería. Cuando lo hagas, tu ranking se
              arma acá.
            </AvisoVacio>
          ) : (
            <AvisoVacio>Todavía nadie calificó ninguna hamburguesería.</AvisoVacio>
          )
        ) : (
          <>
            <p className="text-sm text-base-content/70">{actual.bajada}</p>

            {/* Una sola foto grande, la del primer puesto: es lo que se viene a ver, y
                lo único que levanta la voz en la página. La versión anterior tenía
                tres fotos grandes, cada una con su tira a cuadros y su número, y era
                mucho para lo que es una lista. */}
            <PrimerPuesto puesto={primero} />

            {siguientes.length > 0 && (
              <ol start={2} className="flex flex-col divide-y divide-base-content/10">
                {siguientes.map((puesto) => (
                  <li key={puesto.item.burgerJointId}>
                    <FilaDelRanking puesto={puesto} />
                  </li>
                ))}
              </ol>
            )}
          </>
        )}
      </section>
    </div>
  )
}

/** El barrio solo: en una lista para recorrer, la calle es de más. */
function lugarDe(item: RankingItem) {
  return item.area ?? shortAddress(item.address, item.area)
}

function PrimerPuesto({ puesto }: { puesto: Puesto }) {
  const { item, nota, detalle } = puesto
  return (
    <Link
      to={`/burger-joints/${item.burgerJointId}`}
      className="flex flex-col overflow-hidden tarjeta transition-shadow hover:tarjeta-alzada"
    >
      <div className="relative">
        <JointPhoto
          src={item.photoUrl}
          name={item.name}
          className="aspect-[16/9] w-full object-cover md:aspect-[21/9]"
        />
        {/* El amarillo sobre marrón de la pestaña activa de la barra: se lee como el
            lugar de honor sin agregar medallas. */}
        <span
          aria-hidden="true"
          className="absolute left-3 top-3 grid size-9 place-items-center rounded-full bg-neutral font-display text-lg font-bold text-secondary"
        >
          1
        </span>
      </div>
      {/* La nota del primero en el burgómetro grande, el del logo: es el puesto que se
          viene a ver, y el único de la lista que lo lleva. */}
      <div className="flex items-center justify-between gap-3 p-4">
        <div className="flex min-w-0 flex-col gap-0.5">
          <h2 className="line-clamp-2 font-display text-2xl font-extrabold leading-tight">
            <span className="sr-only">Puesto 1: </span>
            {item.name}
          </h2>
          <p className="truncate text-sm text-base-content/70">{lugarDe(item)}</p>
        </div>
        <MedidorGrande valor={nota} tamanio="md" etiqueta="Nota">
          <span className="mt-1 text-xs text-base-content/70">{detalle}</span>
        </MedidorGrande>
      </div>
    </Link>
  )
}

/**
 * Del segundo puesto para abajo: número, foto chica, nombre y nota, sin caja.
 *
 * La nota va en el mismo sello amarillo que la del primero. Probé una versión sin el
 * fondo para que la lista hiciera menos ruido, pero el sello es como se reconoce una
 * nota en toda la app, y un primer puesto con sello y el resto sin se leía como dos
 * cosas distintas.
 */
function FilaDelRanking({ puesto }: { puesto: Puesto }) {
  const { item, posicion, nota, detalle } = puesto
  return (
    <Link
      to={`/burger-joints/${item.burgerJointId}`}
      className="-mx-2 flex items-center gap-3 rounded-field px-2 py-3 transition-colors hover:bg-base-100"
    >
      {/* El segundo y el tercero en kétchup, como el podio que les falta: el primero
          ya tiene su foto grande arriba. */}
      <span
        className={`w-7 flex-none text-right font-display text-2xl font-extrabold tabular-nums ${
          posicion <= 3 ? 'text-primary' : 'text-base-content/70'
        }`}
      >
        {posicion}
      </span>
      <JointPhoto src={item.photoUrl} name={item.name} className="size-11 flex-none rounded-field object-cover" />
      <div className="flex min-w-0 flex-1 flex-col">
        <span className="truncate font-display text-base font-semibold">{item.name}</span>
        <span className="truncate text-sm text-base-content/70">{lugarDe(item)}</span>
      </div>
      <div className="flex flex-none flex-col items-end gap-0.5">
        <ScoreBadge score={nota} size="sm" />
        <span className="text-xs text-base-content/70">{detalle}</span>
      </div>
    </Link>
  )
}

/** La forma de la lista mientras llega, para que la página no salte. */
function ListaCargando() {
  return (
    <div aria-hidden="true" className="flex flex-col gap-4">
      <div className="skeleton h-4 w-1/2" />
      <div className="overflow-hidden tarjeta">
        <div className="skeleton aspect-[16/9] w-full rounded-none md:aspect-[21/9]" />
        <div className="flex flex-col gap-2 p-4">
          <div className="skeleton h-5 w-2/3" />
          <div className="skeleton h-4 w-1/3" />
        </div>
      </div>
      <div className="flex flex-col">
        {[0, 1, 2, 3].map((n) => (
          <div key={n} className="flex items-center gap-3 py-3">
            <div className="skeleton h-5 w-6" />
            <div className="skeleton size-11 rounded-field" />
            <div className="flex flex-1 flex-col gap-1.5">
              <div className="skeleton h-4 w-1/2" />
              <div className="skeleton h-3 w-1/4" />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
