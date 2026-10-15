import type { ButtonHTMLAttributes } from 'react'

/**
 * Un botón redondo que va encima de una foto: volver, compartir, guardar.
 *
 * El fondo crema casi opaco es lo que lo hace legible sobre cualquier foto, clara u
 * oscura. Está en la portada de la ficha y en las tarjetas de Explorar, y tienen que
 * verse iguales para que el corazón de una sea reconocible como el de la otra.
 *
 * Encendido (un local guardado) va en marrón con el corazón amarillo (#245), como la
 * opción elegida de la barra de arriba (7,4 : 1): con el corazón rojo sobre el mismo
 * círculo de siempre, guardado y sin guardar se distinguían por el color de un dibujo
 * de 20 px, y sobre una foto con rojos casi no se notaba. El aro amarillo es para el
 * tema oscuro, donde el círculo marrón casi no se distingue del crema oscurecido.
 */
export function BotonSobreFoto({
  encendido = false,
  className = '',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { encendido?: boolean }) {
  const colores = encendido
    ? 'bg-neutral text-secondary ring-2 ring-secondary hover:bg-neutral'
    : 'bg-base-100/90 text-base-content hover:bg-base-100'
  return (
    <button
      type="button"
      {...props}
      className={`btn btn-circle btn-sm h-11 w-11 border-0 shadow-md backdrop-blur-sm ${colores} ${className}`}
    />
  )
}
