import { useId, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { IconChevronRight } from './icons'

/**
 * Una sección de página con su título y, si hace falta, un "Ver todas" a la derecha.
 *
 * Los títulos eran de 14 px, del mismo tamaño que el texto de abajo: las secciones del
 * perfil se leían como un solo bloque largo. Van del tamaño de los de la ficha.
 */
export function Seccion({
  titulo,
  bajada,
  verTodas,
  textoDeVerTodas = 'Ver todas',
  children,
}: {
  titulo: string
  /** Una línea debajo del título, para decir para qué está la sección. */
  bajada?: string
  verTodas?: string
  textoDeVerTodas?: string
  children: ReactNode
}) {
  const id = useId()
  return (
    <section className="flex flex-col gap-3" aria-labelledby={id}>
      <div className="flex items-center justify-between gap-3">
        <div className="flex min-w-0 flex-col gap-0.5">
          <h2 id={id} className="font-display text-lg font-bold">
            {titulo}
          </h2>
          {bajada && <p className="text-sm text-base-content/70">{bajada}</p>}
        </div>
        {verTodas && (
          <Link
            to={verTodas}
            className="-my-3 flex flex-none items-center gap-1 rounded-field py-3 text-sm font-semibold text-primary hover:underline"
          >
            {textoDeVerTodas}
            <IconChevronRight size={14} />
          </Link>
        )}
      </div>
      {children}
    </section>
  )
}

/**
 * Lo que va en una sección que todavía no tiene nada: qué falta y cómo conseguirlo.
 *
 * Es la misma tarjeta que dice "Todavía nadie la calificó" en la ficha, para que
 * "acá no hay nada" se vea igual en toda la app.
 */
export function AvisoVacio({
  children,
  accion,
}: {
  children: ReactNode
  accion?: { texto: string; a: string }
}) {
  return (
    <div className="tarjeta flex flex-col items-start gap-3 p-4">
      <p className="text-sm text-base-content/70">{children}</p>
      {accion && (
        <Link to={accion.a} className="btn btn-outline btn-sm">
          {accion.texto}
        </Link>
      )}
    </div>
  )
}
