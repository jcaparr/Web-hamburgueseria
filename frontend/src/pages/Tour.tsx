import { useState } from 'react'
import { apiClient } from '../api/client'
import { Interruptor } from '../components/Interruptor'
import { LoadError } from '../components/LoadError'
import { Recorrido } from '../components/Recorrido'
import { SelectorDeBarrios } from '../components/SelectorDeBarrios'
import { useAuth } from '../context/useAuth'
import { useBarrios } from '../hooks/useBarrios'
import { useTitulo } from '../hooks/useTitulo'
import type { SavedTour, Tour as TourRecorrido } from '../types'
import { routeUrl } from '../utils/maps'

/** Lo que admite Maps en un enlace de direcciones, que es lo que limita el recorrido. */
const MAXIMO_DE_PARADAS = 10

/**
 * Los topes que se ofrecen, que no son los mismos a pie que en auto: ocho cuadras es una
 * caminata y en auto no es nada.
 */
const KILOMETROS = {
  A_PIE: [1, 2, 3, 5, 8, 12],
  EN_AUTO: [5, 10, 20, 30, 50, 80],
}

/**
 * Cuántos recorridos ya propuestos se le recuerdan al servidor.
 *
 * Van todos para que cada vuelta proponga uno nuevo, pero viajan en la URL, así que la
 * lista no puede crecer sin fin. Veinte son más recorridos de los que nadie pide seguidos.
 */
const RECORDAR = 20

type Modo = 'A_PIE' | 'EN_AUTO'
type Ubicacion = { lat: number; lon: number }

export function Tour() {
  const { user } = useAuth()
  useTitulo('Armar un tour')

  const barriosDisponibles = useBarrios()
  const [barrios, setBarrios] = useState<string[]>([])
  const [cantidad, setCantidad] = useState(4)
  const [tope, setTope] = useState<number | null>(null)
  const [modo, setModo] = useState<Modo>('A_PIE')
  const [incluirVisitadas, setIncluirVisitadas] = useState(true)
  const [conCadenas, setConCadenas] = useState(false)

  const [ubicacion, setUbicacion] = useState<Ubicacion | null>(null)
  const [buscandoUbicacion, setBuscandoUbicacion] = useState(false)
  const [sinUbicacion, setSinUbicacion] = useState<string | null>(null)

  const [tour, setTour] = useState<TourRecorrido | null>(null)
  const [armando, setArmando] = useState(false)
  const [error, setError] = useState<unknown>(null)

  // Los que ya se propusieron en esta vuelta, cada uno como sus ids separados por coma.
  // Con solo el último, pedir otro alternaba entre dos.
  const [propuestos, setPropuestos] = useState<string[]>([])
  const [excluirLasDeMisTours, setExcluirLasDeMisTours] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [guardado, setGuardado] = useState<SavedTour | null>(null)

  /**
   * Dónde está quien va a hacer el recorrido, pedido recién cuando hace falta.
   *
   * Sin barrios el recorrido sale desde ahí, pero la ubicación se pide al tocar "Armar
   * tour" y no al entrar: antes el navegador recibía al visitante con su cartel de
   * permiso apenas abría la pantalla, antes de que hubiera hecho nada.
   *
   * Negarse es una respuesta válida: el recorrido se arma igual, empezando por una bien
   * puntuada en vez de por la más cercana. Y no se vuelve a preguntar en cada intento:
   * el navegador ya contestó.
   */
  function pedirUbicacion(): Promise<Ubicacion | null> {
    if (ubicacion) return Promise.resolve(ubicacion)
    if (sinUbicacion || !navigator.geolocation) return Promise.resolve(null)

    setBuscandoUbicacion(true)
    return new Promise((listo) => {
      navigator.geolocation.getCurrentPosition(
        ({ coords }) => {
          const encontrada = { lat: coords.latitude, lon: coords.longitude }
          setUbicacion(encontrada)
          setBuscandoUbicacion(false)
          listo(encontrada)
        },
        () => {
          setSinUbicacion('Sin tu ubicación, el recorrido empieza por una bien puntuada.')
          setBuscandoUbicacion(false)
          listo(null)
        },
        { timeout: 10_000 },
      )
    })
  }

  async function armar() {
    setArmando(true)
    const desde = barrios.length === 0 ? await pedirUbicacion() : null
    apiClient
      .get<TourRecorrido>('/tours', {
        params: {
          cantidad,
          kilometrosMaximos: tope ?? undefined,
          barrios: barrios.length > 0 ? barrios : undefined,
          latitud: desde?.lat,
          longitud: desde?.lon,
          incluirVisitadas,
          conCadenas,
          modo,
          excluirLasDeMisTours: user ? excluirLasDeMisTours : false,
          distintoDe: propuestos,
        },
      })
      .then(({ data }) => {
        setTour(data)
        setError(null)
        setGuardado(null)
        if (data.paradas.length > 0) {
          const ids = data.paradas.map((p) => p.local.id).join('-')
          setPropuestos((previos) => [...previos, ids].slice(-RECORDAR))
        }
      })
      .catch(setError)
      .finally(() => setArmando(false))
  }

  function guardar() {
    if (!tour) return

    setGuardando(true)
    apiClient
      .post<SavedTour>('/tours', {
        paradas: tour.paradas.map((p) => p.local.id),
        modo,
      })
      .then(({ data }) => {
        setGuardado(data)
        setError(null)
      })
      .catch(setError)
      .finally(() => setGuardando(false))
  }

  function alternarBarrio(barrio: string) {
    setBarrios((previos) =>
      previos.includes(barrio) ? previos.filter((b) => b !== barrio) : [...previos, barrio],
    )
  }

  const paradas = tour?.paradas ?? []
  const enlace = routeUrl(paradas.map((p) => p.local), {
    desde: barrios.length === 0 && ubicacion ? ubicacion : undefined,
    enAuto: modo === 'EN_AUTO',
  })

  return (
    <div className="flex flex-col gap-5 p-4 md:p-0">
      <div>
        <h1 className="font-display text-3xl font-extrabold leading-tight md:text-4xl">Armar un tour</h1>
        <p className="mt-1 text-sm text-base-content/70">
          Un recorrido de hamburgueserías para hacer {modo === 'A_PIE' ? 'caminando' : 'en auto'}.
        </p>
      </div>

      <section className="flex flex-col gap-5 rounded-box bg-base-100 p-4 ring-1 ring-inset ring-base-content/15">
        <div role="group" aria-label="Cómo vas" className="join w-full">
          {(['A_PIE', 'EN_AUTO'] as const).map((opcion) => (
            <button
              key={opcion}
              type="button"
              aria-pressed={modo === opcion}
              // Los topes de un modo no significan lo mismo en el otro, así que el que
              // estaba elegido se suelta en vez de arrastrarse.
              onClick={() => {
                setModo(opcion)
                setTope(null)
              }}
              className={`btn join-item flex-1 ${modo === opcion ? 'btn-primary' : ''}`}
            >
              {opcion === 'A_PIE' ? 'A pie' : 'En auto'}
            </button>
          ))}
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-baseline justify-between">
            <label htmlFor="cuantas-paradas" className="text-sm font-semibold">
              Cuántas paradas
            </label>
            <span aria-hidden="true" className="font-display text-lg font-bold text-primary">
              {cantidad}
            </span>
          </div>
          <input
            id="cuantas-paradas"
            type="range"
            min={2}
            max={MAXIMO_DE_PARADAS}
            value={cantidad}
            onChange={(e) => setCantidad(Number(e.target.value))}
            className="range range-primary"
          />
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-baseline justify-between">
            <span id="cuanto-recorrer" className="text-sm font-semibold">
              {modo === 'A_PIE' ? 'Cuánto caminar' : 'Cuánto manejar'}
            </span>
            <span className="font-display text-lg font-bold text-primary">
              {tope ? `${tope} km` : 'Sin límite'}
            </span>
          </div>
          <div role="group" aria-labelledby="cuanto-recorrer" className="flex flex-wrap gap-2">
            {KILOMETROS[modo].map((km) => (
              <button
                key={km}
                type="button"
                onClick={() => setTope(tope === km ? null : km)}
                aria-pressed={tope === km}
                className={`btn btn-sm ${tope === km ? 'btn-primary' : 'btn-outline'}`}
              >
                {km} km
              </button>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-2">
          <span id="en-que-barrios" className="text-sm font-semibold">
            En qué barrios
          </span>
          {/* El mismo selector que en Explorar, con buscador. Era un desplegable de más
              de doscientas casillas sin forma de buscar, y con el teclado eran doscientas
              paradas del Tab. */}
          <div role="group" aria-labelledby="en-que-barrios" className="flex">
            <SelectorDeBarrios
              barrios={barriosDisponibles}
              elegidos={barrios}
              onAlternar={alternarBarrio}
              onLimpiar={() => setBarrios([])}
              textoSinElegir="Cerca mío"
              claseDelBoton="btn-block justify-start font-normal"
            />
          </div>
          {barrios.length === 0 && (
            <p aria-live="polite" className="text-xs text-base-content/70">
              {buscandoUbicacion
                ? 'Buscando dónde estás…'
                : (sinUbicacion ??
                  'Sin barrios, el recorrido arranca donde estás vos: al armarlo te vamos a pedir la ubicación.')}
            </p>
          )}
          {/* Sacar los barrios es volver a la ubicación: el botón dice eso. */}
          {barrios.length > 0 && (
            <button
              type="button"
              onClick={() => setBarrios([])}
              className="btn btn-ghost btn-sm self-start text-primary"
            >
              Usar mi ubicación
            </button>
          )}
        </div>

        {/* Juntos y sin hueco entre uno y otro: cada fila ya mide 44 px de alto. */}
        <div className="flex flex-col">
          <Interruptor activo={conCadenas} onCambiar={setConCadenas}>
            Incluir cadenas de comida rápida
          </Interruptor>

          {user && (
            <>
              <Interruptor activo={incluirVisitadas} onCambiar={setIncluirVisitadas}>
                Incluir las que ya puntuaste
              </Interruptor>

              <Interruptor activo={excluirLasDeMisTours} onCambiar={setExcluirLasDeMisTours}>
                No repetir hamburgueserías de mis recorridos
              </Interruptor>
            </>
          )}
        </div>

        <button
          type="button"
          onClick={armar}
          disabled={armando}
          className="btn btn-primary btn-block"
        >
          {armando ? 'Armando…' : tour ? 'Armar otro' : 'Armar tour'}
        </button>
      </section>

      {error ? (
        <LoadError error={error} onRetry={armar} />
      ) : (
        tour && (
          <Recorrido
            tour={tour}
            enlace={enlace}
            enAuto={modo === 'EN_AUTO'}
            puedeGuardar={Boolean(user)}
            guardando={guardando}
            guardado={guardado}
            onGuardar={guardar}
          />
        )
      )}
    </div>
  )
}
