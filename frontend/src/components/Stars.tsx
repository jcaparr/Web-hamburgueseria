interface Props {
  value: number
  onChange?: (value: number) => void
  size?: number
}

export function Stars({ value, onChange, size = 20 }: Props) {
  const interactive = Boolean(onChange)

  // Todas con contorno, y las llenas además rellenas de mostaza. Sin el contorno, la
  // mostaza queda a 1,6:1 del fondo crema y las vacías, en el beige del borde, a 1,2:1:
  // una nota de 3 se veía casi igual que una de 5.
  const estilo = (n: number) => ({
    fontSize: size,
    lineHeight: 1,
    color: n <= value ? 'var(--color-secondary)' : 'transparent',
    WebkitTextStroke: `${size < 20 ? 1 : 1.5}px color-mix(in srgb, var(--color-base-content) 65%, var(--color-base-100))`,
  })

  if (!interactive) {
    return (
      <div className="flex gap-0.5" role="img" aria-label={`${value} de 5 estrellas`}>
        {[1, 2, 3, 4, 5].map((n) => (
          <span key={n} aria-hidden="true" style={estilo(n)}>
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
          style={estilo(n)}
        >
          ★
        </button>
      ))}
    </div>
  )
}
