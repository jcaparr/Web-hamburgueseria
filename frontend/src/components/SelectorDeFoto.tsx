import { useEffect, useState } from 'react'

/** Lo mismo que acepta el servidor. El WEBP queda afuera: el JDK no lo sabe leer. */
const ACEPTADOS = 'image/jpeg,image/png,image/gif'

/** El mismo tope que el servidor, para avisar acá y no después de subir 20 MB. */
const MAXIMO_MB = 8

/**
 * Elegir la foto que acompaña a una reseña.
 *
 * Muestra lo elegido antes de mandarlo: una foto que se sube a ciegas es una foto que
 * se sube al revés o de otra cosa, y recién se nota cuando ya está publicada.
 */
export function SelectorDeFoto({
  elegida,
  yaSubida,
  onElegir,
  onQuitar,
}: {
  elegida: File | null
  /** La que ya está guardada en la reseña, si hay. */
  yaSubida: string | null
  onElegir: (foto: File | null) => void
  onQuitar: () => void
}) {
  const [vistaPrevia, setVistaPrevia] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  // La URL del objeto ocupa memoria hasta que se la suelta, así que se revoca al
  // cambiar de foto y al desmontar.
  useEffect(() => {
    if (!elegida) {
      setVistaPrevia(null)
      return
    }
    const url = URL.createObjectURL(elegida)
    setVistaPrevia(url)
    return () => URL.revokeObjectURL(url)
  }, [elegida])

  function elegir(archivo: File | null) {
    setError(null)
    if (archivo && archivo.size > MAXIMO_MB * 1024 * 1024) {
      setError(`Esa foto pesa más de ${MAXIMO_MB} MB. Probá con otra.`)
      onElegir(null)
      return
    }
    onElegir(archivo)
  }

  const mostrando = vistaPrevia ?? yaSubida

  return (
    <div className="flex flex-col gap-2">
      {mostrando && (
        <div className="flex items-center gap-3">
          <img
            src={mostrando}
            alt="La foto de tu reseña"
            className="h-20 w-20 flex-none rounded-lg object-cover"
          />
          <button
            type="button"
            onClick={() => (vistaPrevia ? elegir(null) : onQuitar())}
            className="btn btn-ghost btn-xs"
          >
            {vistaPrevia ? 'Elegir otra' : 'Quitar foto'}
          </button>
        </div>
      )}

      <label className="flex cursor-pointer items-center gap-2 text-sm text-base-content/60">
        <input
          type="file"
          accept={ACEPTADOS}
          onChange={(e) => elegir(e.target.files?.[0] ?? null)}
          className="file-input file-input-bordered file-input-sm w-full"
        />
      </label>

      {error && <p className="text-xs text-error">{error}</p>}
    </div>
  )
}
