import { FilaDePersona } from './FilaDePersona'
import { LoadError } from './LoadError'
import type { BusquedaDeGente } from '../hooks/useBuscarGente'

/**
 * La lista de gente que encontró una búsqueda. El renglón que dice cómo va lo pone quien
 * la usa, siempre montado: una región viva que entra al DOM ya con texto casi nunca se
 * anuncia.
 */
export function ResultadosDeGente({ busqueda }: { busqueda: BusquedaDeGente }) {
  const { resultados, error, reintentar, cambioDeSeguimiento } = busqueda

  return (
    <>
      {error ? (
        <LoadError error={error} onRetry={reintentar} />
      ) : (
        <ul className="flex flex-col gap-2">
          {resultados.map((persona) => (
            <FilaDePersona
              key={persona.userId}
              persona={persona}
              onCambio={(loSigo) => cambioDeSeguimiento(persona.username, loSigo)}
            />
          ))}
        </ul>
      )}
    </>
  )
}
