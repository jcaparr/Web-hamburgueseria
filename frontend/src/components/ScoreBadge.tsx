import { nota } from '../utils/numeros'

interface Props {
  score: number
  size?: 'sm' | 'md'
  /**
   * Sin el fondo amarillo, solo la hamburguesita y el número. Para las listas largas:
   * un sello por fila, todos en columna, es lo que más se ve de la pantalla.
   */
  plain?: boolean
}

export function ScoreBadge({ score, size = 'md', plain = false }: Props) {
  const small = size === 'sm'
  const padding = small ? '4px 10px 4px 6px' : '6px 12px 6px 8px'

  return (
    <div
      className={`relative inline-flex w-fit items-center rounded-lg ${plain ? '' : 'bg-secondary'}`}
      style={{ gap: small ? 6 : 8, padding: plain ? 0 : padding }}
    >
      <div className="flex flex-col" style={{ gap: 1.5 }}>
        <div className="bg-neutral" style={{ width: small ? 16 : 22, height: small ? 5 : 6, borderRadius: '3px 3px 1px 1px' }} />
        <div className="bg-primary" style={{ width: small ? 16 : 22, height: small ? 4 : 5, borderRadius: 2 }} />
        <div className="bg-neutral" style={{ width: small ? 16 : 22, height: small ? 5 : 6, borderRadius: '1px 1px 3px 3px' }} />
      </div>
      <span
        className={`font-display font-bold tabular-nums ${plain ? 'text-base-content' : 'text-secondary-content'}`}
        style={{ fontSize: small ? 15 : 18 }}
      >
        <span className="sr-only">Nota </span>
        {nota(score)}
      </span>
    </div>
  )
}
