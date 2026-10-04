import { useRef, type KeyboardEvent } from 'react'

interface Props {
  value: number
  onChange?: (value: number) => void
  size?: number
  /** Cómo se anuncia el grupo para elegir; conviene que diga lo mismo que el rótulo. */
  etiqueta?: string
}

export function Stars({ value, onChange, size = 20, etiqueta = 'Puntaje' }: Props) {
  const interactive = Boolean(onChange)
  const botones = useRef<(HTMLButtonElement | null)[]>([])

  // Todas con contorno, y las llenas además rellenas de mostaza. Sin el contorno, la
  // mostaza queda a 1,6:1 del fondo crema y las vacías, en el beige del borde, a 1,2:1:
  // una nota de 3 se veía casi igual que una de 5.
  const estilo = (n: number) => ({
    fontSize: size,
    lineHeight: 1,
    color: n <= value ? 'var(--color-secondary)' : 'transparent',
    WebkitTextStroke: `${size < 20 ? 1 : 1.5}px color-mix(in srgb, var(--color-base-content) 65%, var(--color-base-100))`,
  })

  // De solo lectura va en un span, que entra donde entra el texto: un div no puede ir
  // dentro de un párrafo, y "Tu puntaje ★★★" es un renglón de texto (#131).
  if (!interactive) {
    return (
      <span className="inline-flex gap-0.5" role="img" aria-label={`${value} de 5 estrellas`}>
        {[1, 2, 3, 4, 5].map((n) => (
          <span key={n} aria-hidden="true" style={estilo(n)}>
            ★
          </span>
        ))}
      </span>
    )
  }

  function elegir(n: number) {
    onChange?.(n)
    botones.current[n - 1]?.focus()
  }

  // Como cualquier grupo de opciones: se entra con Tab a la elegida y se cambia con
  // las flechas. Antes cada estrella era una parada del Tab, cinco seguidas.
  function conTeclado(e: KeyboardEvent<HTMLDivElement>) {
    const actual = value || 0
    const siguiente: Record<string, number> = {
      ArrowRight: Math.min(5, actual + 1),
      ArrowUp: Math.min(5, actual + 1),
      ArrowLeft: Math.max(1, actual - 1),
      ArrowDown: Math.max(1, actual - 1),
      Home: 1,
      End: 5,
    }
    if (!(e.key in siguiente)) return
    e.preventDefault()
    elegir(siguiente[e.key])
  }

  return (
    <div className="flex" role="radiogroup" aria-label={etiqueta} onKeyDown={conTeclado}>
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          ref={(boton) => {
            botones.current[n - 1] = boton
          }}
          type="button"
          role="radio"
          aria-checked={n === value}
          tabIndex={n === (value || 1) ? 0 : -1}
          aria-label={n === 1 ? '1 estrella' : `${n} estrellas`}
          onClick={() => elegir(n)}
          className="flex min-h-11 min-w-11 cursor-pointer items-center justify-center rounded-field transition-transform focus-visible:outline-2 focus-visible:outline-primary active:scale-90 motion-reduce:transition-none"
          style={estilo(n)}
        >
          ★
        </button>
      ))}
    </div>
  )
}
