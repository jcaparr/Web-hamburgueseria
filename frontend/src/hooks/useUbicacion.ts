import { useEffect, useState } from 'react'

export interface Ubicacion {
  lat: number
  lon: number
}

const CLAVE = 'ubicacion.actual'

/** Con tres decimales (unos 100 m) alcanza para ordenar por cercanía, y no mandamos más. */
function redondeada(n: number) {
  return Math.round(n * 1000) / 1000
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
 * Negarse, o un navegador sin ubicación, es una respuesta válida: la lista sale por
 * relevancia, como antes.
 */
export function useUbicacion(): { ubicacion: Ubicacion | null; esperando: boolean } {
  const [ubicacion, setUbicacion] = useState<Ubicacion | null>(guardada)
  const [esperando, setEsperando] = useState(false)

  useEffect(() => {
    if (ubicacion || !navigator.geolocation) return
    let vigente = true

    function pedir() {
      navigator.geolocation.getCurrentPosition(
        ({ coords }) => {
          if (!vigente) return
          const encontrada = { lat: redondeada(coords.latitude), lon: redondeada(coords.longitude) }
          try {
            window.sessionStorage.setItem(CLAVE, JSON.stringify(encontrada))
          } catch {
            // Sin almacenamiento se vuelve a pedir la próxima vez, nada más.
          }
          setUbicacion(encontrada)
          setEsperando(false)
        },
        () => {
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
          if (estado.state === 'granted') setEsperando(true)
          pedir()
        })
        .catch(() => {
          if (vigente) pedir()
        })
    }

    return () => {
      vigente = false
    }
    // Solo al entrar: si la ubicación llega, el efecto no tiene nada más que hacer.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return { ubicacion, esperando }
}
