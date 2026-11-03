import { useState } from 'react'
import type { Rating } from '../types'
import { armarTarjetaDeHistoria } from '../utils/tarjetaDeHistoria'
import { compartirImagen } from '../utils/compartirImagen'
import { IconShare } from './icons'

/**
 * «Compartir en historias» (#278): arma en el teléfono la imagen vertical de la reseña y
 * abre el menú de compartir con ella, para elegir Instagram. Donde el navegador no puede
 * compartir archivos (escritorio), la descarga.
 */
export function BotonCompartirEnHistorias({
  resenia,
  nombreDelLocal,
  barrio,
}: {
  resenia: Rating
  nombreDelLocal: string
  barrio: string | null
}) {
  const [armando, setArmando] = useState(false)
  const [aviso, setAviso] = useState<string | null>(null)

  async function compartir() {
    setArmando(true)
    setAviso(null)
    try {
      const imagen = await armarTarjetaDeHistoria({
        nombreDelLocal,
        barrio,
        puntaje: resenia.score,
        comentario: resenia.comment,
        username: resenia.username,
        foto: resenia.fotos[0] ?? null,
      })
      setAviso(await compartirImagen(imagen, 'burgometro-resenia.png'))
    } catch {
      setAviso('No pudimos armar la imagen. Probá de nuevo.')
    } finally {
      setArmando(false)
    }
  }

  return (
    <div className="flex flex-col items-start gap-1">
      <button type="button" onClick={compartir} disabled={armando} className="btn btn-outline btn-sm min-h-11 gap-2">
        <IconShare size={16} />
        {armando ? 'Armando la imagen…' : 'Compartir reseña'}
      </button>
      {aviso && (
        <p role="status" className="text-xs text-base-content/70">
          {aviso}
        </p>
      )}
    </div>
  )
}
