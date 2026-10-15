import { useRef, useState } from 'react'
import { apiClient } from '../api/client'
import { IconSearch } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { ResultadosDeGente } from '../components/ResultadosDeGente'
import { TarjetaDeFeed } from '../components/TarjetaDeFeed'
import { useAuth } from '../context/useAuth'
import { estadoDeLaBusqueda, useBuscarGente } from '../hooks/useBuscarGente'
import { usePedido } from '../hooks/usePedido'
import { useTitulo } from '../hooks/useTitulo'
import type { ItemDeFeed, PaginaDeFeed } from '../types'

type Fuente = 'TODOS' | 'SIGUIENDO'

/** Una página del feed y de qué pestaña es. */
type ListaDelFeed = PaginaDeFeed & { fuente: Fuente }

const PESTANIAS: { fuente: Fuente; texto: string }[] = [
  { fuente: 'TODOS', texto: 'Para vos' },
  { fuente: 'SIGUIENDO', texto: 'Siguiendo' },
]

/**
 * Lo último que se reseñó, de todos o de quienes seguís.
 *
 * Arranca en "Para vos", que es todo, y no en "Siguiendo". El día que alguien se
 * registra no sigue a nadie: si la pantalla de entrada fuera la de los seguidos,
 * lo primero que vería sería un vacío, y no tendría de dónde sacar a quién seguir.
 */
export function Feed() {
  const { user } = useAuth()
  useTitulo('Feed')
  const [fuente, setFuente] = useState<Fuente>('TODOS')
  const [trayendoMas, setTrayendoMas] = useState(false)
  const [errorAlTraerMas, setErrorAlTraerMas] = useState<unknown>(null)
  const busqueda = useBuscarGente()
  const buscandoGente = busqueda.texto !== ''
  const campo = useRef<HTMLInputElement>(null)

  // Al cambiar de pestaña se vuelve a empezar: el cursor de una no sirve para la otra,
  // porque apunta a una reseña que en la otra lista puede no estar. La página lleva de
  // qué pestaña es, para no sumarle a una lo que llegue tarde de la otra.
  const pedido = usePedido(`feed:${fuente}`, () =>
    apiClient
      .get<PaginaDeFeed>('/feed', { params: { fuente } })
      .then(({ data }): ListaDelFeed => ({ fuente, ...data })),
  )
  const items: ItemDeFeed[] = pedido.datos?.items ?? []
  const siguiente = pedido.datos?.siguiente ?? null
  const cargando = pedido.cargando
  const error = pedido.error ?? errorAlTraerMas

  function cambiarDePestania(nueva: Fuente) {
    setFuente(nueva)
    setErrorAlTraerMas(null)
  }

  function reintentar() {
    setErrorAlTraerMas(null)
    pedido.reintentar()
  }

  function escribir(escrito: string) {
    const limpio = busqueda.setTexto(escrito)
    // Al volver de buscar, "Siguiendo" se pide de nuevo: puede haber gente recién
    // seguida, y antes eso pasaba solo porque buscar era otra página.
    if (buscandoGente && limpio === '' && fuente === 'SIGUIENDO') reintentar()
  }

  function traerMas() {
    if (!siguiente || trayendoMas) return

    const deQueFuente = fuente
    setTrayendoMas(true)
    apiClient
      .get<PaginaDeFeed>('/feed', { params: { fuente, cursor: siguiente } })
      .then(({ data }) => {
        pedido.actualizar((previa) =>
          previa.fuente === deQueFuente
            ? { fuente: deQueFuente, items: [...previa.items, ...data.items], siguiente: data.siguiente }
            : previa,
        )
      })
      .catch(setErrorAlTraerMas)
      .finally(() => setTrayendoMas(false))
  }

  return (
    // Angosto en pantalla grande, y no todo el ancho disponible: la foto se muestra en
    // un cuadrado tan ancho como la columna, así que una columna de 900 px pediría
    // fotos de 900 para verse nítida. A este ancho, el mínimo que se exige al subir
    // alcanza y sobra.
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-[560px] md:p-0 md:pt-6">
      {/* Sin título a la vista, porque la pestaña de abajo ya dice dónde se está; pero
          el lector de pantalla necesita uno para saber en qué página cayó. */}
      <h1 className="sr-only">Feed</h1>
      {/* Un buscador a la vista y no una lupa sola (#252): sin palabras, casi nadie se
          daba cuenta de que desde acá se encuentra gente para seguir. Se escribe acá
          mismo, sin ir a otra página: mientras hay algo escrito, la gente encontrada
          ocupa el lugar del feed, y al borrarlo el feed vuelve como estaba. Mismo
          aspecto que el buscador de Explorar. */}
      <div className="flex min-h-12 items-center gap-2 rounded-full bg-base-100 py-1 pl-4 pr-1 shadow-[var(--sombra-control)] focus-within:ring-2 focus-within:ring-primary md:pl-5">
        <IconSearch size={18} className="flex-none text-base-content/70" />
        <input
          ref={campo}
          type="search"
          value={busqueda.texto}
          onChange={(e) => escribir(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Escape') escribir('')
          }}
          placeholder="Buscá gente para seguir…"
          aria-label="Buscar gente por nombre de usuario"
          autoComplete="off"
          autoCapitalize="none"
          autoCorrect="off"
          spellCheck={false}
          enterKeyHint="search"
          className="min-w-0 flex-1 bg-transparent py-2 text-base outline-none placeholder:text-base-content/70 [&::-webkit-search-cancel-button]:appearance-none"
        />
        {/* La X propia y no la del navegador, que es chiquita y no está en todos. */}
        {buscandoGente && (
          <button
            type="button"
            onClick={() => {
              // El botón desaparece al borrar: el foco vuelve al campo y no se pierde.
              escribir('')
              campo.current?.focus()
            }}
            aria-label="Borrar la búsqueda y volver al feed"
            className="btn btn-circle btn-ghost size-11 flex-none text-base-content/70"
          >
            <span aria-hidden="true">✕</span>
          </button>
        )}
      </div>

      {buscandoGente ? (
        <ResultadosDeGente busqueda={busqueda} />
      ) : (
        <>
          <div role="tablist" aria-label="Qué reseñas ver" className="tabs tabs-box">
            {PESTANIAS.map((p) => (
              <button
                key={p.fuente}
                role="tab"
                type="button"
                aria-selected={fuente === p.fuente}
                onClick={() => cambiarDePestania(p.fuente)}
                className={`tab flex-1 ${fuente === p.fuente ? 'tab-active' : 'text-base-content/70'}`}
              >
                {p.texto}
              </button>
            ))}
          </div>

          {error ? (
            <LoadError error={error} onRetry={reintentar} />
          ) : (
            <>
              <div className="flex flex-col gap-3">
                {items.map((item) => (
                  <TarjetaDeFeed
                    key={item.ratingId}
                    item={item}
                    esMia={item.autorId === user?.userId}
                  />
                ))}
              </div>

              {cargando && <p role="status" className="text-sm text-base-content/70">Cargando…</p>}

              {!cargando && items.length === 0 && (
                <div className="flex flex-col items-start gap-3 tarjeta p-4">
                  <p className="text-sm text-base-content/80">
                    {fuente === 'SIGUIENDO'
                      ? 'Todavía no seguís a nadie, o quienes seguís no reseñaron nada.'
                      : 'Todavía no hay reseñas en la app.'}
                  </p>
                  {fuente === 'SIGUIENDO' && (
                    // Lleva al buscador de arriba, que ya busca acá mismo.
                    <button
                      type="button"
                      onClick={() => campo.current?.focus()}
                      className="btn btn-sm btn-primary"
                    >
                      Buscar gente
                    </button>
                  )}
                </div>
              )}

              {siguiente && (
                <button
                  type="button"
                  onClick={traerMas}
                  disabled={trayendoMas}
                  className="btn btn-outline btn-block"
                >
                  {trayendoMas ? 'Cargando más…' : 'Ver más'}
                </button>
              )}
            </>
          )}
        </>
      )}

      {/* Siempre montado, para que el lector de pantalla anuncie cómo va la búsqueda. */}
      <p role="status" className={buscandoGente ? 'text-sm text-base-content/70' : 'sr-only'}>
        {buscandoGente ? estadoDeLaBusqueda(busqueda) : ''}
      </p>
    </div>
  )
}
