import { useState } from 'react'

/**
 * La foto de una reseña, que se esconde sola si no está.
 *
 * Un archivo puede faltar: una limpieza de disco, una restauración a medias, una
 * reseña vieja de antes de que las fotos se guardaran acá. Sin esto el navegador
 * dibuja su ícono de imagen rota con el texto alternativo al lado, que se ve mucho
 * peor que una tarjeta sin foto.
 */
export function FotoDeResenia({
  src,
  autorUsername,
  className,
}: {
  src: string
  autorUsername: string
  className?: string
}) {
  const [fallo, setFallo] = useState(false)

  if (fallo) return null

  return (
    <img
      src={src}
      alt={`La hamburguesa que reseñó @${autorUsername}`}
      onError={() => setFallo(true)}
      // Perezosa porque el feed baja de a veinte: cargar veinte fotos que nadie llegó a
      // ver todavía es gastar datos del teléfono de otro.
      loading="lazy"
      className={className ?? 'aspect-square w-full bg-base-200 object-cover'}
    />
  )
}
