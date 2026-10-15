import { FilaDePersona } from './FilaDePersona'
import { LoadError } from './LoadError'
import { estadoDeLaBusqueda, type BusquedaDeGente } from '../hooks/useBuscarGente'

/**
 * La lista de gente que encontró una búsqueda, y el renglón que dice cómo va. Quien la
 * monta y desmonta según haya algo escrito pasa conEstado={false} y deja el renglón
 * siempre montado: una región viva que entra al DOM ya con texto casi nunca se anuncia.
 */
export function ResultadosDeGente({
  busqueda,
  conEstado = true,
}: {
  busqueda: BusquedaDeGente
  conEstado?: boolean
}) {
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

      {conEstado && (
        // Un solo renglón que dice cómo va la búsqueda, y que el lector de pantalla
        // anuncia: antes los resultados aparecían y desaparecían sin que se dijera.
        <p role="status" className="text-sm text-base-content/70">
          {estadoDeLaBusqueda(busqueda)}
        </p>
      )}
    </>
  )
}
