import { nota } from '../utils/numeros'

interface Props {
  score: number
  size?: 'sm' | 'md'
}

export function ScoreBadge({ score, size = 'md' }: Props) {
  const small = size === 'sm'

  // relative: el "Nota" oculto de abajo se ubica contra el sello, y así no se escapa de
  // las filas que se deslizan de costado.
  return (
    <div
      className="relative inline-flex w-fit items-center rounded-lg bg-secondary"
      style={{ gap: small ? 6 : 8, padding: small ? '4px 10px 4px 6px' : '6px 12px 6px 8px' }}
    >
      <div className="flex flex-col" style={{ gap: 1.5 }}>
        <div className="bg-neutral" style={{ width: small ? 16 : 22, height: small ? 5 : 6, borderRadius: '3px 3px 1px 1px' }} />
        <div className="bg-primary" style={{ width: small ? 16 : 22, height: small ? 4 : 5, borderRadius: 2 }} />
        <div className="bg-neutral" style={{ width: small ? 16 : 22, height: small ? 5 : 6, borderRadius: '1px 1px 3px 3px' }} />
      </div>
      <span
        className="font-display font-bold tabular-nums text-secondary-content"
        style={{ fontSize: small ? 15 : 18 }}
      >
        <span className="sr-only">Nota </span>
        {nota(score)}
      </span>
    </div>
  )
}
