import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { PANEL_DEL_PERFIL, PESTANIAS, type Pestania } from '../utils/pestaniasDePerfil'
import { IconChevronRight } from './icons'

/**
 * Las pestañas de un perfil: las del propio y, si esa persona las muestra, las del de
 * otro (#264). Las mismas en los dos, para que se lean igual.
 *
 * Pestañas en vez de tres secciones apiladas: con las tres a la vista, el perfil era
 * una tira larga de tarjetas y cajas vacías, y lo de abajo no lo veía nadie.
 */
export function BarraDePestanias({
  actual,
  onElegir,
  etiqueta,
}: {
  actual: Pestania
  onElegir: (pestania: Pestania) => void
  etiqueta: string
}) {
  return (
    <div role="tablist" aria-label={etiqueta} className="flex border-b border-base-content/15">
      {PESTANIAS.map((p) => (
        <button
          key={p.id}
          type="button"
          role="tab"
          id={`pestania-${p.id}`}
          aria-selected={actual === p.id}
          aria-controls={PANEL_DEL_PERFIL}
          onClick={() => onElegir(p.id)}
          className={`-mb-px min-h-11 flex-1 cursor-pointer border-b-2 px-2 text-sm font-semibold transition-colors ${
            actual === p.id
              ? 'border-primary text-base-content'
              : 'border-transparent text-base-content/70 hover:text-base-content'
          }`}
        >
          {p.texto}
        </button>
      ))}
    </div>
  )
}

/** Las tarjetas de una pestaña: de a dos en el teléfono y de a tres en la compu. */
export function GrillaDeLocales({ children }: { children: ReactNode }) {
  return <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3">{children}</ul>
}

/** El enlace al final de una pestaña, hacia la lista entera. */
export function VerTodas({ a, children }: { a: string; children: ReactNode }) {
  return (
    <Link
      to={a}
      className="-my-3 flex w-fit items-center gap-1 self-end rounded-field py-3 text-sm font-semibold text-primary hover:underline"
    >
      {children}
      <IconChevronRight size={14} />
    </Link>
  )
}
