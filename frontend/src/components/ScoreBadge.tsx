import { nota } from '../utils/numeros'

interface Props {
  score: number
  size?: 'sm' | 'md'
}

/**
 * La nota de un local en el sello amarillo, con un tablero chiquito al lado.
 *
 * El tablero es el del logo: un semicírculo que se llena hasta la nota, sobre cinco, y
 * una aguja que apunta ahí. Antes había un dibujo de hamburguesa que decía "esto es
 * una nota" pero no cuánto: un 2,0 y un 4,8 llevaban el mismo dibujo. Ahora el sello se
 * lee de un vistazo, antes que el número.
 *
 * El amarillo se queda: es como se reconoce una nota en toda la app.
 */
export function ScoreBadge({ score, size = 'md' }: Props) {
  const small = size === 'sm'

  // relative: el "Nota" oculto de abajo se ubica contra el sello, y así no se escapa de
  // las filas que se deslizan de costado.
  return (
    <div
      className="relative inline-flex w-fit items-center rounded-lg bg-secondary"
      style={{ gap: small ? 5 : 7, padding: small ? '4px 9px 4px 6px' : '6px 12px 6px 8px' }}
    >
      <TableroChico fraccion={score / 5} ancho={small ? 22 : 28} />
      <span
        className="font-display font-bold tabular-nums text-secondary-content"
        style={{ fontSize: small ? 15 : 18 }}
      >
        <span className="sr-only">Nota </span>
        {nota(score)}
        <span className="sr-only"> de 5</span>
      </span>
    </div>
  )
}

/**
 * El tablero del sello. Va en marrón sobre el amarillo, con la aguja de kétchup como
 * en el logo; el riel apagado deja ver cuánto falta para el cinco.
 *
 * pathLength=100 hace que el trazo se mida en por ciento del arco: llenar hasta la nota
 * es pedir ese por ciento de raya y el resto de hueco, sin calcular ningún ángulo.
 */
function TableroChico({ fraccion, ancho }: { fraccion: number; ancho: number }) {
  const lleno = Math.min(Math.max(fraccion, 0), 1) * 100
  const giro = -90 + lleno * 1.8
  return (
    <svg viewBox="0 0 24 14" width={ancho} height={(ancho * 14) / 24} aria-hidden="true" className="flex-none">
      <path d="M3 12 A9 9 0 0 1 21 12" pathLength={100} fill="none" strokeWidth={3.2} className="stroke-secondary-content/25" />
      <path
        d="M3 12 A9 9 0 0 1 21 12"
        pathLength={100}
        fill="none"
        strokeWidth={3.2}
        strokeDasharray={`${lleno} 100`}
        className="stroke-secondary-content"
      />
      <line
        x1="12"
        y1="12"
        x2="12"
        y2="4.6"
        strokeWidth={2.2}
        strokeLinecap="round"
        transform={`rotate(${giro} 12 12)`}
        className="stroke-ketchup"
      />
      <circle cx="12" cy="12" r="2" className="fill-secondary-content" />
    </svg>
  )
}
