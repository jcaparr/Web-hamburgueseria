import { useEffect, useState } from 'react'
import { apiClient } from '../api/client'
import { Interruptor } from '../components/Interruptor'
import { LoadError } from '../components/LoadError'
import { Recorrido } from '../components/Recorrido'
import { useAuth } from '../context/AuthContext'
import { useBarrios } from '../hooks/useBarrios'
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

  // Sin barrios el recorrido sale desde donde está quien camina, así que al soltar el
  // último se pide la ubicación. Se pide acá y no al entrar para no recibir al visitante
  // con un cartel del navegador antes de que haya hecho nada.
  useEffect(() => {
    if (barrios.length > 0 || ubicacion || !navigator.geolocation) return

    setBuscandoUbicacion(true)
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setUbicacion({ lat: coords.latitude, lon: coords.longitude })
        setSinUbicacion(null)
        setBuscandoUbicacion(false)
      },
      () => {
        // Negarse es una respuesta válida: el recorrido se arma igual, empezando por
        // una bien puntuada en vez de por la más cercana.
        setSinUbicacion('Sin tu ubicación, el recorrido empieza por una bien puntuada')
        setBuscandoUbicacion(false)
      },
      { timeout: 10_000 },
    )
  }, [barrios, ubicacion])

  function armar() {
    setArmando(true)
    apiClient
      .get<TourRecorrido>('/tours', {
        params: {
          cantidad,
          kilometrosMaximos: tope ?? undefined,
          barrios: barrios.length > 0 ? barrios : undefined,
          latitud: barrios.length === 0 ? ubicacion?.lat : undefined,
          longitud: barrios.length === 0 ? ubicacion?.lon : undefined,
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
        <h1 className="font-display text-2xl font-bold">Armar un tour</h1>
        <p className="mt-1 text-sm text-base-content/70">
          Un recorrido de hamburgueserías para hacer {modo === 'A_PIE' ? 'caminando' : 'en auto'}.
        </p>
      </div>

      <section className="flex flex-col gap-5 rounded-box bg-base-100 p-4 ring-1 ring-inset ring-base-content/15">
        <div className="join w-full">
          {(['A_PIE', 'EN_AUTO'] as const).map((opcion) => (
            <button
              key={opcion}
              type="button"
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
            <span className="text-sm font-semibold">Cuántas parar</span>
            <span className="font-display text-lg font-bold text-primary">{cantidad}</span>
          </div>
          <input
            type="range"
            min={2}
            max={MAXIMO_DE_PARADAS}
            value={cantidad}
            onChange={(e) => setCantidad(Number(e.target.value))}
            className="range range-sm range-primary"
          />
        </div>

        <div className="flex flex-col gap-2">
          <div className="flex items-baseline justify-between">
            <span className="text-sm font-semibold">
              {modo === 'A_PIE' ? 'Cuánto caminar' : 'Cuánto manejar'}
            </span>
            <span className="font-display text-lg font-bold text-primary">
              {tope ? `${tope} km` : 'Sin límite'}
            </span>
          </div>
          <div className="flex flex-wrap gap-2">
            {KILOMETROS[modo].map((km) => (
              <button
                key={km}
                type="button"
                onClick={() => setTope(tope === km ? null : km)}
                className={`btn btn-xs ${tope === km ? 'btn-primary' : 'btn-outline'}`}
              >
                {km} km
              </button>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-2">
          <span className="text-sm font-semibold">En qué barrios</span>
          <details className="dropdown w-full">
            <summary className="btn btn-block justify-between font-normal">
              <span className="truncate">
                {barrios.length === 0 ? 'Cerca mío' : barrios.join(', ')}
              </span>
              <span className="text-base-content/70">▾</span>
            </summary>
            <ul className="dropdown-content menu z-10 mt-1 max-h-72 w-full flex-nowrap overflow-y-auto rounded-box bg-base-100 p-2 shadow ring-1 ring-inset ring-base-content/15">
              {barriosDisponibles.map((barrio) => (
                <li key={barrio}>
                  <label className="flex cursor-pointer items-center gap-3">
                    <input
                      type="checkbox"
                      className="checkbox checkbox-sm checkbox-primary"
                      checked={barrios.includes(barrio)}
                      onChange={() => alternarBarrio(barrio)}
                    />
                    <span className="text-sm">{barrio}</span>
                  </label>
                </li>
              ))}
            </ul>
          </details>
          {barrios.length === 0 && (
            <p className="text-xs text-base-content/70">
              {buscandoUbicacion
                ? 'Buscando dónde estás...'
                : (sinUbicacion ?? 'Sin barrios, el recorrido arranca donde estás vos.')}
            </p>
          )}
          {barrios.length > 0 && (
            <button
              type="button"
              onClick={() => setBarrios([])}
              className="self-start text-xs font-semibold text-primary"
            >
              Limpiar barrios
            </button>
          )}
        </div>

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

        <button
          type="button"
          onClick={armar}
          disabled={armando}
          className="btn btn-primary btn-block"
        >
          {armando ? 'Armando...' : tour ? 'Armar otro' : 'Armar tour'}
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
