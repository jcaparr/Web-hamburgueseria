import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconPin, IconRoute } from '../components/icons'
import { JointPhoto } from '../components/JointPhoto'
import { LoadError } from '../components/LoadError'
import { ScoreBadge } from '../components/ScoreBadge'
import { useAuth } from '../context/AuthContext'
import type { Tour as TourRecorrido } from '../types'
import { shortAddress } from '../utils/address'
import { routeUrl } from '../utils/maps'

/** Lo que admite Maps en un enlace de direcciones, que es lo que limita el recorrido. */
const MAXIMO_DE_PARADAS = 10

const KILOMETROS = [1, 2, 3, 5, 8, 12, 20]

type Ubicacion = { lat: number; lon: number }

export function Tour() {
  const { user } = useAuth()

  const [barriosDisponibles, setBarriosDisponibles] = useState<string[]>([])
  const [barrios, setBarrios] = useState<string[]>([])
  const [cantidad, setCantidad] = useState(4)
  const [tope, setTope] = useState<number | null>(null)
  const [incluirVisitadas, setIncluirVisitadas] = useState(true)
  const [conCadenas, setConCadenas] = useState(false)

  const [ubicacion, setUbicacion] = useState<Ubicacion | null>(null)
  const [buscandoUbicacion, setBuscandoUbicacion] = useState(false)
  const [sinUbicacion, setSinUbicacion] = useState<string | null>(null)

  const [tour, setTour] = useState<TourRecorrido | null>(null)
  const [armando, setArmando] = useState(false)
  const [error, setError] = useState<unknown>(null)

  useEffect(() => {
    apiClient
      .get<string[]>('/tours/barrios')
      .then(({ data }) => setBarriosDisponibles(data))
      .catch(() => setBarriosDisponibles([]))
  }, [])

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
          // Sin esto, el botón devolvería siempre el mismo recorrido.
          semilla: Math.floor(Math.random() * 1_000_000),
        },
        // Los barrios van repetidos —barrios=Palermo&barrios=Boedo— y no separados por
        // comas, que es como los espera Spring.
        paramsSerializer: { indexes: null },
      })
      .then(({ data }) => {
        setTour(data)
        setError(null)
      })
      .catch(setError)
      .finally(() => setArmando(false))
  }

  function alternarBarrio(barrio: string) {
    setBarrios((previos) =>
      previos.includes(barrio) ? previos.filter((b) => b !== barrio) : [...previos, barrio],
    )
  }

  const paradas = tour?.paradas ?? []
  const enlace = routeUrl(
    paradas.map((p) => p.local),
    barrios.length === 0 && ubicacion ? ubicacion : undefined,
  )

  return (
    <div className="flex flex-col gap-5 p-4 md:p-0">
      <div>
        <h1 className="font-display text-2xl font-bold">Armar un tour</h1>
        <p className="mt-1 text-sm text-base-content/60">
          Un recorrido de hamburgueserías para hacer caminando.
        </p>
      </div>

      <section className="flex flex-col gap-5 rounded-box bg-base-100 p-4 ring-1 ring-inset ring-base-content/15">
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
            <span className="text-sm font-semibold">Cuánto caminar</span>
            <span className="font-display text-lg font-bold text-primary">
              {tope ? `${tope} km` : 'Sin límite'}
            </span>
          </div>
          <div className="flex flex-wrap gap-2">
            {KILOMETROS.map((km) => (
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
              <span className="text-base-content/50">▾</span>
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
            <p className="text-xs text-base-content/60">
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

        <label className="flex cursor-pointer items-center gap-3 text-sm">
          <input
            type="checkbox"
            className="toggle toggle-sm toggle-secondary shrink-0"
            checked={conCadenas}
            onChange={(e) => setConCadenas(e.target.checked)}
          />
          <span>Incluir cadenas de comida rápida</span>
        </label>

        {user && (
          <label className="flex cursor-pointer items-center gap-3 text-sm">
            <input
              type="checkbox"
              className="toggle toggle-sm toggle-secondary shrink-0"
              checked={incluirVisitadas}
              onChange={(e) => setIncluirVisitadas(e.target.checked)}
            />
            <span>Incluir las que ya puntuaste</span>
          </label>
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
        tour && <Recorrido tour={tour} enlace={enlace} />
      )}
    </div>
  )
}

function Recorrido({ tour, enlace }: { tour: TourRecorrido; enlace: string }) {
  if (tour.paradas.length === 0) {
    return (
      <p className="rounded-box bg-base-200 p-4 text-sm text-base-content/70">
        {tour.aviso ?? 'No salió ningún recorrido con esos filtros.'}
      </p>
    )
  }

  return (
    <section className="flex flex-col gap-4">
      <div className="flex items-center justify-around rounded-box bg-neutral p-4 text-base-100">
        <Dato valor={String(tour.paradas.length)} etiqueta="paradas" />
        <Dato valor={`${tour.kilometros}`} etiqueta="km a pie" />
        <Dato valor={enHoras(tour.minutos)} etiqueta="caminando" />
      </div>

      {tour.aviso && (
        <p className="rounded-box bg-base-200 px-4 py-3 text-sm text-base-content/70">
          {tour.aviso}
        </p>
      )}

      <ol className="flex flex-col gap-3">
        {tour.paradas.map((parada) => (
          <li
            key={parada.local.id}
            className="flex items-center gap-3 rounded-box bg-base-100 p-3 ring-1 ring-inset ring-base-content/15"
          >
            <span className="flex h-7 w-7 flex-none items-center justify-center rounded-full bg-neutral font-display text-sm font-bold text-secondary">
              {parada.orden}
            </span>

            <Link to={`/burger-joints/${parada.local.id}`} className="flex min-w-0 flex-1 items-center gap-3">
              <figure className="h-14 w-14 flex-none overflow-hidden rounded-xl bg-base-200">
                <JointPhoto
                  src={parada.local.photoUrl}
                  name={parada.local.name}
                  className="h-full w-full object-cover"
                />
              </figure>

              <div className="flex min-w-0 flex-col gap-0.5">
                <h2 className="font-display line-clamp-1 text-sm font-bold">{parada.local.name}</h2>
                <p className="line-clamp-1 text-xs text-base-content/60">
                  {shortAddress(parada.local.address, parada.local.area)}
                </p>
                <div className="mt-0.5 flex items-center gap-2">
                  {parada.local.averageScore ? (
                    <ScoreBadge score={parada.local.averageScore} size="sm" />
                  ) : (
                    <span className="text-[11px] font-medium text-base-content/50">
                      Sin calificaciones
                    </span>
                  )}
                  {parada.kilometros > 0 && (
                    <span className="text-[11px] text-base-content/50">
                      +{parada.kilometros} km
                    </span>
                  )}
                  {parada.visitada && (
                    <span className="text-[11px] font-semibold text-secondary">Ya fuiste</span>
                  )}
                </div>
              </div>
            </Link>
          </li>
        ))}
      </ol>

      <a
        href={enlace}
        target="_blank"
        rel="noopener noreferrer"
        className="btn btn-neutral btn-block"
      >
        <IconRoute size={18} />
        Abrir el recorrido en Maps
      </a>
      <p className="-mt-2 flex items-center justify-center gap-1 text-center text-xs text-base-content/50">
        <IconPin />
        Los kilómetros son una estimación: Maps te da el camino exacto.
      </p>
    </section>
  )
}

function Dato({ valor, etiqueta }: { valor: string; etiqueta: string }) {
  return (
    <div className="flex flex-col items-center">
      <span className="font-display text-xl font-bold text-secondary">{valor}</span>
      <span className="text-[11px] uppercase tracking-wide text-base-100/60">{etiqueta}</span>
    </div>
  )
}

/** 95 minutos se leen peor que "1 h 35". */
function enHoras(minutos: number): string {
  if (minutos < 60) return `${minutos} min`
  const horas = Math.floor(minutos / 60)
  const resto = minutos % 60
  return resto === 0 ? `${horas} h` : `${horas} h ${resto}`
}
