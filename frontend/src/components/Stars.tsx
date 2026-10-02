interface Props {
  value: number
  onChange?: (value: number) => void
  size?: number
}

export function Stars({ value, onChange, size = 20 }: Props) {
  const interactive = Boolean(onChange)

  const color = (n: number) =>
    n <= value ? 'var(--color-secondary)' : 'var(--color-base-300)'

  if (!interactive) {
    return (
      <div className="flex gap-0.5" role="img" aria-label={`${value} de 5 estrellas`}>
        {[1, 2, 3, 4, 5].map((n) => (
          <span key={n} aria-hidden="true" style={{ fontSize: size, lineHeight: 1, color: color(n) }}>
            ★
          </span>
        ))}
      </div>
    )
  }

  return (
    <div className="flex gap-1" role="radiogroup" aria-label="Puntaje">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          role="radio"
          aria-checked={n === value}
          aria-label={n === 1 ? '1 estrella' : `${n} estrellas`}
          onClick={() => onChange?.(n)}
          className="cursor-pointer rounded-field p-0.5 transition-transform focus-visible:outline-2 focus-visible:outline-primary active:scale-90 motion-reduce:transition-none"
          style={{ fontSize: size, lineHeight: 1, color: color(n) }}
        >
          ★
        </button>
      ))}
    </div>
  )
}
