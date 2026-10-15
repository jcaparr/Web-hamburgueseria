import { FilaDePersona } from './FilaDePersona'
import { LoadError } from './LoadError'
import { MINIMO, type BusquedaDeGente } from '../hooks/useBuscarGente'

/** La lista de gente que encontró una búsqueda, y el renglón que dice cómo va. */
export function ResultadosDeGente({ busqueda }: { busqueda: BusquedaDeGente }) {
  const { texto, resultados, buscando, error, reintentar, cambioDeSeguimiento } = busqueda

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

      {/* Un solo renglón que dice cómo va la búsqueda, y que el lector de pantalla
          anuncia: antes los resultados aparecían y desaparecían sin que se dijera. */}
      <p role="status" className="text-sm text-base-content/70">
        {error
          ? ''
          : texto.length < MINIMO
            ? `Escribí al menos ${MINIMO} letras del nombre de usuario que buscás.`
            : buscando
              ? 'Buscando…'
              : resultados.length === 0
                ? 'No hay nadie con ese nombre.'
                : resultados.length === 1
                  ? '1 persona'
                  : `${resultados.length} personas`}
      </p>
    </>
  )
}
