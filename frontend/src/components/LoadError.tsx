import { loadErrorMessage } from '../utils/errors'
import { Personaje } from './Personaje'

/**
 * Lo que ve una pantalla cuando no pudo cargar, en lugar de su estado vacío.
 * El botón importa: el caso típico es el backend reiniciándose, que dura segundos.
 * Burgómetro confundido avisa que algo falló sin asustar (#365).
 */
export function LoadError({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  return (
    <div role="alert" className="flex items-center gap-4 rounded-box bg-base-100 p-4 ring-1 ring-inset ring-error/30">
      <Personaje pose="confundido" className="h-20 w-auto shrink-0" />
      <div className="flex flex-col items-start gap-3">
        <p className="text-sm text-base-content/80">{loadErrorMessage(error)}</p>
        <button type="button" className="btn btn-sm btn-outline" onClick={onRetry}>
          Reintentar
        </button>
      </div>
    </div>
  )
}
