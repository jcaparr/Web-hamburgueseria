import { useCallback, useEffect, useRef, useState } from 'react'

interface Estado<T> {
  /** De qué pedido es lo que llegó, con su intento: "ranking:nota#0". */
  pedido: string
  /** Lo mismo sin el intento, para saber si es del mismo recurso aunque se esté recargando. */
  clave: string
  datos?: T
  error?: unknown
  /** Lo último que llegó bien, de este pedido o de uno anterior. */
  ultimosDatos?: T
}

export interface Pedido<T> {
  /**
   * Lo último que llegó bien de este mismo pedido, aunque se lo esté volviendo a pedir:
   * al recargar, la pantalla no se vacía.
   */
  datos: T | undefined
  /** Lo último que llegó bien, aunque sea de un pedido anterior: un ranking que cambia de pestaña. */
  datosAnteriores: T | undefined
  /** El error del pedido de ahora, si falló. */
  error: unknown
  cargando: boolean
  /** Vuelve a pedir lo mismo: para el botón de reintentar, o después de cambiar algo. */
  reintentar: () => void
  /** Cambia lo que llegó sin volver a pedirlo: seguir a alguien, traer más de una lista. */
  actualizar: (cambio: (datos: T) => T) => void
}

/**
 * Pide algo al servidor, y lo vuelve a pedir cuando cambia la clave.
 *
 * Cada pantalla armaba a mano lo mismo —cargando, error, reintentar, y descartar la
 * respuesta lenta de un pedido que ya no importa— dentro de un efecto, y "cargando" se
 * prendía desde el efecto: un dibujo de más cada vez, que es lo que marcaba el linter en
 * seis pantallas (#109). Acá "cargando" se deduce: es que lo último que llegó no es de
 * lo que se está pidiendo.
 *
 * @param clave lo que identifica al pedido, como la ruta con sus parámetros: cuando
 *              cambia, se pide de nuevo. Null es no pedir nada.
 * @param pedir cómo pedirlo. Se usa el de la última vez que se dibujó la pantalla, así
 *              que puede leer lo que tenga a mano sin entrar en la clave.
 */
export function usePedido<T>(clave: string | null, pedir: () => Promise<T>): Pedido<T> {
  const [intento, setIntento] = useState(0)
  const [estado, setEstado] = useState<Estado<T> | null>(null)

  const pedirActual = useRef(pedir)
  useEffect(() => {
    pedirActual.current = pedir
  })

  const pedido = clave === null ? null : `${clave}#${intento}`

  useEffect(() => {
    if (clave === null || pedido === null) return
    // Si la clave cambia antes de que llegue la respuesta, la vieja se descarta: si no,
    // una respuesta lenta de la pestaña anterior pisaba a la de ahora.
    let vigente = true
    pedirActual.current().then(
      (datos) => {
        if (vigente) setEstado({ pedido, clave, datos, ultimosDatos: datos })
      },
      (error) => {
        // Si falla una recarga, lo que ya se veía se queda: el aviso de error va arriba
        // y la pantalla no se vacía.
        if (vigente) {
          setEstado((previo) => ({
            pedido,
            clave,
            error,
            datos: previo?.clave === clave ? previo.datos : undefined,
            ultimosDatos: previo?.ultimosDatos,
          }))
        }
      },
    )
    return () => {
      vigente = false
    }
  }, [clave, pedido])

  const reintentar = useCallback(() => setIntento((n) => n + 1), [])

  const actualizar = useCallback((cambio: (datos: T) => T) => {
    setEstado((previo) => {
      if (!previo || previo.datos === undefined) return previo
      const datos = cambio(previo.datos)
      return { ...previo, datos, ultimosDatos: datos }
    })
  }, [])

  const esDeEstaClave = estado !== null && estado.clave === clave
  return {
    datos: esDeEstaClave ? (estado.datos ?? undefined) : undefined,
    datosAnteriores: estado?.ultimosDatos,
    error: estado !== null && estado.pedido === pedido ? estado.error : undefined,
    cargando: pedido !== null && estado?.pedido !== pedido,
    reintentar,
    actualizar,
  }
}
