import { Link, useParams, useSearchParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconArrowLeft, IconChevronRight } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { ScoreBadge } from '../components/ScoreBadge'
import { usePedido } from '../hooks/usePedido'
import { useTitulo } from '../hooks/useTitulo'
import type { BurgerJoint, SucursalesDeCadena } from '../types'
import { shortAddress } from '../utils/address'
import { isNotFound } from '../utils/errors'

/**
 * Todas las sucursales de una cadena, por barrio (#206).
 *
 * Las cadenas no salen en Explorar: quien busca una por su nombre recibe una tarjeta, y
 * la tarjeta trae acá. Cada sucursal lleva a su ficha de siempre, con su dirección, su
 * horario y sus reseñas, porque la gente opina de la sucursal a la que fue.
 *
 * Si en Explorar había barrios elegidos, llegan en la dirección y sus sucursales van
 * primero: quien filtró por Palermo quiere ver el McDonald's de Palermo antes que los
 * otros noventa y seis.
 */
export function PaginaDeCadena() {
  const { marca = '' } = useParams()
  const [parametros] = useSearchParams()
  const elegidos = parametros.getAll('area').filter(Boolean)

  const pedido = usePedido(`cadena:${marca}`, () =>
    apiClient.get<SucursalesDeCadena>(`/cadenas/${marca}`).then(({ data }) => data),
  )
  const cadena = pedido.datos
  useTitulo(cadena?.nombre ?? null)

  const volver = (
    <Link to="/" aria-label="Volver a Explorar" className="btn btn-ghost btn-square -ml-2 flex-none">
      <IconArrowLeft size={18} />
    </Link>
  )

  if (isNotFound(pedido.error)) {
    return (
      <div className="flex flex-col items-start gap-3 p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
        <h1 className="titulo-pagina">No encontramos esa cadena</h1>
        <p className="text-sm text-base-content/70">Puede que el enlace esté mal escrito.</p>
        <Link to="/" className="btn btn-primary btn-sm">
          Ver todas las hamburgueserías
        </Link>
      </div>
    )
  }

  const enLosElegidos = cadena?.sucursales.filter((s) => s.area && elegidos.includes(s.area)) ?? []
  const resto = cadena?.sucursales.filter((s) => !s.area || !elegidos.includes(s.area)) ?? []

  return (
    <div className="flex flex-col gap-5 p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
      <div className="flex items-center gap-2">
        {volver}
        <div className="flex min-w-0 flex-col">
          <h1 className="titulo-pagina truncate">{cadena?.nombre ?? 'Cadena'}</h1>
          {cadena && (
            <p className="text-sm text-base-content/70">{cuantas(cadena.sucursales.length)}</p>
          )}
        </div>
      </div>

      {pedido.error ? (
        <LoadError error={pedido.error} onRetry={pedido.reintentar} />
      ) : !cadena ? (
        <p role="status" className="text-sm text-base-content/70">
          Cargando…
        </p>
      ) : (
        <>
          {enLosElegidos.length > 0 && (
            <Grupo titulo={`En ${enCastellano(elegidos)}`} cadena={cadena.nombre} sucursales={enLosElegidos} />
          )}
          {/* Las demás, cada barrio con su título. Vienen del servidor ordenadas por
              barrio, así que alcanza con cortar donde cambia. */}
          {porBarrio(resto).map(([barrio, sucursales]) => (
            <Grupo
              key={barrio}
              titulo={barrio}
              cadena={cadena.nombre}
              sucursales={sucursales}
              chico={enLosElegidos.length > 0}
            />
          ))}
        </>
      )}
    </div>
  )
}

/**
 * Las sucursales de un barrio, en una sola tarjeta con renglones: son hasta noventa y
 * siete, y una tarjeta por sucursal volvía a hacer de esto una lista de páginas.
 *
 * @param chico para los barrios que vienen después de los elegidos, que ya no son lo
 *              principal de la pantalla
 */
function Grupo({ titulo, cadena, sucursales, chico = false }: {
  titulo: string
  cadena: string
  sucursales: BurgerJoint[]
  chico?: boolean
}) {
  return (
    <section className="flex flex-col gap-2" aria-label={titulo}>
      <h2 className={`font-display font-bold ${chico ? 'text-base text-base-content/80' : 'text-lg'}`}>
        {titulo} <span className="font-sans text-sm font-normal text-base-content/70">({sucursales.length})</span>
      </h2>
      <ul className="list tarjeta">
        {sucursales.map((sucursal) => (
          <li key={sucursal.id}>
            <Link
              to={`/burger-joints/${sucursal.id}`}
              className="list-row items-center rounded-box hover:bg-base-200 focus-visible:-outline-offset-2"
            >
              {/* list-col-grow: sin esto el renglón estira la segunda columna, y la flecha
                  quedaba pegada al texto en vez de ir contra el borde. */}
              <span className="list-col-grow flex min-w-0 flex-col">
                <span className="truncate font-medium">{shortAddress(sucursal.address, null)}</span>
                {/* El nombre solo si dice algo más que la cadena: "McDonald's Abasto Patio
                    de Comidas" ayuda a ubicarla; "McDonald's" en cada renglón, no. */}
                {compacto(sucursal.name) !== compacto(cadena) && (
                  <span className="truncate text-sm text-base-content/70">{sucursal.name}</span>
                )}
              </span>
              <span className="flex items-center gap-2">
                {sucursal.averageScore ? <ScoreBadge score={sucursal.averageScore} size="sm" /> : null}
                <IconChevronRight size={16} className="text-base-content/70" />
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}

/** Sin mayúsculas, espacios ni signos: "MC DONALDS" y "McDonald's" son lo mismo. */
function compacto(texto: string) {
  return texto.normalize('NFD').toLowerCase().replace(/[^a-z0-9]/g, '')
}

/** Los renglones agrupados por barrio, en el orden en que vinieron. */
function porBarrio(sucursales: BurgerJoint[]): [string, BurgerJoint[]][] {
  const grupos = new Map<string, BurgerJoint[]>()
  for (const sucursal of sucursales) {
    const barrio = sucursal.area ?? 'Sin barrio'
    grupos.set(barrio, [...(grupos.get(barrio) ?? []), sucursal])
  }
  return [...grupos.entries()]
}

/** "Palermo, Belgrano y Núñez". */
function enCastellano(barrios: string[]) {
  if (barrios.length === 1) return barrios[0]
  return `${barrios.slice(0, -1).join(', ')} y ${barrios[barrios.length - 1]}`
}

/** "1 sucursal", "97 sucursales". */
function cuantas(n: number) {
  return n === 1 ? '1 sucursal' : `${n.toLocaleString('es-AR')} sucursales`
}
