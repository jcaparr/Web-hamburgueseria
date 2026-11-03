import { useCallback, useEffect, useState } from 'react'

export interface Ubicacion {
  lat: number
  lon: number
}

const CLAVE = 'ubicacion.actual'

/** Cuánto se espera la respuesta del navegador antes de mostrar la lista sin ubicación. */
const TOPE_DE_ESPERA_MS = 2_000

/** Con tres decimales (unos 100 m) alcanza para ordenar por cercanía, y no mandamos más. */
function redondeada(n: number) {
  return Math.round(n * 1000) / 1000
}

/** Lo que se recuerda mientras dure la pestaña, para no volver a pedirla ni reordenar la lista. */
function recordar(encontrada: Ubicacion) {
  try {
    window.sessionStorage.setItem(CLAVE, JSON.stringify(encontrada))
  } catch {
    // Sin almacenamiento se vuelve a pedir la próxima vez, nada más.
  }
}

/** La posición tal como la dio el navegador, con su precisión en metros: es lo que usa el mapa. */
export interface PosicionExacta extends Ubicacion {
  precision: number
}

function guardada(): Ubicacion | null {
  try {
    const texto = window.sessionStorage.getItem(CLAVE)
    if (!texto) return null
    const { lat, lon } = JSON.parse(texto) as Partial<Ubicacion>
    return typeof lat === 'number' && typeof lon === 'number' ? { lat, lon } : null
  } catch {
    return null
  }
}

/**
 * Dónde está quien mira Explorar (#288), para ordenar por cercanía.
 *
 * Se pide al entrar y no se guarda en ningún servidor: viaja con el pedido de la lista y
 * ahí termina. Se recuerda lo que contestó el navegador mientras dure la pestaña, así
 * volver de una ficha no vuelve a pedirla ni reordena la lista.
 *
 * `esperando` es verdadero solo cuando el permiso ya estaba dado: la respuesta llega en
 * un instante y vale la pena esperarla para no dibujar la lista en un orden y reacomodarla
 * enseguida. Con el cartel del navegador en pantalla no se espera: la lista no puede
 * quedar tapada hasta que la persona decida.
 *
 * La espera tiene un tope propio (#304): si el navegador no contesta —la app se abre con la
 * pantalla en segundo plano, o el GPS está en frío— la lista no puede quedar sin pedirse.
 * Pasado el tope sale por relevancia, y si la ubicación llega después, se reordena.
 *
 * Negarse, o un navegador sin ubicación, es una respuesta válida: la lista sale por
 * relevancia, como antes.
 *
 * El mapa (#395) la comparte: si Explorar ya la tenía, la reusa sin pedirla otra vez. Además
 * devuelve `pedirAhora`, que el botón de ubicación del mapa llama al tocarse para recibir la
 * posición exacta con su precisión (el valor de `ubicacion` va redondeado a unos 100 m).
 */
export function useUbicacion(): {
  ubicacion: Ubicacion | null
  esperando: boolean
  pedirAhora: () => Promise<PosicionExacta>
} {
  const [ubicacion, setUbicacion] = useState<Ubicacion | null>(guardada)
  const [esperando, setEsperando] = useState(false)

  useEffect(() => {
    if (ubicacion || !navigator.geolocation) return
    let vigente = true
    let tope: ReturnType<typeof setTimeout> | undefined

    function pedir() {
      navigator.geolocation.getCurrentPosition(
        ({ coords }) => {
          if (!vigente) return
          clearTimeout(tope)
          const encontrada = { lat: redondeada(coords.latitude), lon: redondeada(coords.longitude) }
          recordar(encontrada)
          setUbicacion(encontrada)
          setEsperando(false)
        },
        () => {
          clearTimeout(tope)
          if (vigente) setEsperando(false)
        },
        { timeout: 8_000, maximumAge: 5 * 60_000 },
      )
    }

    const permisos = navigator.permissions?.query({ name: 'geolocation' })
    if (!permisos) {
      pedir()
    } else {
      permisos
        .then((estado) => {
          if (!vigente || estado.state === 'denied') return
          if (estado.state === 'granted') {
            setEsperando(true)
            tope = setTimeout(() => setEsperando(false), TOPE_DE_ESPERA_MS)
          }
          pedir()
        })
        .catch(() => {
          if (vigente) pedir()
        })
    }

    return () => {
      vigente = false
      clearTimeout(tope)
    }
    // Solo al entrar: si la ubicación llega, el efecto no tiene nada más que hacer.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const pedirAhora = useCallback(
    () =>
      new Promise<PosicionExacta>((resolver, rechazar) => {
        if (!navigator.geolocation) {
          rechazar(new Error('Este navegador no tiene ubicación'))
          return
        }
        navigator.geolocation.getCurrentPosition(
          ({ coords }) => {
            const encontrada = { lat: redondeada(coords.latitude), lon: redondeada(coords.longitude) }
            recordar(encontrada)
            setUbicacion(encontrada)
            resolver({ lat: coords.latitude, lon: coords.longitude, precision: coords.accuracy })
          },
          rechazar,
          { enableHighAccuracy: true, timeout: 10_000, maximumAge: 60_000 },
        )
      }),
    [],
  )

  return { ubicacion, esperando, pedirAhora }
}
