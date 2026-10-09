import { Link } from 'react-router-dom'
import type { Cadena } from '../types'
import { IconChevronRight } from './icons'
import { JointPhoto } from './JointPhoto'

/**
 * Una cadena entera en una tarjeta: el nombre y cuántas sucursales tiene (#206).
 *
 * Aparece en Explorar solo cuando alguien la busca por su nombre. Con cientos de
 * sucursales sueltas, McDonald's, Burger King y Hamburguesas Extremas ocupaban páginas
 * enteras de la lista; así son una tarjeta cada una, y la página de la cadena tiene el
 * resto.
 *
 * Apaisada y chica a propósito, distinta de la de un local: es un atajo a una lista,
 * no una hamburguesería para mirar.
 *
 * @param barrios los elegidos en Explorar. Viajan a la página de la cadena, que pone
 *                primero las sucursales de esos barrios.
 */
export function TarjetaDeCadena({ cadena, barrios }: { cadena: Cadena; barrios: string[] }) {
  const parametros = new URLSearchParams()
  barrios.forEach((barrio) => parametros.append('area', barrio))
  const consulta = parametros.toString()

  return (
    <Link
      to={`/cadenas/${cadena.marca}${consulta ? `?${consulta}` : ''}`}
      className="flex items-center gap-3 tarjeta p-1.5 pr-3 transition-shadow hover:tarjeta-alzada focus-visible:-outline-offset-2"
    >
      <JointPhoto
        src={cadena.fotoUrl}
        name={cadena.nombre}
        className="size-16 flex-none rounded-[calc(var(--radius-box)-0.25rem)] object-cover"
      />
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="line-clamp-1 font-display text-lg font-bold">{cadena.nombre}</span>
        <span className="text-sm text-base-content/70">
          {cuantas(cadena.sucursales)}
          {barrios.length === 1 ? ` en ${barrios[0]}` : barrios.length > 1 ? ' en esos barrios' : ''}
        </span>
      </span>
      <IconChevronRight size={18} className="flex-none text-base-content/70" />
    </Link>
  )
}

/** "1 sucursal", "97 sucursales". */
function cuantas(n: number) {
  return n === 1 ? '1 sucursal' : `${n.toLocaleString('es-AR')} sucursales`
}
