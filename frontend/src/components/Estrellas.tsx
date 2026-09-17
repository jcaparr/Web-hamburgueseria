interface Props {
  valor: number
  onChange?: (valor: number) => void
  size?: number
}

export function Estrellas({ valor, onChange, size = 20 }: Props) {
  const interactivo = Boolean(onChange)

  return (
    <div className="flex gap-0.5" role={interactivo ? 'radiogroup' : undefined} aria-label="Calificacion">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          disabled={!interactivo}
          onClick={() => onChange?.(n)}
          aria-label={`${n} estrellas`}
          aria-pressed={n <= valor}
          className={interactivo ? 'cursor-pointer' : 'cursor-default'}
          style={{ fontSize: size, lineHeight: 1, color: n <= valor ? '#f59e0b' : '#d4d4d8' }}
        >
          ★
        </button>
      ))}
    </div>
  )
}
