import type { Feature, FeatureCollection, Point } from 'geojson'
import maplibregl from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { Protocol } from 'pmtiles'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { LoadError } from '../components/LoadError'
import { TarjetaDelMapa } from '../components/TarjetaDelMapa'
import { IconArrowLeft } from '../components/icons'
import { useAuth } from '../context/useAuth'
import { usePedido } from '../hooks/usePedido'
import { useTitulo } from '../hooks/useTitulo'
import { useUbicacion } from '../hooks/useUbicacion'
import type { BurgerJoint, LocalDelMapa } from '../types'
import { distanciaLegible, metrosEntre } from '../utils/distancia'
import { isSessionExpired } from '../utils/errors'
import { estiloDelMapa } from '../utils/estiloDelMapa'
import { nota } from '../utils/numeros'

/** El Obelisco: de donde arranca el mapa si no sabemos dónde está quien mira. */
const CENTRO_INICIAL: [number, number] = [-58.4, -34.6]
/** Argentina entera con un margen: no tiene sentido arrastrar el mapa a otro continente. */
const LIMITES: [[number, number], [number, number]] = [[-80, -58], [-48, -19]]
const FUENTE = 'locales'

let protocoloRegistrado = false

/** El tema que se está viendo: el elegido en la app o, en automático, el del sistema. */
function temaActual(): 'claro' | 'oscuro' {
  const elegido = document.documentElement.getAttribute('data-theme')
  if (elegido === 'burger-noche') return 'oscuro'
  if (elegido === 'burger') return 'claro'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'oscuro' : 'claro'
}

/** Los locales como puntos para MapLibre, que es quien los agrupa. */
function comoPuntos(locales: LocalDelMapa[]): FeatureCollection<Point> {
  return {
    type: 'FeatureCollection',
    features: locales.map((l) => ({
      type: 'Feature',
      properties: { id: l.id },
      geometry: { type: 'Point', coordinates: [l.longitude, l.latitude] },
    })),
  }
}

/**
 * El mapa de hamburgueserías (#395): el mapa base sale de un archivo nuestro (Protomaps,
 * datos de OpenStreetMap) y cada local es su foto en un círculo con la nota abajo.
 *
 * Los puntos son botones del DOM y no capas del mapa: así se pueden enfocar con el teclado,
 * leer con un lector de pantalla y tocar con 44 px. Se dibujan solo los que entran en
 * pantalla, ya agrupados por MapLibre, así que nunca son más de unas decenas.
 */
export function Mapa() {
  useTitulo('Mapa')
  const { user } = useAuth()
  const navigate = useNavigate()
  const { ubicacion } = useUbicacion()

  const pedido = usePedido('mapa', () =>
    apiClient.get<LocalDelMapa[]>('/burger-joints/mapa').then((r) => r.data),
  )
  const locales = useMemo(() => pedido.datos ?? [], [pedido.datos])
  const porId = useMemo(() => new Map(locales.map((l) => [l.id, l])), [locales])

  const contenedor = useRef<HTMLDivElement>(null)
  const mapa = useRef<maplibregl.Map | null>(null)
  const marcadores = useRef(new Map<string, maplibregl.Marker>())
  const tarjeta = useRef<HTMLDivElement>(null)
  const [listo, setListo] = useState(false)
  const [sinMapaBase, setSinMapaBase] = useState(false)
  const [seleccionadoId, setSeleccionadoId] = useState<number | null>(null)
  const [guardadas, setGuardadas] = useState<Set<number>>(new Set())
  const seleccionado = seleccionadoId != null ? (porId.get(seleccionadoId) ?? null) : null
  const seleccionadoRef = useRef<LocalDelMapa | null>(null)

  useEffect(() => {
    seleccionadoRef.current = seleccionado
  }, [seleccionado])

  // Los guardados de quien mira, para pintar el corazón. Sin sesión no hay nada que pedir.
  useEffect(() => {
    if (!user) return
    let vigente = true
    apiClient
      .get<BurgerJoint[]>('/wishlist')
      .then(({ data }) => vigente && setGuardadas(new Set(data.map((b) => b.id))))
      .catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [user])

  const alternarGuardado = useCallback(async () => {
    const local = seleccionadoRef.current
    if (!local) return
    if (!user) return navigate('/login')
    const estaba = guardadas.has(local.id)
    // Se ve el cambio al instante y se deshace si el servidor lo rechaza.
    setGuardadas((previas) => {
      const nuevas = new Set(previas)
      if (estaba) nuevas.delete(local.id)
      else nuevas.add(local.id)
      return nuevas
    })
    try {
      if (estaba) await apiClient.delete(`/wishlist/${local.id}`)
      else await apiClient.post(`/wishlist/${local.id}`)
    } catch (err) {
      setGuardadas((previas) => {
        const nuevas = new Set(previas)
        if (estaba) nuevas.add(local.id)
        else nuevas.delete(local.id)
        return nuevas
      })
      if (isSessionExpired(err)) navigate('/login')
    }
  }, [user, guardadas, navigate])

  /** Pone la tarjeta al lado del punto (escritorio); en el teléfono la hoja va abajo y no se mueve. */
  const acomodarTarjeta = useCallback(() => {
    const m = mapa.current
    const local = seleccionadoRef.current
    const caja = tarjeta.current
    if (!m || !local || !caja || !window.matchMedia('(min-width: 768px)').matches) return
    const punto = m.project([local.longitude, local.latitude])
    const { clientWidth: ancho, clientHeight: alto } = m.getContainer()
    const x = Math.min(Math.max(punto.x + 36, 8), ancho - caja.offsetWidth - 8)
    const y = Math.min(Math.max(punto.y - caja.offsetHeight / 2, 8), alto - caja.offsetHeight - 8)
    caja.style.transform = `translate(${x}px, ${y}px)`
  }, [])

  // Crear el mapa una sola vez.
  useEffect(() => {
    if (!contenedor.current) return
    if (!protocoloRegistrado) {
      maplibregl.addProtocol('pmtiles', new Protocol().tile)
      protocoloRegistrado = true
    }

    const m = new maplibregl.Map({
      container: contenedor.current,
      style: estiloDelMapa(temaActual()),
      center: CENTRO_INICIAL,
      zoom: 11,
      minZoom: 3,
      maxZoom: 18,
      maxBounds: LIMITES,
      // La atribución de OpenStreetMap tiene que verse siempre, no detrás de una "i".
      attributionControl: { compact: false },
      // Sin giro: el mapa se usa para ubicarse, y rotarlo por error desorienta.
      dragRotate: false,
      pitchWithRotate: false,
    })
    m.touchZoomRotate.disableRotation()
    // Botones de más y menos solo donde hay mouse; en el teléfono se usan los dedos.
    if (window.matchMedia('(hover: hover) and (pointer: fine)').matches) {
      m.addControl(new maplibregl.NavigationControl({ showCompass: false }), 'top-right')
    }
    m.on('load', () => setListo(true))
    // Si falta el archivo del mapa, los puntos siguen sirviendo: se avisa y listo.
    m.on('error', (e) => {
      const mensaje = String((e as { error?: Error }).error?.message ?? '')
      if (/pmtiles|tiles|Failed to fetch|404/i.test(mensaje)) setSinMapaBase(true)
    })
    m.on('click', () => setSeleccionadoId(null))
    m.on('move', acomodarTarjeta)
    mapa.current = m

    const marcadoresActuales = marcadores.current
    return () => {
      for (const marcador of marcadoresActuales.values()) marcador.remove()
      marcadoresActuales.clear()
      m.remove()
      mapa.current = null
      setListo(false)
    }
  }, [acomodarTarjeta])

  // Cambiar el estilo si cambia el tema mientras se mira el mapa.
  useEffect(() => {
    const m = mapa.current
    if (!m || !listo) return
    let ultimo = temaActual()
    const revisar = () => {
      const tema = temaActual()
      if (tema === ultimo) return
      ultimo = tema
      m.setStyle(estiloDelMapa(tema))
    }
    const observador = new MutationObserver(revisar)
    observador.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
    const sistema = window.matchMedia('(prefers-color-scheme: dark)')
    sistema.addEventListener('change', revisar)
    return () => {
      observador.disconnect()
      sistema.removeEventListener('change', revisar)
    }
  }, [listo])

  // La fuente de los puntos: se arma con los locales y se rearma si el estilo se cambia.
  useEffect(() => {
    const m = mapa.current
    if (!m || !listo || locales.length === 0) return

    const puestos = marcadores.current
    const poner = () => {
      if (m.getSource(FUENTE)) return
      m.addSource(FUENTE, {
        type: 'geojson',
        data: comoPuntos(locales),
        cluster: true,
        clusterRadius: 56,
        clusterMaxZoom: 16,
      })
      // Sin dibujo propio: los puntos son del DOM. Hace falta una capa para que MapLibre
      // cargue los datos de la fuente y se puedan consultar.
      m.addLayer({
        id: `${FUENTE}-oculta`,
        type: 'circle',
        source: FUENTE,
        paint: { 'circle-radius': 1, 'circle-opacity': 0 },
      })
    }

    const dibujar = () => {
      if (!m.getSource(FUENTE) || !m.isSourceLoaded(FUENTE)) return
      const vistos = new Map<string, Feature<Point>>()
      for (const f of m.querySourceFeatures(FUENTE) as unknown as Feature<Point>[]) {
        const p = f.properties ?? {}
        const clave = p.cluster ? `c${p.cluster_id}` : `l${p.id}`
        vistos.set(clave, f)
      }
      for (const [clave, marcador] of marcadores.current) {
        if (!vistos.has(clave)) {
          marcador.remove()
          marcadores.current.delete(clave)
        }
      }
      for (const [clave, f] of vistos) {
        if (marcadores.current.has(clave)) continue
        const p = f.properties ?? {}
        const elemento = p.cluster
          ? elementoDeGrupo(Number(p.point_count), () => {
              const fuente = m.getSource(FUENTE) as maplibregl.GeoJSONSource
              fuente.getClusterExpansionZoom(Number(p.cluster_id)).then((zoom) => {
                m.easeTo({ center: f.geometry.coordinates as [number, number], zoom })
              })
            })
          : elementoDeLocal(porId.get(Number(p.id)), (local) => setSeleccionadoId(local.id))
        if (!elemento) continue
        marcadores.current.set(
          clave,
          new maplibregl.Marker({ element: elemento, anchor: 'center' })
            .setLngLat(f.geometry.coordinates as [number, number])
            .addTo(m),
        )
      }
    }

    poner()
    const alCambiarElEstilo = () => {
      poner()
      dibujar()
    }
    m.on('style.load', alCambiarElEstilo)
    m.on('sourcedata', dibujar)
    m.on('moveend', dibujar)
    m.on('zoomend', dibujar)
    dibujar()
    return () => {
      m.off('style.load', alCambiarElEstilo)
      m.off('sourcedata', dibujar)
      m.off('moveend', dibujar)
      m.off('zoomend', dibujar)
      for (const marcador of puestos.values()) marcador.remove()
      puestos.clear()
      if (m.getLayer(`${FUENTE}-oculta`)) m.removeLayer(`${FUENTE}-oculta`)
      if (m.getSource(FUENTE)) m.removeSource(FUENTE)
    }
  }, [listo, locales, porId])

  // El punto elegido se marca, y se mueve el mapa para que no lo tape la hoja.
  useEffect(() => {
    for (const [clave, marcador] of marcadores.current) {
      marcador.getElement().classList.toggle('pin-mapa--elegido', clave === `l${seleccionadoId}`)
    }
  })
  useEffect(() => {
    const m = mapa.current
    if (!m || !seleccionado) return
    const esEscritorio = window.matchMedia('(min-width: 768px)').matches
    m.easeTo({
      center: [seleccionado.longitude, seleccionado.latitude],
      // En el teléfono la hoja ocupa abajo: se corre el centro para que el punto quede arriba.
      padding: esEscritorio ? { top: 0, bottom: 0, left: 0, right: 0 } : { top: 0, bottom: 260, left: 0, right: 0 },
      duration: 400,
    })
    acomodarTarjeta()
  }, [seleccionado, acomodarTarjeta])

  // Escape cierra la tarjeta.
  useEffect(() => {
    function alTocarUnaTecla(e: KeyboardEvent) {
      if (e.key === 'Escape') setSeleccionadoId(null)
    }
    window.addEventListener('keydown', alTocarUnaTecla)
    return () => window.removeEventListener('keydown', alTocarUnaTecla)
  }, [])

  // Si se sabe dónde está quien mira y no hay nada elegido, el mapa arranca ahí.
  const yaCentrado = useRef(false)
  useEffect(() => {
    const m = mapa.current
    if (!m || !listo || !ubicacion || yaCentrado.current) return
    yaCentrado.current = true
    m.jumpTo({ center: [ubicacion.lon, ubicacion.lat], zoom: 13 })
  }, [listo, ubicacion])

  const distancia =
    seleccionado && ubicacion
      ? distanciaLegible(
          metrosEntre(ubicacion, { lat: seleccionado.latitude, lon: seleccionado.longitude }),
        )
      : null

  return (
    <div className="relative h-[calc(100dvh-7.5rem)] min-h-[26rem] overflow-hidden bg-base-300 md:h-[calc(100dvh-9rem)] md:rounded-box md:ring-1 md:ring-inset md:ring-base-content/15">
      {/* MapLibre le pone position: relative a su contenedor, y eso pisaba el absolute y lo
          dejaba sin alto: el mapa va en un div propio que llena a este. */}
      <div className="absolute inset-0">
        <div ref={contenedor} className="h-full w-full" role="region" aria-label="Mapa de hamburgueserías" />
      </div>

      <Link
        to="/"
        className="absolute left-3 top-3 z-10 inline-flex min-h-11 items-center gap-2 rounded-full bg-base-100 px-4 text-sm font-semibold shadow-[var(--sombra-control)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
      >
        <IconArrowLeft size={16} />
        Lista
      </Link>

      {pedido.error !== undefined && !pedido.datos && (
        <div className="absolute inset-x-3 top-16 z-10">
          <LoadError error={pedido.error} onRetry={pedido.reintentar} />
        </div>
      )}
      {pedido.cargando && (
        <p role="status" className="absolute left-1/2 top-3 z-10 -translate-x-1/2 rounded-full bg-base-100 px-4 py-2.5 text-sm shadow-[var(--sombra-control)]">
          Cargando el mapa…
        </p>
      )}
      {!pedido.cargando && pedido.error === undefined && locales.length > 0 && (
        <p className="sr-only" role="status">
          {locales.length} hamburgueserías en el mapa. Nota promedio de la primera: {locales[0].averageScore != null ? nota(locales[0].averageScore) : 'sin reseñas'}.
        </p>
      )}
      {sinMapaBase && (
        <p role="status" className="absolute inset-x-3 bottom-3 z-10 rounded-box bg-base-100 p-3 text-sm shadow-[var(--sombra-control)]">
          No pudimos cargar el mapa de fondo. Los locales se ven igual, pero sin calles.
        </p>
      )}

      {seleccionado && (
        <div
          ref={tarjeta}
          className="absolute inset-x-0 bottom-0 z-20 md:inset-x-auto md:bottom-auto md:left-0 md:top-0"
        >
          <TarjetaDelMapa
            local={seleccionado}
            distancia={distancia}
            guardado={guardadas.has(seleccionado.id)}
            onGuardar={alternarGuardado}
            onCerrar={() => setSeleccionadoId(null)}
          />
        </div>
      )}
    </div>
  )
}

/** El círculo con el número de locales que hay juntos; al tocarlo el mapa se acerca. */
function elementoDeGrupo(cuantos: number, alTocar: () => void): HTMLElement {
  const boton = document.createElement('button')
  boton.type = 'button'
  boton.className = 'grupo-mapa'
  boton.textContent = String(cuantos)
  boton.setAttribute('aria-label', `${cuantos} hamburgueserías juntas. Acercar el mapa.`)
  boton.addEventListener('click', (e) => {
    e.stopPropagation()
    alTocar()
  })
  return boton
}

/** El punto de un local: su foto en un círculo y, si tiene reseñas, el sello con la nota. */
function elementoDeLocal(local: LocalDelMapa | undefined, alTocar: (local: LocalDelMapa) => void): HTMLElement | null {
  if (!local) return null
  const boton = document.createElement('button')
  boton.type = 'button'
  boton.className = 'pin-mapa'
  boton.setAttribute(
    'aria-label',
    local.averageScore != null ? `${local.name}, nota ${nota(local.averageScore)}` : `${local.name}, sin reseñas`,
  )

  const foto = document.createElement('span')
  foto.className = 'pin-mapa__foto'
  if (local.miniatura) {
    const img = document.createElement('img')
    img.src = local.miniatura
    img.alt = ''
    img.loading = 'lazy'
    img.decoding = 'async'
    foto.appendChild(img)
  }
  boton.appendChild(foto)

  if (local.averageScore != null) {
    const sello = document.createElement('span')
    sello.className = 'pin-mapa__nota'
    sello.textContent = nota(local.averageScore)
    boton.appendChild(sello)
  }

  boton.addEventListener('click', (e) => {
    e.stopPropagation()
    alTocar(local)
  })
  return boton
}
