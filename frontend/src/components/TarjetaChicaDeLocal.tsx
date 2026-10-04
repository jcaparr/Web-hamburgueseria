import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { JointPhoto } from './JointPhoto'
import { ScoreBadge } from './ScoreBadge'

/**
 * Una hamburguesería en chico: la foto, el nombre y la nota.
 *
 * Es la tarjeta de las filas que se deslizan de costado —más del barrio, tus reseñas,
 * tus guardadas—. Estaban hechas a mano en cada lugar, cada una con su alto de foto y
 * su forma de decir "sin calificaciones".
 *
 * @param nota    la que se muestra en el sello: el promedio del local, o la que le
 *                puso alguien en su reseña
 * @param detalle una línea chica debajo, como la fecha de la reseña
 */
export function TarjetaChicaDeLocal({
  id,
  nombre,
  foto,
  nota,
  detalle,
}: {
  id: number
  nombre: string
  foto: string | null
  nota: number | null
  detalle?: ReactNode
}) {
  return (
    <Link
      to={`/burger-joints/${id}`}
      className="flex h-full flex-col overflow-hidden rounded-box bg-base-100 ring-1 ring-inset ring-base-content/15 transition-shadow hover:shadow-md focus-visible:-outline-offset-2"
    >
      <JointPhoto src={foto} name={nombre} className="aspect-[4/3] w-full object-cover" />
      <div className="flex flex-1 flex-col gap-1.5 p-2.5">
        <span className="line-clamp-2 text-sm font-semibold leading-snug">{nombre}</span>
        <span className="mt-auto flex flex-wrap items-center gap-x-2 gap-y-1">
          {nota ? (
            <ScoreBadge score={nota} size="sm" />
          ) : (
            <span className="text-xs text-base-content/70">Sin calificaciones</span>
          )}
          {detalle && <span className="text-xs text-base-content/70">{detalle}</span>}
        </span>
      </div>
    </Link>
  )
}

/**
 * La fila que se desliza de costado, con las tarjetas que entran de a dos y media en
 * un teléfono: la que queda cortada es lo que avisa que hay más.
 *
 * En el teléfono llega hasta el borde de la pantalla, para que la tarjeta cortada se
 * vea cortada por el borde y no por el margen.
 */
export function FilaDeTarjetas({ children }: { children: ReactNode }) {
  // relative para que la fila recorte también lo que va posicionado adentro, como los
  // textos solo para lectores de pantalla: sin esto, los de las tarjetas que no entran
  // ensanchaban la página entera y el teléfono la mostraba achicada.
  return (
    <ul className="relative -mx-4 flex snap-x scroll-px-4 gap-3 overflow-x-auto px-4 pb-2 md:mx-0 md:scroll-px-0 md:px-0">
      {children}
    </ul>
  )
}

/** Un lugar en la fila, del ancho de una tarjeta chica. */
export function LugarEnLaFila({ children }: { children: ReactNode }) {
  return <li className="w-40 flex-none snap-start">{children}</li>
}
