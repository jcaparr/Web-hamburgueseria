import type { ReactNode } from 'react'

/**
 * Un interruptor con su texto al lado, que es como se ven todas las opciones de sí o no.
 *
 * El texto va adentro de la etiqueta para que tocarlo también lo cambie: en el celular,
 * el interruptor solo es un blanco chico. Por lo mismo la fila mide 44 px de alto
 * aunque la letra sea chica.
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
    <label className={`flex min-h-11 cursor-pointer items-center gap-3 text-sm ${className}`}>
      <input
        type="checkbox"
        // Del color del texto y no amarillo: el amarillo es el de las notas, y un
        // interruptor amarillo al lado de un sello de nota se leía como parte de él.
        className="toggle toggle-sm shrink-0"
        checked={activo}
        onChange={(e) => onCambiar(e.target.checked)}
      />
      <span>{children}</span>
    </label>
  )
}
