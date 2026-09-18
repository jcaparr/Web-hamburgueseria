interface Props {
  value: number
  onChange?: (value: number) => void
  size?: number
}

export function Stars({ value, onChange, size = 20 }: Props) {
  const interactive = Boolean(onChange)

  return (
    <div className="flex gap-0.5" role={interactive ? 'radiogroup' : undefined} aria-label="Rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          disabled={!interactive}
          onClick={() => onChange?.(n)}
          aria-label={`${n} stars`}
          aria-pressed={n <= value}
          className={interactive ? 'cursor-pointer' : 'cursor-default'}
          style={{ fontSize: size, lineHeight: 1, color: n <= value ? '#f2b705' : '#e3d6b4' }}
        >
          ★
        </button>
      ))}
    </div>
  )
}
