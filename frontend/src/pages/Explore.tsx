import { useEffect, useRef, useState } from 'react'
import { useNavigate, useNavigationType, useSearchParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconSearch } from '../components/icons'
import { Interruptor } from '../components/Interruptor'
import { LoadError } from '../components/LoadError'
import { MejorCalificadas } from '../components/MejorCalificadas'
import { AvisoVacio } from '../components/Seccion'
import { SelectorDeBarrios } from '../components/SelectorDeBarrios'
import { TarjetaDeLocal, TarjetaDeLocalCargando } from '../components/TarjetaDeLocal'
import { useAuth } from '../context/AuthContext'
import { useBarrios } from '../hooks/useBarrios'
import { useTitulo } from '../hooks/useTitulo'
import type { BurgerJoint, PageResponse } from '../types'
import { isSessionExpired } from '../utils/errors'

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
  const { user } = useAuth()
  const navigate = useNavigate()
  useTitulo(null)

  const query = parametros.get('q') ?? ''
  // Varios: el parámetro se repite, "?area=Palermo&area=Belgrano". Sigue llamándose
  // "area" en singular porque es lo que está escrito en las direcciones que la gente
  // dejó en favoritos, y una sola sigue andando igual.
  const barriosElegidos = parametros.getAll('area').filter(Boolean)
  // Un arreglo cambia de identidad en cada render, así que como dependencia del efecto
  // que pide la lista dispararía un pedido tras otro, sin parar. Lo que no cambia
  // mientras los barrios sean los mismos es este texto.
  const claveDeBarrios = JSON.stringify(barriosElegidos)
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
   * Lo que hay puesto se lee de la dirección del navegador y no del estado de React.
   * Dos clics seguidos, antes de que React vuelva a dibujar, leen los dos el mismo valor
   * viejo y el segundo pisa al primero: marcabas Quilmes y después Quilmes Oeste, y
   * quedaba solo Quilmes Oeste. Pasa igual con la forma funcional del setter, porque lo
   * que recibe también viene del último dibujo.
   *
   * La barra de direcciones, en cambio, ya quedó cambiada por el clic anterior. Acá es la
   * fuente de verdad: el filtro vive en la dirección justamente para que se pueda
   * compartir y para que volver atrás lo restablezca.
   */
  function alternarBarrio(barrio: string) {
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

  /** Prende o apaga las cadenas, y se acuerda de la elección para la próxima visita. */
  /** Saca el nombre y los barrios, que son lo que deja la lista vacía. Las cadenas no: son una preferencia. */
  function borrarFiltros() {
    const nuevos = conBarrios(new URLSearchParams(window.location.search), [])
    nuevos.delete('q')
    setParametros(nuevos, { replace: true })
  }

  function cambiarCadenas(valor: boolean) {
    cambiar({ cadenas: valor ? null : 'no', pagina: null })
    try {
      window.localStorage.setItem(CLAVE_CADENAS, String(valor))
    } catch {
      // Sin almacenamiento la preferencia dura lo que dure la visita, nada más.
    }
  }

  const barrios = useBarrios()
  const [pageData, setPageData] = useState<PageResponse<BurgerJoint> | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<unknown>(null)
  // Sube con "Reintentar" para volver a correr la búsqueda con los mismos filtros.
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    // Se rearma desde el texto y no se usa el arreglo de afuera: el efecto depende del
    // texto, que no cambia mientras los barrios sean los mismos, y usar el arreglo
    // obligaría a depender de él, que cambia en cada dibujo.
    const elegidos: string[] = JSON.parse(claveDeBarrios)

    const timeout = setTimeout(() => {
      setLoading(true)
      apiClient
        .get<PageResponse<BurgerJoint>>('/burger-joints', {
          params: {
            q: query || undefined,
            // Axios repite el parámetro por cada elemento del arreglo, que es lo que
            // espera el servidor. Vacío se omite, y eso quiere decir "todos".
            area: elegidos.length > 0 ? elegidos : undefined,
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
    // Sin deslizar si la persona pidió menos movimiento en su sistema.
    const sinMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    window.scrollTo({ top: 0, behavior: sinMovimiento ? 'auto' : 'smooth' })
  }, [page])

  // Dónde estaba mirando, para volver al mismo lugar de la lista.
  //
  // El navegador solo no alcanza: los locales llegan después de pedirlos, así que al
  // volver la página mide cero y no hay a dónde bajar. Se guarda al salir y se
  // restablece recién cuando la lista está dibujada.
  //
  // Solo al volver con "atrás". Se restablecía también al llegar desde la barra de
  // navegación, y Explorar se abría por la mitad, donde había quedado la última vez que
  // se tocó una tarjeta: tocar "Explorar" es empezar, y empieza arriba.
  const comoLlego = useNavigationType()
  useEffect(() => {
    if (loading || items.length === 0 || yaSeVolvio.current) return
    yaSeVolvio.current = true
    if (comoLlego !== 'POP') return
    try {
      const guardado = window.sessionStorage.getItem(CLAVE_SCROLL)
      if (guardado) window.scrollTo({ top: Number(guardado) })
    } catch {
      // Sin almacenamiento se vuelve arriba, que es lo que pasaba antes.
    }
  }, [loading, items.length, comoLlego])

  function marcarGuardada(id: number, guardada: boolean) {
    setPageData((previa) =>
      previa && {
        ...previa,
        content: previa.content.map((local) =>
          local.id === id ? { ...local, inWishlist: guardada } : local,
        ),
      },
    )
  }

  /**
   * Guarda o saca de guardadas desde la tarjeta, sin entrar a la ficha.
   *
   * El corazón se marca enseguida y se desmarca si el servidor no lo acepta: esperar la
   * respuesta para pintarlo se sentía como un toque que no anduvo. Sin sesión lleva a
   * ingresar, que después vuelve acá.
   */
  async function alternarGuardada(local: BurgerJoint) {
    if (!user) {
      navigate('/login')
      return
    }
    const guardar = !local.inWishlist
    marcarGuardada(local.id, guardar)
    try {
      if (guardar) await apiClient.post(`/wishlist/${local.id}`)
      else await apiClient.delete(`/wishlist/${local.id}`)
    } catch (err) {
      marcarGuardada(local.id, !guardar)
      if (isSessionExpired(err)) navigate('/login')
    }
  }

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

  const sinFiltros = !query && barriosElegidos.length === 0
  const titulo = barriosElegidos.length > 0
    ? `En ${enCastellano(barriosElegidos)}`
    : query
      ? 'Resultados'
      : 'Todas las hamburgueserías'

  return (
    <div className="flex flex-col gap-6 p-4 md:p-0">
      {/* Liviana a propósito. La primera versión iba en una banda marrón y, sumada a la
          fila de mejor calificadas, eran dos bloques pesados antes de la primera foto:
          acá lo que tiene que llamar la atención son las fotos de las tarjetas.

          El nombre y el barrio van juntos porque son las dos formas de buscar, aunque
          sean preguntas distintas: buscar por nombre es "quiero este local"; elegir
          barrio es "quiero comer por acá", que es lo que uno se pregunta cuando todavía
          no sabe adónde ir.

          Las sucursales de cadenas son cientos, y entre McDonald's, Burger King y
          Hamburguesas Extremas ocupan páginas enteras de la lista. Quien busca dónde
          comer algo distinto las quiere fuera del medio; quien busca la más cercana, no.
          Por eso es una decisión de quien mira, y arranca mostrándolas. */}
      <header className="flex flex-col gap-4 md:pt-2">
        <div className="flex flex-col gap-1">
          <h1 className="font-display text-2xl font-bold leading-tight md:text-3xl">
            ¿Dónde comemos hoy?
          </h1>
          <p className="text-sm text-base-content/70 md:text-base">
            Las hamburgueserías de Buenos Aires, con las notas de quienes fueron.
          </p>
        </div>

        <div className="flex flex-col gap-3 md:flex-row md:items-center">
          <label className="flex flex-1 items-center gap-2 rounded-field bg-base-100 px-4 py-3 shadow-sm ring-1 ring-inset ring-base-content/20 focus-within:ring-2 focus-within:ring-primary md:max-w-xl">
            <IconSearch size={18} className="flex-none text-base-content/70" />
            <input
              type="search"
              value={query}
              onChange={(e) => {
                cambiar({ q: e.target.value, pagina: null })
              }}
              placeholder="Buscar por nombre…"
              aria-label="Buscar hamburguesería por nombre"
              autoComplete="off"
              enterKeyHint="search"
              className="w-full bg-transparent text-base outline-none placeholder:text-base-content/70"
            />
          </label>
          <div className="flex items-center gap-2">
            <SelectorDeBarrios
              barrios={barrios}
              elegidos={barriosElegidos}
              onAlternar={alternarBarrio}
              onLimpiar={limpiarBarrios}
            />
            {barriosElegidos.length > 0 && (
              <button type="button" onClick={limpiarBarrios} className="btn btn-ghost btn-sm">
                Ver todos los barrios
              </button>
            )}
          </div>
        </div>

        <Interruptor activo={conCadenas} onCambiar={cambiarCadenas} className="self-start">
          Mostrar cadenas de comida rápida
        </Interruptor>
      </header>

      <section className="flex flex-col gap-4" aria-labelledby="titulo-de-la-lista">
        <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
          <h2 id="titulo-de-la-lista" className="font-display text-lg font-bold">
            {titulo}
          </h2>
          {/* Cuántas hay, en el mismo renglón que "Buscando…": así la lista no salta
              cuando termina de buscar, y quien filtra ve enseguida cuánto achicó. */}
          <p role="status" className="min-h-5 text-sm text-base-content/70">
            {loading
              ? 'Buscando…'
              : pageData && pageData.totalElements > 0
                ? cuantas(pageData.totalElements)
                : ''}
          </p>
        </div>

        {error ? (
          <LoadError error={error} onRetry={() => setAttempt((n) => n + 1)} />
        ) : items.length > 0 ? (
          <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {items.map((local) => (
              <li key={local.id}>
                <TarjetaDeLocal local={local} onAbrir={anotarDondeQuedo} onGuardar={alternarGuardada} />
              </li>
            ))}
          </ul>
        ) : loading ? (
          // La forma de las tarjetas mientras llegan, la primera vez: con el texto
          // "Buscando…" solo, la página quedaba vacía y después saltaba.
          <ul aria-hidden="true" className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {[0, 1, 2].map((n) => (
              <li key={n}>
                <TarjetaDeLocalCargando />
              </li>
            ))}
          </ul>
        ) : (
          pageData && (
            // Decir qué filtro dejó la lista vacía, que es lo que hay que aflojar: con
            // el barrio puesto, "con ese nombre" mandaba a cambiar lo que no era. Con
            // varios se nombran todos: si no, no se sabe en cuál no hay nada.
            <div className="flex flex-col items-start gap-3">
              <AvisoVacio>
                {barriosElegidos.length > 0
                  ? `No encontramos hamburgueserías en ${enCastellano(barriosElegidos)}${query ? ' con ese nombre' : ''}.`
                  : 'No encontramos hamburgueserías con ese nombre.'}
              </AvisoVacio>
              <button type="button" onClick={borrarFiltros} className="btn btn-outline btn-sm">
                Ver todas las hamburgueserías
              </button>
            </div>
          )
        )}
      </section>

      {!error && pageData && pageData.totalPages > 1 && (
        <nav aria-label="Páginas de resultados" className="flex items-center justify-between gap-3 py-2">
          <button
            type="button"
            className="btn btn-outline"
            disabled={page === 0}
            onClick={() => cambiar({ pagina: String(Math.max(0, page - 1)) })}
          >
            Anterior
          </button>
          <span className="text-sm tabular-nums text-base-content/70">
            Página {pageData.number + 1} de {pageData.totalPages}
          </span>
          <button
            type="button"
            className="btn btn-outline"
            disabled={pageData.last}
            onClick={() => cambiar({ pagina: String(page + 1) })}
          >
            Siguiente
          </button>
        </nav>
      )}

      {/* Al final y no arriba: arriba competía con la lista por la primera mirada.
          Acá llega justo cuando sirve, a quien recorrió la página y no se decidió. Solo
          sin filtros y en la primera página: con un filtro puesto ya se sabe qué se
          busca. */}
      {sinFiltros && page === 0 && !error && <MejorCalificadas />}
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

/** "1 hamburguesería", "1.548 hamburgueserías". */
function cuantas(n: number) {
  return n === 1 ? '1 hamburguesería' : `${n.toLocaleString('es-AR')} hamburgueserías`
}

/** Los mismos parámetros pero con estos barrios, y de vuelta a la primera página. */
function conBarrios(previos: URLSearchParams, barrios: string[]) {
  const nuevos = new URLSearchParams(previos)
  nuevos.delete('area')
  barrios.forEach((barrio) => nuevos.append('area', barrio))
  nuevos.delete('pagina')
  return nuevos
}
