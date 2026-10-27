import { useEffect, useId, useState } from 'react'
import { apiClient } from '../api/client'
import type { BurgerJoint, PageResponse } from '../types'
import { BotonSugerirUnLocal } from './SugerirUnLocal'

/** Cuánto se espera después de la última letra antes de buscar. */
const ESPERA_EN_MS = 300

/** Cuántos locales se muestran: la lista es para elegir, no para recorrer. */
const MAXIMO_DE_RESULTADOS = 6

/**
 * Elegir el local que se va a reseñar, buscándolo en nuestra base (#337).
 *
 * Busca por nombre y por barrio ("mostaza belgrano", #335) y también entre las sucursales
 * de cadenas, que en Explorar no salen pero se reseñan. No consulta a Google.
 *
 * "Sugerí un local" aparece solo cuando se buscó y no hubo resultados: con resultados a
 * la vista, ofrecerlo siempre invitaría a sugerir uno que ya está.
 */
export function BuscadorDeLocal({ onElegir }: { onElegir: (local: BurgerJoint) => void }) {
  const idDelCampo = useId()
  const [texto, setTexto] = useState('')
  const [resultados, setResultados] = useState<BurgerJoint[]>([])
  // Lo último que se buscó de verdad, para no mostrar "no encontramos" mientras se escribe.
  const [buscado, setBuscado] = useState('')
  const [buscando, setBuscando] = useState(false)
  const [fallo, setFallo] = useState(false)

  useEffect(() => {
    const q = texto.trim()
    if (q.length < 2) return
    let vigente = true
    const espera = setTimeout(() => {
      setBuscando(true)
      apiClient
        .get<PageResponse<BurgerJoint>>('/burger-joints', {
          params: { q, resenia: true, orden: 'relevantes', size: MAXIMO_DE_RESULTADOS },
        })
        .then(({ data }) => {
          if (!vigente) return
          setResultados(data.content)
          setBuscado(q)
          setFallo(false)
        })
        .catch(() => {
          if (vigente) setFallo(true)
        })
        .finally(() => {
          if (vigente) setBuscando(false)
        })
    }, ESPERA_EN_MS)
    return () => {
      vigente = false
      clearTimeout(espera)
    }
  }, [texto])

  const hayTexto = texto.trim().length >= 2
  const sinResultados = hayTexto && !buscando && !fallo && buscado === texto.trim() && resultados.length === 0

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={idDelCampo} className="text-sm font-semibold">
        ¿Dónde comiste?
      </label>
      <input
        id={idDelCampo}
        type="search"
        value={texto}
        onChange={(e) => {
          setTexto(e.target.value)
          // Vaciado el campo no queda ninguna búsqueda en vuelo que apague el "Buscando…".
          if (!e.target.value.trim()) {
            setBuscando(false)
            setResultados([])
          }
        }}
        placeholder="Nombre del local (y barrio, si querés)"
        autoComplete="off"
        enterKeyHint="search"
        className="input input-bordered h-12 w-full focus:border-primary"
      />

      <p role="status" className={buscando ? 'text-sm text-base-content/70' : 'sr-only'}>
        {buscando ? 'Buscando…' : ''}
      </p>

      {fallo && (
        <p role="alert" className="text-sm text-error">
          No pudimos buscar. Probá de nuevo en un rato.
        </p>
      )}

      {hayTexto && resultados.length > 0 && !fallo && (
        <ul className="flex flex-col overflow-hidden rounded-field border border-base-300">
          {resultados.map((local) => (
            <li key={local.id} className="border-b border-base-300 last:border-b-0">
              <button
                type="button"
                onClick={() => onElegir(local)}
                className="flex min-h-14 w-full flex-col items-start justify-center px-3 py-2 text-left hover:bg-base-200 focus-visible:-outline-offset-2"
              >
                <span className="line-clamp-2 w-full font-semibold">{local.name}</span>
                {local.area && <span className="w-full truncate text-sm text-base-content/70">{local.area}</span>}
              </button>
            </li>
          ))}
        </ul>
      )}


      {sinResultados && (
        <div className="flex flex-col items-start gap-2 rounded-field bg-base-200 p-3">
          <span className="text-sm">No encontramos “{texto.trim()}”. ¿Es un local que falta?</span>
          <BotonSugerirUnLocal />
        </div>
      )}
    </div>
  )
}
