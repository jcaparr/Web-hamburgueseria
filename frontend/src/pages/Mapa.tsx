import type { Feature, FeatureCollection, Point } from 'geojson'
import maplibregl from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { Protocol } from 'pmtiles'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { BuscadorDelMapa } from '../components/BuscadorDelMapa'
import { LoadError } from '../components/LoadError'
import { TarjetaDelMapa } from '../components/TarjetaDelMapa'
import { IconArrowLeft, IconMira } from '../components/icons'
import { useAuth } from '../context/useAuth'
import { usePedido } from '../hooks/usePedido'
import { useTitulo } from '../hooks/useTitulo'
import { useUbicacion, type PosicionExacta } from '../hooks/useUbicacion'
import type { BurgerJoint, LocalDelMapa } from '../types'
import { distanciaLegible, metrosEntre } from '../utils/distancia'
import { isSessionExpired } from '../utils/errors'
import { estiloDelMapa } from '../utils/estiloDelMapa'
import { nota } from '../utils/numeros'
import { claseDeNota } from '../utils/rangoDeNota'

/** El Obelisco: de donde arranca el mapa si no sabemos dónde está quien mira. */
const CENTRO_INICIAL: [number, number] = [-58.4, -34.6]
/** Argentina entera con un margen: no tiene sentido arrastrar el mapa a otro continente. */
const LIMITES: [[number, number], [number, number]] = [[-80, -58], [-48, -19]]
const FUENTE = 'locales'
/** Dónde está quien mira: el punto azul y, si se sabe, el círculo de cuánto se equivoca la ubicación. */
const FUENTE_YO = 'mi-posicion'
/** A este zoom un local nunca está agrupado (se agrupa hasta el 11) y se ve entero. */
const ZOOM_AL_BUSCAR = 16
const ZOOM_AL_UBICARSE = 14
/** Si la ubicación llega antes que esto, el mapa ya arranca ahí; si llega después, se desliza hasta ahí. */
const ARRANQUE_DIRECTO_MS = 1500
/** Desde este zoom (nivel de barrio) los puntos se dibujan grandes y con el sello completo. */
const ZOOM_DE_PUNTO_GRANDE = 15
/** Por debajo de este zoom (una ciudad entera a la vista) la foto va más chica todavía, con el sello mini. */
const ZOOM_DE_PUNTO_MINI = 14

let protocoloRegistrado = false

/** Quien pidió menos movimiento no ve el mapa deslizarse: los saltos son instantáneos. */
const sinMovimiento = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches

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

function comoPunto(p: { lat: number; lon: number }): FeatureCollection<Point> {
  return {
    type: 'FeatureCollection',
    features: [{ type: 'Feature', properties: {}, geometry: { type: 'Point', coordinates: [p.lon, p.lat] } }],
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
  const { ubicacion, pedirAhora } = useUbicacion()

  const pedido = usePedido('mapa', () =>
    apiClient.get<LocalDelMapa[]>('/burger-joints/mapa').then((r) => r.data),
  )
  const locales = useMemo(() => pedido.datos ?? [], [pedido.datos])
  const porId = useMemo(() => new Map(locales.map((l) => [l.id, l])), [locales])

  const contenedor = useRef<HTMLDivElement>(null)
  const mapa = useRef<maplibregl.Map | null>(null)
  const marcadores = useRef(new Map<string, maplibregl.Marker>())
  const tarjeta = useRef<HTMLDivElement>(null)
  const alFinalDelMapa = useRef<HTMLDivElement>(null)
  /** El punto que abrió la tarjeta: al cerrarla, el foco vuelve ahí. */
  const puntoQueAbrio = useRef<HTMLElement | null>(null)
  const [listo, setListo] = useState(false)
  const [sinMapaBase, setSinMapaBase] = useState(false)
  const [seleccionadoId, setSeleccionadoId] = useState<number | null>(null)
  const [guardadas, setGuardadas] = useState<Set<number>>(new Set())
  const [miPosicion, setMiPosicion] = useState<(Pick<PosicionExacta, 'lat' | 'lon'> & { precision: number }) | null>(null)
  const [buscandoUbicacion, setBuscandoUbicacion] = useState(false)
  const [avisoDeUbicacion, setAvisoDeUbicacion] = useState<string | null>(null)
  /** Si se eligió desde el buscador: el mapa vuela y se acerca; con un punto tocado solo se centra. */
  const zoomAlElegir = useRef<number | null>(null)
  const seleccionadoIdRef = useRef<number | null>(null)
  const seleccionado = seleccionadoId != null ? (porId.get(seleccionadoId) ?? null) : null
  const seleccionadoRef = useRef<LocalDelMapa | null>(null)

  useEffect(() => {
    seleccionadoRef.current = seleccionado
    seleccionadoIdRef.current = seleccionadoId
  }, [seleccionado, seleccionadoId])

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
    // De lejos los puntos se dibujan más chicos para que no se encimen (ver index.css).
    const ajustarTamanio = () => {
      contenedor.current?.classList.toggle('mapa-lejos', m.getZoom() < ZOOM_DE_PUNTO_GRANDE)
      contenedor.current?.classList.toggle('mapa-muy-lejos', m.getZoom() < ZOOM_DE_PUNTO_MINI)
    }
    m.on('zoom', ajustarTamanio)
    ajustarTamanio()
    m.on('load', () => setListo(true))
    // Si falta el archivo del mapa, los puntos siguen sirviendo: se avisa y listo.
    m.on('error', (e) => {
      const mensaje = String((e as { error?: Error }).error?.message ?? '')
      if (/pmtiles|tiles|Failed to fetch|404/i.test(mensaje)) setSinMapaBase(true)
    })
    // El estilo pide íconos (escudos de rutas, aeropuertos) que no tenemos: se callan con uno vacío.
    m.on('styleimagemissing', (e) => {
      if (!m.hasImage(e.id)) m.addImage(e.id, { width: 1, height: 1, data: new Uint8Array(4) })
    })
    m.on('click', () => setSeleccionadoId(null))
    // Si la persona ya movió o acercó el mapa con la mano, la ubicación que llegue después no se lo corre.
    m.on('movestart', (e) => {
      if ((e as { originalEvent?: unknown }).originalEvent) yaTocoElMapa.current = true
    })
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
      // Si el estilo todavía se está cargando (se acaba de cambiar el tema), MapLibre no deja
      // agregar nada: se reintenta cuando termina, que es cuando salta style.load.
      if (!m.getStyle()) return
      try {
        m.addSource(FUENTE, {
          type: 'geojson',
          data: comoPuntos(locales),
          cluster: true,
          clusterRadius: 44,
          clusterMaxZoom: 11,
        })
        // Sin dibujo propio: los puntos son del DOM. Hace falta una capa para que MapLibre
        // cargue los datos de la fuente y se puedan consultar.
        m.addLayer({
          id: `${FUENTE}-oculta`,
          type: 'circle',
          source: FUENTE,
          paint: { 'circle-radius': 1, 'circle-opacity': 0 },
        })
      } catch {
        m.once('style.load', poner)
      }
    }

    const dibujar = () => {
      if (!m.getSource(FUENTE) || !m.isSourceLoaded(FUENTE)) return
      const vistos = new Map<string, Feature<Point>>()
      // querySourceFeatures devuelve todo lo de los mosaicos cargados, también lo que quedó
      // afuera de la pantalla: se dibuja solo lo que está a la vista, con un margen para que
      // al arrastrar no aparezcan de golpe.
      const [[oeste, sur], [este, norte]] = m.getBounds().toArray()
      const margenX = (este - oeste) * 0.25
      const margenY = (norte - sur) * 0.25
      for (const f of m.querySourceFeatures(FUENTE) as unknown as Feature<Point>[]) {
        const [lon, lat] = f.geometry.coordinates
        if (lon < oeste - margenX || lon > este + margenX || lat < sur - margenY || lat > norte + margenY) continue
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
                m.easeTo({ center: f.geometry.coordinates as [number, number], zoom, duration: sinMovimiento() ? 0 : 500 })
              })
            })
          : elementoDeLocal(porId.get(Number(p.id)), (local, boton) => {
              puntoQueAbrio.current = boton
              setSeleccionadoId(local.id)
            })
        if (!elemento) continue
        marcadores.current.set(
          clave,
          new maplibregl.Marker({ element: elemento, anchor: 'center' })
            .setLngLat(f.geometry.coordinates as [number, number])
            .addTo(m),
        )
        elemento.classList.toggle('pin-mapa--elegido', clave === `l${seleccionadoIdRef.current}`)
      }
      // Con el teclado solo se recorren los puntos que están a la vista: los que quedaron en el
      // margen de afuera no son un lugar adonde tabular. Para saltear el mapa entero está el botón
      // "Saltar el mapa".
      for (const marcador of marcadores.current.values()) {
        const { lng, lat } = marcador.getLngLat()
        marcador.getElement().tabIndex = lng >= oeste && lng <= este && lat >= sur && lat <= norte ? 0 : -1
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
    // El último mosaico de la fuente puede terminar de cargar después de su 'sourcedata': 'idle'
    // salta cuando ya no queda nada pendiente, y ahí se completa lo que faltaba.
    m.on('idle', dibujar)
    dibujar()
    return () => {
      m.off('style.load', alCambiarElEstilo)
      m.off('sourcedata', dibujar)
      m.off('moveend', dibujar)
      m.off('zoomend', dibujar)
      m.off('idle', dibujar)
      for (const marcador of puestos.values()) marcador.remove()
      puestos.clear()
      // Si el mapa ya se destruyó (se salió de la página), no queda nada que sacarle.
      if (mapa.current !== m) return
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
    const opciones = {
      center: [seleccionado.longitude, seleccionado.latitude] as [number, number],
      // En el teléfono la hoja ocupa abajo: se corre el centro para que el punto quede arriba.
      padding: esEscritorio ? { top: 0, bottom: 0, left: 0, right: 0 } : { top: 0, bottom: 260, left: 0, right: 0 },
    }
    const zoom = zoomAlElegir.current
    zoomAlElegir.current = null
    if (zoom != null) {
      // Elegido en el buscador: el mapa vuela hasta el local, con zoom suficiente para que se separe del grupo.
      m.flyTo({ ...opciones, zoom: Math.max(m.getZoom(), zoom), duration: sinMovimiento() ? 0 : 1400 })
    } else {
      m.easeTo({ ...opciones, duration: sinMovimiento() ? 0 : 400 })
    }
    acomodarTarjeta()
  }, [seleccionado, acomodarTarjeta])

  // Al abrir la tarjeta el foco pasa a ella (con el teclado, lo que sigue es su nombre y sus botones)
  // y al cerrarla vuelve al punto que la abrió, para seguir recorriendo desde ahí.
  const habiaTarjeta = useRef(false)
  useEffect(() => {
    if (seleccionadoId != null) {
      habiaTarjeta.current = true
      tarjeta.current?.focus({ preventScroll: true })
    } else if (habiaTarjeta.current) {
      habiaTarjeta.current = false
      const punto = puntoQueAbrio.current
      if (punto?.isConnected) punto.focus({ preventScroll: true })
    }
  }, [seleccionadoId])

  // Escape cierra la tarjeta.
  useEffect(() => {
    function alTocarUnaTecla(e: KeyboardEvent) {
      if (e.key === 'Escape') setSeleccionadoId(null)
    }
    window.addEventListener('keydown', alTocarUnaTecla)
    return () => window.removeEventListener('keydown', alTocarUnaTecla)
  }, [])

  // El mapa abre en donde está la persona, con su punto azul. La ubicación se pide apenas se abre la
  // pantalla; el cartel del navegador lleva el tiempo que lleve contestarlo, así que si llega tarde el
  // mapa se desliza hasta ahí, salvo que ya lo haya movido o haya elegido un local. Si la niega queda
  // en la vista de siempre, sin carteles: no hay nada que avisar.
  const [abiertoEn] = useState(() => Date.now())
  const llegoEnMs = useRef<number | null>(null)
  const yaCentrado = useRef(false)
  const yaTocoElMapa = useRef(false)
  useEffect(() => {
    if (ubicacion && llegoEnMs.current == null) llegoEnMs.current = Date.now() - abiertoEn
  }, [ubicacion, abiertoEn])
  useEffect(() => {
    const m = mapa.current
    if (!m || !listo || !ubicacion || yaCentrado.current) return
    yaCentrado.current = true
    // Lo que se sabe acá va redondeado a unos 100 m: se dibuja el punto, sin círculo de precisión.
    setMiPosicion((previa) => previa ?? { lat: ubicacion.lat, lon: ubicacion.lon, precision: 0 })
    if (yaTocoElMapa.current || seleccionadoIdRef.current != null) return
    const destino = { center: [ubicacion.lon, ubicacion.lat] as [number, number], zoom: ZOOM_AL_UBICARSE }
    if ((llegoEnMs.current ?? 0) <= ARRANQUE_DIRECTO_MS) m.jumpTo(destino)
    else m.flyTo({ ...destino, duration: sinMovimiento() ? 0 : 1200 })
  }, [listo, ubicacion])

  const elegirDesdeElBuscador = useCallback((local: LocalDelMapa) => {
    zoomAlElegir.current = ZOOM_AL_BUSCAR
    puntoQueAbrio.current = null
    setSeleccionadoId(local.id)
  }, [])

  const centrarEnMiUbicacion = useCallback(async () => {
    const m = mapa.current
    if (!m || buscandoUbicacion) return
    setBuscandoUbicacion(true)
    setAvisoDeUbicacion(null)
    try {
      const posicion = await pedirAhora()
      setMiPosicion(posicion)
      m.flyTo({ center: [posicion.lon, posicion.lat], zoom: ZOOM_AL_UBICARSE, duration: sinMovimiento() ? 0 : 1200 })
    } catch (err) {
      const denegada = (err as { code?: number }).code === 1
      setAvisoDeUbicacion(
        denegada
          ? 'Activá la ubicación de este sitio en el navegador para centrar el mapa donde estás.'
          : 'No pudimos saber dónde estás. Probá de nuevo en un rato.',
      )
    } finally {
      setBuscandoUbicacion(false)
    }
  }, [buscandoUbicacion, pedirAhora])

  // El aviso se va solo.
  useEffect(() => {
    if (!avisoDeUbicacion) return
    const espera = window.setTimeout(() => setAvisoDeUbicacion(null), 7000)
    return () => window.clearTimeout(espera)
  }, [avisoDeUbicacion])

  // El punto azul y su círculo de precisión. Se vuelve a poner si cambia el estilo del mapa.
  useEffect(() => {
    const m = mapa.current
    if (!m || !listo || !miPosicion) return
    const poner = () => {
      if (!m.getStyle()) return
      try {
        const fuente = m.getSource(FUENTE_YO) as maplibregl.GeoJSONSource | undefined
        if (fuente) {
          fuente.setData(comoPunto(miPosicion))
          return
        }
        m.addSource(FUENTE_YO, { type: 'geojson', data: comoPunto(miPosicion) })
        if (miPosicion.precision > 0) {
          // Los metros de precisión pasados a píxeles: en cada zoom un metro mide el doble de píxeles que en
          // el anterior, y la escala depende de la latitud.
          const metrosPorPixelEnZoomCero = 78271.52 * Math.cos((miPosicion.lat * Math.PI) / 180)
          const radioEnZoomCero = Math.max(miPosicion.precision, 10) / metrosPorPixelEnZoomCero
          m.addLayer({
            id: FUENTE_YO + '-precision',
            type: 'circle',
            source: FUENTE_YO,
            paint: {
              'circle-radius': ['interpolate', ['exponential', 2], ['zoom'], 0, radioEnZoomCero, 22, radioEnZoomCero * 2 ** 22],
              'circle-color': '#2f7de1',
              'circle-opacity': 0.16,
              'circle-stroke-color': '#2f7de1',
              'circle-stroke-opacity': 0.5,
              'circle-stroke-width': 1,
            },
          })
        }
        m.addLayer({
          id: FUENTE_YO + '-punto',
          type: 'circle',
          source: FUENTE_YO,
          paint: { 'circle-radius': 8, 'circle-color': '#2f7de1', 'circle-stroke-color': '#ffffff', 'circle-stroke-width': 3 },
        })
      } catch {
        m.once('style.load', poner)
      }
    }
    poner()
    m.on('style.load', poner)
    return () => {
      m.off('style.load', poner)
      // Si el mapa ya se destruyó (se salió de la página), no queda nada que sacarle.
      if (mapa.current !== m) return
      for (const id of [FUENTE_YO + '-punto', FUENTE_YO + '-precision']) if (m.getLayer(id)) m.removeLayer(id)
      if (m.getSource(FUENTE_YO)) m.removeSource(FUENTE_YO)
    }
  }, [listo, miPosicion])

  const distancia =
    seleccionado && ubicacion
      ? distanciaLegible(
          metrosEntre(ubicacion, { lat: seleccionado.latitude, lon: seleccionado.longitude }),
        )
      : null

  return (
    <>
    <div className="relative h-[calc(100dvh-7.5rem)] min-h-[26rem] overflow-hidden bg-base-300 md:h-[calc(100dvh-9rem)] md:rounded-box md:ring-1 md:ring-inset md:ring-base-content/15">
      {/* MapLibre le pone position: relative a su contenedor, y eso pisaba el absolute y lo
          dejaba sin alto: el mapa va en un div propio que llena a este. */}
      {/* Hay hasta cientos de puntos: este botón es lo primero que se alcanza con el teclado y salta el mapa entero. */}
      <button
        type="button"
        onClick={() => alFinalDelMapa.current?.focus()}
        className="sr-only focus:not-sr-only focus:absolute focus:left-3 focus:top-3 focus:z-30 focus:inline-flex focus:min-h-11 focus:items-center focus:rounded-full focus:bg-neutral focus:px-4 focus:text-sm focus:font-semibold focus:text-secondary"
      >
        Saltar el mapa
      </button>
      <div className="absolute inset-0 isolate">
        <div ref={contenedor} className="h-full w-full" role="region" aria-label="Mapa de hamburgueserías" />
      </div>

      {/* Arriba: volver a la lista y buscar un local. En el teléfono "Lista" es solo la flecha, para que entren los dos. */}
      <div className="absolute inset-x-3 top-3 z-20 flex items-start gap-2 md:right-16">
        <Link
          to="/"
          aria-label="Volver a la lista"
          className="inline-flex min-h-11 flex-none items-center gap-2 rounded-full bg-base-100 px-3.5 text-sm font-semibold shadow-[var(--sombra-control)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary md:px-4"
        >
          <IconArrowLeft size={16} />
          <span className="max-md:sr-only">Lista</span>
        </Link>
        <BuscadorDelMapa locales={locales} onElegir={elegirDesdeElBuscador} />
      </div>

      {/* Abajo a la derecha, arriba de la atribución de OpenStreetMap. En el teléfono, con la tarjeta abierta sube para no quedar tapado. */}
      <button
        type="button"
        onClick={centrarEnMiUbicacion}
        disabled={buscandoUbicacion}
        aria-label="Centrar el mapa en mi ubicación"
        className={`absolute right-3 z-10 grid h-11 w-11 place-items-center rounded-full bg-base-100 shadow-[var(--sombra-control)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${miPosicion ? 'text-primary' : 'text-base-content'} ${seleccionado ? 'max-md:bottom-[17rem] md:bottom-9' : 'bottom-9'}`}
      >
        {buscandoUbicacion ? <span className="loading loading-spinner loading-sm" aria-hidden="true" /> : <IconMira size={22} />}
      </button>
      {avisoDeUbicacion && (
        <p
          role="status"
          className={`absolute right-3 z-10 max-w-[18rem] rounded-box bg-base-100 p-3 text-sm shadow-[var(--sombra-control)] ${seleccionado ? 'max-md:bottom-[20.5rem] md:bottom-[5.5rem]' : 'bottom-[5.5rem]'}`}
        >
          {avisoDeUbicacion}
        </p>
      )}

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
          role="dialog"
          aria-label={`Ficha de ${seleccionado.name}`}
          tabIndex={-1}
          className="absolute inset-x-0 bottom-0 z-20 focus:outline-none md:inset-x-auto md:bottom-auto md:left-0 md:top-0"
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
    {/* Adonde llega el foco al saltar el mapa. */}
    <div ref={alFinalDelMapa} tabIndex={-1} className="sr-only focus:outline-none">
      Fin del mapa
    </div>
    </>
  )
}

/**
 * El grupo de locales: una mini hamburguesa con la cantidad en el medallón; al tocarlo el mapa se
 * acerca. Está dibujada de arriba abajo: pan con sésamo, cheddar, lechuga, medallón y pan de abajo.
 */
function elementoDeGrupo(cuantos: number, alTocar: () => void): HTMLElement {
  const boton = document.createElement('button')
  boton.type = 'button'
  boton.className = 'grupo-mapa'
  // Más locales, más grande: 46, 52 o 60 px de ancho (con 46 el número queda de 12,5 px).
  boton.style.setProperty('--g', `${cuantos >= 30 ? 60 : cuantos >= 10 ? 52 : 46}px`)
  // La cantidad la cuenta MapLibre: es un número, no texto de nadie, así que entra tal cual.
  boton.innerHTML =
    '<svg viewBox="0 0 48 46" aria-hidden="true" focusable="false">' +
    '<path class="grupo-mapa__borde grupo-mapa__pan" d="M4 15C4 6.5 13 1.5 24 1.5S44 6.5 44 15z"/>' +
    '<ellipse class="grupo-mapa__semilla" cx="15" cy="8.5" rx="1.4" ry="0.85"/>' +
    '<ellipse class="grupo-mapa__semilla" cx="24" cy="6" rx="1.4" ry="0.85"/>' +
    '<ellipse class="grupo-mapa__semilla" cx="33" cy="8.5" rx="1.4" ry="0.85"/>' +
    '<ellipse class="grupo-mapa__semilla" cx="19.5" cy="11.5" rx="1.4" ry="0.85"/>' +
    '<ellipse class="grupo-mapa__semilla" cx="28.5" cy="11.5" rx="1.4" ry="0.85"/>' +
    '<path class="grupo-mapa__borde grupo-mapa__lechuga" d="M2.5 16.5H45.5V21Q42 25 38.5 21Q35 25 31.5 21Q28 25 24.5 21Q21 25 17.5 21Q14 25 10.5 21Q7 25 2.5 21z"/>' +
    '<path class="grupo-mapa__borde grupo-mapa__queso" d="M3 15H45V18H40.5L40 21.5Q39 23 37.5 21.5L37 18H11.5L11 20.5Q10 22 8.5 20.5L8 18H3z"/>' +
    '<rect class="grupo-mapa__borde grupo-mapa__carne" x="3.5" y="23.5" width="41" height="13.5" rx="6.75"/>' +
    '<path class="grupo-mapa__borde grupo-mapa__pan" d="M4 38.5H44V41C44 44 41.5 45 38 45H10C6.5 45 4 44 4 41z"/>' +
    `<text class="grupo-mapa__cantidad" x="24" y="34.6">${Math.trunc(cuantos)}</text>` +
    '</svg>'
  boton.setAttribute('aria-label', `${cuantos} hamburgueserías juntas. Acercar el mapa.`)
  boton.addEventListener('click', (e) => {
    e.stopPropagation()
    alTocar()
  })
  return boton
}

/** El punto de un local: su foto en un círculo y, si tiene reseñas, el sello con la nota. */
function elementoDeLocal(
  local: LocalDelMapa | undefined,
  alTocar: (local: LocalDelMapa, boton: HTMLElement) => void,
): HTMLElement | null {
  if (!local) return null
  const boton = document.createElement('button')
  boton.type = 'button'
  boton.className = 'pin-mapa'
  // Si se pisan, quedan arriba los de mejor nota.
  boton.style.zIndex = String(Math.round((local.averageScore ?? 0) * 10))
  boton.setAttribute(
    'aria-label',
    local.averageScore != null ? `${local.name}, nota ${nota(local.averageScore)}` : `${local.name}, sin reseñas`,
  )

  const foto = document.createElement('span')
  foto.className = 'pin-mapa__foto'
  if (!local.miniatura) foto.classList.add('pin-mapa__foto--vacia')
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
    // El anillo de foco del punto abarca también el sello (ver .pin-mapa--con-nota).
    boton.classList.add('pin-mapa--con-nota')
    const sello = document.createElement('span')
    sello.className = `pin-mapa__nota ${claseDeNota(local.averageScore)}`
    sello.textContent = nota(local.averageScore)
    boton.appendChild(sello)
  }

  boton.addEventListener('click', (e) => {
    e.stopPropagation()
    alTocar(local, boton)
  })
  return boton
}
