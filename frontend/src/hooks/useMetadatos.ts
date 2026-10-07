import { useEffect } from 'react'

/**
 * Lo que una pantalla les dice a los buscadores, además del título (ver useTitulo).
 *
 * Es una app de una sola página: el index.html es el mismo para todas las direcciones,
 * y lo propio de cada pantalla se pone al entrar y se saca al salir. Google ejecuta la
 * página antes de leerla, así que ve lo que queda puesto.
 */

/** La descripción que trae el index.html, para volver a ella al salir de una pantalla. */
const DESCRIPCION_DEL_SITIO =
  document.querySelector<HTMLMetaElement>('meta[name="description"]')?.content ?? ''

/**
 * La descripción de la pantalla: el texto que Google muestra debajo del título en los
 * resultados. Sin ella usa la del sitio, igual para todas.
 */
export function useDescripcion(texto?: string | null) {
  useEffect(() => {
    const meta = document.querySelector<HTMLMetaElement>('meta[name="description"]')
    if (!meta || !texto) return
    meta.content = texto
    return () => {
      meta.content = DESCRIPCION_DEL_SITIO
    }
  }, [texto])
}

/**
 * Que los buscadores no guarden esta pantalla.
 *
 * Para las que dicen "no existe": el servidor contesta 200 a cualquier dirección,
 * porque las rutas las resuelve la app, y sin esto Google podía guardar como página
 * real a un enlace mal escrito.
 */
export function useNoIndexar(activo = true) {
  useEffect(() => {
    if (!activo) return
    const meta = document.createElement('meta')
    meta.name = 'robots'
    meta.content = 'noindex'
    document.head.appendChild(meta)
    return () => meta.remove()
  }, [activo])
}

/**
 * Datos estructurados (schema.org en JSON-LD) de la pantalla: lo que deja a Google
 * mostrar, por ejemplo, la nota de un local con estrellas en los resultados.
 *
 * Va con textContent y no armado como HTML: un nombre de local con "</script>" adentro
 * queda como texto y no puede cerrar la etiqueta.
 */
export function useDatosEstructurados(datos: object | null) {
  const json = datos ? JSON.stringify(datos) : null
  useEffect(() => {
    if (!json) return
    const script = document.createElement('script')
    script.type = 'application/ld+json'
    script.textContent = json
    document.head.appendChild(script)
    return () => script.remove()
  }, [json])
}
