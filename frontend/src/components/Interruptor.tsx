import type { ReactNode } from 'react'

/**
 * Un interruptor con su texto al lado, que es como se ven todas las opciones de sí o no.
 *
 * El texto va adentro de la etiqueta para que tocarlo también lo cambie: en el celular,
 * el interruptor solo es un blanco chico.
 */
export function Interruptor({
  activo,
  onCambiar,
  children,
  className = '',
}: {
  activo: boolean
  onCambiar: (activo: boolean) => void
  children: ReactNode
  className?: string
}) {
  return (
    <label className={`flex cursor-pointer items-center gap-3 text-sm ${className}`}>
      <input
        type="checkbox"
        className="toggle toggle-sm toggle-secondary shrink-0"
        checked={activo}
        onChange={(e) => onCambiar(e.target.checked)}
      />
      <span>{children}</span>
    </label>
  )
}
