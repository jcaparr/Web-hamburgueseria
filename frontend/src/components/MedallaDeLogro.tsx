import { CHAPITA_CHICA, CHAPITA_GRANDE, POR_CODIGO } from '../utils/logros'

/** El candado de los secretos, en la grilla de 48 de los demás dibujos. */
const CANDADO = 'M14 22h20v16H14zM18 22v-5a6 6 0 0 1 12 0v5M24 29v4'

/**
 * La chapita dentada de un logro (#316): con sus colores si se ganó, y apagada con un
 * candado de color si todavía no. Los de serie (nivel, seguidores, barrios...) llevan un
 * chip con el número, que se saltea en las chicas.
 *
 * Las de 40 px o menos usan el borde con menos dientes y el dibujo simple, si lo hay, y el
 * trazo fino queda fino aunque se achique (`vector-effect`): a ese tamaño un trazo
 * escalado se vuelve una mancha.
 */
export function MedallaDeLogro({
  codigo,
  ganado,
  size = 56,
  sinNumero = false,
  oculto = false,
}: {
  codigo: string
  ganado: boolean
  size?: number
  sinNumero?: boolean
  /** Un secreto sin ganar: se dibuja un candado y no el ícono, que diría qué es. */
  oculto?: boolean
}) {
  const datos = POR_CODIGO[codigo]
  if (!datos) return null
  const chica = size <= 40
  const borde = chica ? CHAPITA_CHICA : CHAPITA_GRANDE
  const dibujo = oculto ? CANDADO : (chica && datos.iconoChico) || datos.icono

  const relleno = ganado ? datos.relleno : 'var(--color-base-300)'
  const aro = ganado ? datos.borde : 'color-mix(in oklab, var(--color-base-content) 30%, var(--color-base-300))'
  const tinta = ganado ? '#fffdf8' : 'color-mix(in oklab, var(--color-base-content) 70%, transparent)'

  const conNumero = !!datos.numero && size >= 52 && !sinNumero && !oculto
  const alto = size >= 80 ? 24 : 20
  const fondoDelChip = ganado ? datos.borde : 'var(--color-base-300)'
  const letraDelChip = ganado ? (datos.borde === '#b23a2e' ? '#f7efd8' : '#3a2a1e') : 'var(--color-base-content)'

  return (
    <span className="relative block flex-none" style={{ width: size, height: size }} aria-hidden>
      <svg width={size} height={size} viewBox="-3 -3 54 54" className="block overflow-visible">
        <path d={borde} style={{ fill: relleno, stroke: aro }} strokeWidth={2.5} strokeLinejoin="round" />
        <g transform="translate(9 9) scale(.625)" fill="none" style={{ stroke: tinta }} strokeLinecap="round" strokeLinejoin="round">
          <path d={dibujo} strokeWidth={chica ? 1.75 : 2.6} vectorEffect={chica ? 'non-scaling-stroke' : undefined} />
        </g>
      </svg>
      {conNumero && (
        <span
          className="absolute left-1/2 -translate-x-1/2 rounded-full px-1.5 text-center font-display font-bold tabular-nums shadow-[0_0_0_2px_var(--color-base-100)]"
          style={{
            bottom: -(alto / 2 - 2),
            height: alto,
            minWidth: alto,
            lineHeight: `${alto}px`,
            fontSize: size >= 80 ? 15 : 13,
            background: fondoDelChip,
            color: letraDelChip,
          }}
        >
          {datos.numero}
        </span>
      )}
    </span>
  )
}
