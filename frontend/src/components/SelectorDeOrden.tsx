import { useEffect, useRef, useState } from 'react'
import { IconChevronRight } from './icons'

/**
 * Cómo se ordena la lista de Explorar, en una pastilla igual a la de barrios (#207).
 *
 * Un desplegable y no un select común. El select servía, pero en el teléfono va en 16 px
 * —con menos, iOS hace zoom al tocarlo— y al lado de la pastilla de barrios, en 13, se
 * veía de otra familia. Tampoco una ventana como la de barrios: para tres opciones es
 * demasiado.
 *
 * Con details y no con el atributo popover, porque el popover necesita posicionamiento
 * por ancla para quedar debajo de la pastilla, y Safari y Firefox todavía no lo tienen.
 * Lo que details no trae —cerrarse al tocar afuera o con Escape— va a mano.
 */
export function SelectorDeOrden<T extends string>({
  opciones,
  valor,
  onCambiar,
}: {
  opciones: readonly { valor: T; texto: string }[]
  valor: T
  onCambiar: (valor: T) => void
}) {
  const desplegable = useRef<HTMLDetailsElement>(null)
  const [abierto, setAbierto] = useState(false)
  const actual = opciones.find((opcion) => opcion.valor === valor) ?? opciones[0]

  useEffect(() => {
    if (!abierto) return
    function cerrarSiEsAfuera(evento: PointerEvent) {
      if (!desplegable.current?.contains(evento.target as Node)) cerrar()
    }
    function cerrarConEscape(evento: KeyboardEvent) {
      if (evento.key !== 'Escape') return
      cerrar()
      desplegable.current?.querySelector('summary')?.focus()
    }
    document.addEventListener('pointerdown', cerrarSiEsAfuera)
    document.addEventListener('keydown', cerrarConEscape)
    return () => {
      document.removeEventListener('pointerdown', cerrarSiEsAfuera)
      document.removeEventListener('keydown', cerrarConEscape)
    }
  }, [abierto])

  function cerrar() {
    if (desplegable.current) desplegable.current.open = false
  }

  return (
    <details
      ref={desplegable}
      className="dropdown dropdown-end"
      onToggle={(evento) => setAbierto(evento.currentTarget.open)}
    >
      <summary className="btn btn-sm list-none rounded-full border-0 bg-base-100 shadow-[var(--sombra-tarjeta)] [&::-webkit-details-marker]:hidden">
        <span className="sr-only">Ordenar por: </span>
        {actual.texto}
        <IconChevronRight size={14} className={`flex-none transition-transform ${abierto ? '-rotate-90' : 'rotate-90'}`} />
      </summary>
      <ul className="menu dropdown-content z-20 mt-2 w-52 rounded-box bg-base-100 p-2 shadow-[var(--sombra-alzada)]">
        {opciones.map((opcion) => (
          <li key={opcion.valor}>
            <button
              type="button"
              aria-pressed={opcion.valor === valor}
              className={opcion.valor === valor ? 'menu-active' : ''}
              onClick={() => {
                onCambiar(opcion.valor)
                cerrar()
              }}
            >
              {opcion.texto}
            </button>
          </li>
        ))}
      </ul>
    </details>
  )
}
