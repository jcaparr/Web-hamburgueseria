/**
 * Abre el menú de compartir del teléfono con la imagen, para elegir Instagram. Donde el
 * navegador no puede compartir archivos (escritorio), la descarga.
 *
 * Devuelve el aviso que hay que mostrar, o null si no hace falta ninguno. Lo usan la
 * historia de una reseña (#278) y "Tu mes en Burgómetro" (#308).
 */
export async function compartirImagen(imagen: Blob, nombreDelArchivo: string): Promise<string | null> {
  const archivo = new File([imagen], nombreDelArchivo, { type: 'image/png' })

  if (navigator.canShare?.({ files: [archivo] })) {
    try {
      await navigator.share({ files: [archivo] })
    } catch {
      // Cerrar el menú de compartir también llega acá, y no es un error.
    }
    return null
  }

  const enlace = URL.createObjectURL(imagen)
  const descarga = document.createElement('a')
  descarga.href = enlace
  descarga.download = archivo.name
  descarga.click()
  URL.revokeObjectURL(enlace)
  return 'Se descargó la imagen. Subila a tu historia desde la galería.'
}
