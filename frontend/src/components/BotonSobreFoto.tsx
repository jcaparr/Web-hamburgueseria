import type { ButtonHTMLAttributes } from 'react'

/**
 * Un botón redondo que va encima de una foto: volver, compartir, guardar.
 *
 * El fondo crema casi opaco es lo que lo hace legible sobre cualquier foto, clara u
 * oscura. Está en la portada de la ficha y en las tarjetas de Explorar, y tienen que
 * verse iguales para que el corazón de una sea reconocible como el de la otra.
 */
export function BotonSobreFoto({ className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      type="button"
      {...props}
      className={`btn btn-circle btn-sm h-11 w-11 border-0 bg-base-100/90 text-base-content shadow-md backdrop-blur-sm hover:bg-base-100 ${className}`}
    />
  )
}
