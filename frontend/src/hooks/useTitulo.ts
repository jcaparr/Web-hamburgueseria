import { useEffect } from 'react'

const SITIO = 'Hamburgueserías BA'

/**
 * El título de la pestaña del navegador para esta pantalla.
 *
 * Es lo primero que anuncia un lector de pantalla al cambiar de página, y lo que se ve
 * en la pestaña y en el historial. Antes todas decían "frontend".
 *
 * @param titulo lo propio de la pantalla; sin él queda solo el nombre del sitio
 */
export function useTitulo(titulo?: string | null) {
  useEffect(() => {
    document.title = titulo ? `${titulo} · ${SITIO}` : SITIO
  }, [titulo])
}
