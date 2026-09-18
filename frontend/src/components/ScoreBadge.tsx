interface Props {
  score: number
  size?: 'sm' | 'md'
}

export function ScoreBadge({ score, size = 'md' }: Props) {
  const small = size === 'sm'

  return (
    <div
      className="inline-flex w-fit items-center rounded-lg bg-secondary"
      style={{ gap: small ? 6 : 8, padding: small ? '4px 10px 4px 6px' : '6px 12px 6px 8px' }}
    >
      <div className="flex flex-col" style={{ gap: 1.5 }}>
        <div className="bg-neutral" style={{ width: small ? 16 : 22, height: small ? 5 : 6, borderRadius: '3px 3px 1px 1px' }} />
        <div className="bg-primary" style={{ width: small ? 16 : 22, height: small ? 4 : 5, borderRadius: 2 }} />
        <div className="bg-neutral" style={{ width: small ? 16 : 22, height: small ? 5 : 6, borderRadius: '1px 1px 3px 3px' }} />
      </div>
      <span className="font-display font-bold text-secondary-content" style={{ fontSize: small ? 14 : 18 }}>
        {score.toFixed(1)}
      </span>
    </div>
  )
}
