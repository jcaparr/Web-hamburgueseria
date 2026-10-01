import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconPin, IconSearch } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { JointPhoto } from '../components/JointPhoto'
import { ScoreBadge } from '../components/ScoreBadge'
import { SelectorDeBarrios } from '../components/SelectorDeBarrios'
import type { BurgerJoint, PageResponse } from '../types'
import { shortAddress } from '../utils/address'
import { mapsUrl } from '../utils/maps'

const PAGE_SIZE = 20

/**
 * Dónde se recuerda si alguien apagó las cadenas.
 *
 * Va en el navegador de cada visitante y no en su cuenta, para que también funcione
 * sin haberse registrado, que es como la mayoría entra a mirar.
 */
const CLAVE_CADENAS = 'explorar.conCadenas'

/** Dónde estaba mirando la lista, para volver al mismo lugar al apretar atrás. */
const CLAVE_SCROLL = 'explorar.scroll'

function leerPreferencia(): boolean {
  // En una ventana de incógnito, o con el almacenamiento bloqueado, esto tira error en
  // vez de devolver vacío. Ante la duda se muestran todas, que es lo que había antes.
  try {
    return window.localStorage.getItem(CLAVE_CADENAS) !== 'false'
  } catch {
    return true
  }
}

export function Explore() {
  // Los filtros viven en la dirección y no en memoria.
  //
  // Entrar a una hamburguesería y volver atrás perdía todo: la búsqueda escrita, el
  // barrio elegido y en qué página estabas, y había que rehacerlo para seguir mirando.
  // En la dirección, volver atrás los restablece solo, porque el navegador vuelve a la
  // dirección anterior y esa dirección los tiene.
  //
  // De paso, un listado filtrado se puede compartir o dejar en favoritos, que antes no
  // se podía: todas las búsquedas eran la misma dirección.
  const [parametros, setParametros] = useSearchParams()

  const query = parametros.get('q') ?? ''
  // Varios: el parámetro se repite, "?area=Palermo&area=Belgrano". Sigue llamándose
  // "area" en singular porque es lo que está escrito en las direcciones que la gente
  // dejó en favoritos, y una sola sigue andando igual.
  const barriosElegidos = parametros.getAll('area').filter(Boolean)
  // Un arreglo cambia de identidad en cada render, así que como dependencia del efecto
  // que pide la lista dispararía un pedido tras otro, sin parar. Lo que no cambia
  // mientras los barrios sean los mismos es este texto.
  const claveDeBarrios = barriosElegidos.join('|')
  const page = Number(parametros.get('pagina') ?? '0')
  // El interruptor de cadenas sigue recordándose en el navegador cuando la dirección no
  // dice nada: es una preferencia de quien mira, no parte de esta búsqueda.
  const conCadenas = parametros.has('cadenas')
    ? parametros.get('cadenas') !== 'no'
    : leerPreferencia()

  /**
   * Cambia un filtro y vuelve a la primera página.
   *
   * Reemplaza la entrada del historial en vez de agregar una: si cada letra tecleada
   * dejara una, salir de la pantalla pediría apretar atrás veinte veces.
   */
  function cambiar(cambios: Record<string, string | null>) {
    const nuevos = new URLSearchParams(parametros)
    for (const [clave, valor] of Object.entries(cambios)) {
      if (valor === null || valor === '') {
        nuevos.delete(clave)
      } else {
        nuevos.set(clave, valor)
      }
    }
    setParametros(nuevos, { replace: true })
  }

  /**
   * Marca o desmarca un barrio, que son varios y van repetidos en la dirección.
   *
   * No puede pasar por `cambiar`, que escribe un valor por clave: acá hay que borrar
   * todos los "area" que había y volver a ponerlos uno por uno.
   *
   * Y la lista nueva se calcula sobre los parámetros que llegan al setter, no sobre los
   * que se leyeron al dibujar. Con la lectura de afuera, dos clics seguidos antes de que
   * React propague el primero hacen que el segundo pise al primero: marcabas Quilmes y
   * después Quilmes Oeste, y quedaba solo Quilmes Oeste.
   */
  function alternarBarrio(barrio: string) {
    // Lo que hay puesto se lee de la dirección del navegador y no del estado de React.
    //
    // Dos clics seguidos, antes de que React vuelva a dibujar, leen los dos el mismo
    // valor viejo y el segundo pisa al primero: marcabas Quilmes y después Quilmes
    // Oeste, y quedaba solo Quilmes Oeste. Pasa igual con la forma funcional del setter,
    // porque lo que recibe también viene del último dibujo.
    //
    // La barra de direcciones, en cambio, ya quedó cambiada por el clic anterior. Acá es
    // la fuente de verdad: el filtro vive en la dirección justamente para que se pueda
    // compartir y para que volver atrás lo restablezca.
    const actuales = new URLSearchParams(window.location.search).getAll('area').filter(Boolean)
    const proximos = actuales.includes(barrio)
      ? actuales.filter((b) => b !== barrio)
      : [...actuales, barrio]

    setParametros(conBarrios(new URLSearchParams(window.location.search), proximos),
      { replace: true })
  }

  function limpiarBarrios() {
    setParametros(conBarrios(new URLSearchParams(window.location.search), []),
      { replace: true })
  }

  const [barrios, setBarrios] = useState<string[]>([])
  const [pageData, setPageData] = useState<PageResponse<BurgerJoint> | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<unknown>(null)
  // Sube con "Reintentar" para volver a correr la búsqueda con los mismos filtros.
  const [attempt, setAttempt] = useState(0)

  // Los barrios se piden una sola vez: son los que tienen al menos un local, y eso no
  // cambia mientras alguien mira la pantalla. Si falla, queda el selector en "Todos" y
  // el resto de Explorar funciona igual.
  useEffect(() => {
    apiClient
      .get<string[]>('/burger-joints/barrios')
      .then(({ data }) => setBarrios(data))
      .catch(() => setBarrios([]))
  }, [])

  useEffect(() => {
    const timeout = setTimeout(() => {
      setLoading(true)
      apiClient
        .get<PageResponse<BurgerJoint>>('/burger-joints', {
          params: {
            q: query || undefined,
            // Axios repite el parámetro por cada elemento del arreglo, que es lo que
            // espera el servidor. Vacío se omite, y eso quiere decir "todos".
            area: barriosElegidos.length > 0 ? barriosElegidos : undefined,
            conCadenas,
            page,
            size: PAGE_SIZE,
          },
        })
        .then(({ data }) => {
          setPageData(data)
          setError(null)
        })
        .catch(setError)
        .finally(() => setLoading(false))
    }, 300)

    return () => clearTimeout(timeout)
  }, [query, claveDeBarrios, conCadenas, page, attempt])

  // Al cambiar de página la lista se renueva entera, pero el navegador conserva el
  // scroll: quedabas a mitad de la página nueva, empezando a leer por el medio.
  //
  // Salvo al entrar, que es cuando se vuelve de una hamburguesería: ahí saltar arriba
  // sería perder el lugar de la lista, que es justamente lo que se quiere conservar.
  const items = pageData?.content ?? []

  const yaEstuvo = useRef(false)
  const yaSeVolvio = useRef(false)
  useEffect(() => {
    if (!yaEstuvo.current) {
      yaEstuvo.current = true
      return
    }
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [page])

  // Dónde estaba mirando, para volver al mismo lugar de la lista.
  //
  // El navegador solo no alcanza: los locales llegan después de pedirlos, así que al
  // volver la página mide cero y no hay a dónde bajar. Se guarda al salir y se
  // restablece recién cuando la lista está dibujada.
  useEffect(() => {
    if (loading || items.length === 0 || yaSeVolvio.current) return
    yaSeVolvio.current = true
    try {
      const guardado = window.sessionStorage.getItem(CLAVE_SCROLL)
      if (guardado) window.scrollTo({ top: Number(guardado) })
    } catch {
      // Sin almacenamiento se vuelve arriba, que es lo que pasaba antes.
    }
  }, [loading, items.length])

  /**
   * Anota dónde estaba la lista justo antes de entrar a una hamburguesería.
   *
   * Se anota en el clic y no escuchando el scroll: durante el cambio de pantalla la
   * posición pasa por valores intermedios, y escuchando se guardaba uno de esos en vez
   * del lugar donde estaba mirando.
   */
  function anotarDondeQuedo() {
    try {
      window.sessionStorage.setItem(CLAVE_SCROLL, String(window.scrollY))
    } catch {
      // Sin almacenamiento no se recuerda, y se vuelve arriba como antes.
    }
  }

  return (
    <div className="flex flex-col gap-4 p-4 md:p-0">
      <h1 className="font-display text-2xl font-bold">Explorar hamburgueserías</h1>

      <div className="flex items-center gap-2 rounded-xl border border-base-300 bg-base-100 px-4 py-2.5">
        <IconSearch size={16} className="text-base-content/50" />
        <input
          type="search"
          value={query}
          onChange={(e) => {
            cambiar({ q: e.target.value, pagina: null })
          }}
          placeholder="Buscar hamburguesería..."
          className="w-full bg-transparent text-sm outline-none placeholder:text-base-content/50"
        />
      </div>

      {/*
        * Las cadenas son 74 de los 417 locales, y 64 de esas son sucursales de
        * McDonald's, Burger King y Hamburguesas Extremas: entre las tres ocupan tres
        * páginas enteras de la lista. Quien busca dónde comer algo distinto las quiere
        * fuera del medio; quien busca la más cercana, no. Por eso es una decisión de
        * quien mira, y arranca mostrándolas.
        */}
      {/*
        * El barrio va al lado del buscador y no adentro: son dos preguntas distintas.
        * Buscar por nombre es "quiero este local"; elegir barrio es "quiero comer por
        * acá", que es lo que uno se pregunta cuando todavía no sabe adónde ir.
        */}
      <div className="flex flex-wrap items-center gap-3">
        <SelectorDeBarrios
          barrios={barrios}
          elegidos={barriosElegidos}
          onAlternar={alternarBarrio}
          onLimpiar={limpiarBarrios}
        />

        {barriosElegidos.length > 0 && (
          <button
            type="button"
            onClick={limpiarBarrios}
            className="btn btn-ghost btn-sm"
          >
            Ver todos
          </button>
        )}
      </div>

      <label className="flex cursor-pointer items-center gap-3 self-start text-sm">
        <input
          type="checkbox"
          className="toggle toggle-sm toggle-secondary shrink-0"
          checked={conCadenas}
          onChange={(e) => {
            const valor = e.target.checked
            cambiar({ cadenas: valor ? null : "no", pagina: null })
            try {
              window.localStorage.setItem(CLAVE_CADENAS, String(valor))
            } catch {
              // Sin almacenamiento la preferencia dura lo que dure la visita, nada más.
            }
          }}
        />
        <span>Mostrar cadenas de comida rápida</span>
      </label>

      {loading && <p className="text-sm text-base-content/60">Buscando...</p>}

      {error ? (
        <LoadError error={error} onRetry={() => setAttempt((n) => n + 1)} />
      ) : (
      <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((b) => (
          <li key={b.id} className="rounded-box bg-base-100 ring-1 ring-inset ring-base-content/15 overflow-hidden">
            <Link to={`/burger-joints/${b.id}`} onClick={anotarDondeQuedo}>
              <figure className="aspect-[4/3] bg-base-200">
                <JointPhoto src={b.photoUrl} name={b.name} className="h-full w-full object-cover" />
              </figure>
              <div className="checker-strip" />
              <div className="flex flex-col gap-1 p-4">
                <h2 className="font-display line-clamp-1 text-base font-bold">{b.name}</h2>
                <p className="line-clamp-2 text-xs text-base-content/60">{shortAddress(b.address, b.area)}</p>
                <div className="mt-2 flex items-center justify-between">
                  {b.averageScore ? (
                    <ScoreBadge score={b.averageScore} size="sm" />
                  ) : (
                    <span className="text-xs font-medium text-base-content/50">Sin calificaciones</span>
                  )}
                </div>
              </div>
            </Link>
            <div className="px-4 pb-4">
              <a
                href={mapsUrl(b.placeId, b.name, b.latitude, b.longitude)}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-1 text-xs font-semibold text-primary"
              >
                <IconPin />
                Ver en Maps
              </a>
            </div>
          </li>
        ))}
        {/* Decir qué filtro dejó la lista vacía, que es lo que hay que aflojar: con el
            barrio puesto, "con ese nombre" mandaba a cambiar lo que no era.
            Con varios se nombran todos: si no, no se sabe en cuál no hay nada. */}
        {!loading && items.length === 0 && (
          <p className="text-sm text-base-content/60">
            {barriosElegidos.length > 0
              ? `No encontramos hamburgueserías en ${enCastellano(barriosElegidos)}${query ? ' con ese nombre' : ''}.`
              : 'No encontramos hamburgueserías con ese nombre.'}
          </p>
        )}
      </ul>
      )}

      {!error && pageData && pageData.totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 py-2">
          <div className="join">
            <button
              className="btn join-item btn-sm"
              disabled={page === 0}
              onClick={() => cambiar({ pagina: String(Math.max(0, page - 1)) })}
            >
              «
            </button>
            <span className="btn join-item btn-sm btn-disabled bg-base-100">
              Página {pageData.number + 1} de {pageData.totalPages}
            </span>
            <button
              className="btn join-item btn-sm"
              disabled={pageData.last}
              onClick={() => cambiar({ pagina: String(page + 1) })}
            >
              »
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

/**
 * "Palermo, Belgrano y Núñez", que es como se enumera en castellano.
 *
 * Con una lista separada por comas hasta el final —"Palermo, Belgrano, Núñez"— el
 * mensaje de "no encontramos nada en..." se lee como si faltara algo.
 */
function enCastellano(barrios: string[]) {
  if (barrios.length === 1) return barrios[0]
  return `${barrios.slice(0, -1).join(', ')} y ${barrios[barrios.length - 1]}`
}

/** Los mismos parámetros pero con estos barrios, y de vuelta a la primera página. */
function conBarrios(previos: URLSearchParams, barrios: string[]) {
  const nuevos = new URLSearchParams(previos)
  nuevos.delete('area')
  barrios.forEach((barrio) => nuevos.append('area', barrio))
  nuevos.delete('pagina')
  return nuevos
}
