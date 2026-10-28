import { useState } from 'react'
import { IconChevronRight } from './icons'
import { diaDeLaVisitaAparte, fechaYHora } from '../utils/fechaYHora'

/**
 * Cuándo se publicó la reseña y, si fue otro día, cuándo se fue (#368).
 *
 * La fecha de publicación va siempre a la vista. El día de la visita queda detrás de un
 * toque en esa fecha, para que la tarjeta no cargue un dato más en cada reseña: solo la
 * fecha que tiene algo para mostrar se vuelve botón, con su flechita, y las demás
 * quedan como texto.
 *
 * Es un botón, así que no va dentro de un enlace: las listas donde toda la tarjeta lleva
 * a la reseña muestran solo la fecha, y el día de la visita se abre ahí.
 */
export function FechaDeResenia({
  createdAt,
  visitDate,
  editada = false,
}: {
  createdAt: string
  visitDate: string | null
  editada?: boolean
}) {
  const [abierta, setAbierta] = useState(false)
  const fue = diaDeLaVisitaAparte({ createdAt, visitDate })
  // La fecha sigue siendo la de cuando se escribió: "(editada)" solo avisa que lo que se
  // está leyendo ya no es lo de ese día.
  const fecha = `${fechaYHora(createdAt)}${editada ? ' (editada)' : ''}`

  if (!fue) return <span className="flex-none">{fecha}</span>

  return (
    <>
      <button
        type="button"
        onClick={() => setAbierta((v) => !v)}
        aria-expanded={abierta}
        className="inline-flex flex-none cursor-pointer items-center gap-0.5 py-1 underline decoration-dotted underline-offset-2"
      >
        {fecha}
        <IconChevronRight size={11} className={`transition-transform ${abierta ? 'rotate-90' : ''}`} />
        <span className="sr-only">: ver el día en que fue</span>
      </button>
      {abierta && <span className="flex-none">{fue}</span>}
    </>
  )
}
