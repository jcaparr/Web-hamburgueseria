import { useEffect, useState } from 'react'
import { IconCamera } from './icons'

/** Lo mismo que acepta el servidor. El WEBP queda afuera: el JDK no lo sabe leer. */
const ACEPTADOS = 'image/jpeg,image/png,image/gif'

/** El mismo tope que el servidor, para avisar acá y no después de subir 12 MB. */
const MAXIMO_MB = 12

/**
 * El mismo mínimo que exige el servidor.
 *
 * Se chequea también acá porque el navegador ya sabe las medidas apenas elige el
 * archivo: enterarse al tocar "publicar", después de escribir toda la reseña, sería
 * hacerle perder el texto por algo que se sabía desde el principio.
 */
const LADO_MINIMO = 600

/**
 * La foto de la reseña, que es obligatoria.
 *
 * Ocupa el lugar que ocupa —un recuadro grande y no un botoncito— porque sacar la foto
 * es parte de reseñar, no un extra. Vacío se ve como algo que falta completar; lleno,
 * como lo que se va a publicar.
 *
 * Muestra lo elegido antes de mandarlo: una foto que se sube a ciegas es una foto que
 * se sube al revés o de otra cosa, y recién se nota cuando ya está publicada.
 */
export function SelectorDeFoto({
  elegida,
  yaSubida,
  onElegir,
}: {
  elegida: File | null
  /** La que ya está guardada en la reseña, si hay. */
  yaSubida: string | null
  onElegir: (foto: File | null) => void
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
    if (!archivo) {
      onElegir(null)
      return
    }
    if (archivo.size > MAXIMO_MB * 1024 * 1024) {
      setError(`Esa foto pesa más de ${MAXIMO_MB} MB. Probá con otra.`)
      onElegir(null)
      return
    }

    // Las medidas se leen cargándola, así que la respuesta llega después. Mientras
    // tanto se la toma como buena: el servidor la vuelve a chequear igual.
    const url = URL.createObjectURL(archivo)
    const prueba = new Image()
    prueba.onload = () => {
      URL.revokeObjectURL(url)
      if (Math.min(prueba.naturalWidth, prueba.naturalHeight) < LADO_MINIMO) {
        setError(
          `Esa foto es muy chica (${prueba.naturalWidth}×${prueba.naturalHeight}) y se ` +
            `vería borrosa. Necesita al menos ${LADO_MINIMO} px de lado.`,
        )
        onElegir(null)
      }
    }
    prueba.onerror = () => URL.revokeObjectURL(url)
    prueba.src = url

    onElegir(archivo)
  }

  const mostrando = vistaPrevia ?? yaSubida

  return (
    <div className="flex flex-col gap-2">
      <label className="group relative block cursor-pointer overflow-hidden rounded-box">
        {mostrando ? (
          <>
            <img
              src={mostrando}
              alt="La foto de tu reseña"
              className="aspect-square w-full bg-base-200 object-cover"
            />
            <span className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/70 to-transparent px-3 py-2 text-xs font-semibold text-white">
              Tocá para cambiarla
            </span>
          </>
        ) : (
          <span className="flex aspect-square w-full flex-col items-center justify-center gap-2 bg-base-200 text-base-content/70 ring-1 ring-inset ring-base-content/10 transition-colors group-hover:bg-base-300">
            <IconCamera size={34} />
            <span className="text-sm font-semibold">Agregá una foto</span>
            <span className="text-xs">Sin foto no se publica</span>
          </span>
        )}
        <input
          type="file"
          accept={ACEPTADOS}
          onChange={(e) => elegir(e.target.files?.[0] ?? null)}
          className="sr-only"
        />
      </label>

      {error && <p className="text-xs text-error">{error}</p>}
    </div>
  )
}
