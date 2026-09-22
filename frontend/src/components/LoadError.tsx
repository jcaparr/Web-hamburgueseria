import { loadErrorMessage } from '../utils/errors'

/**
 * Lo que ve una pantalla cuando no pudo cargar, en lugar de su estado vacío.
 * El botón importa: el caso típico es el backend reiniciándose, que dura segundos.
 */
export function LoadError({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  return (
    <div role="alert" className="flex flex-col items-start gap-3 rounded-box bg-base-100 p-4 ring-1 ring-inset ring-error/30">
      <p className="text-sm text-base-content/80">{loadErrorMessage(error)}</p>
      <button type="button" className="btn btn-sm btn-outline" onClick={onRetry}>
        Reintentar
      </button>
    </div>
  )
}
