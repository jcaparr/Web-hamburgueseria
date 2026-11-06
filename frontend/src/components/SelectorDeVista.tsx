import { useEffect, useRef, type KeyboardEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { direccionDeLaVista, type Vista } from '../utils/vistaDeExplorar'
import { IconFeed, IconPin } from './icons'

const OPCIONES: { vista: Vista; nombre: string; Icono: typeof IconFeed }[] = [
  { vista: 'lista', nombre: 'Lista', Icono: IconFeed },
  { vista: 'mapa', nombre: 'Mapa', Icono: IconPin },
]

/**
 * Las dos caras de Explorar (#395): la lista y el mapa, en un selector grande de dos partes. Lo
 * elegido va relleno con el color del texto. Es un grupo de opciones (radiogroup): se pasa de una
 * a otra con las flechas y se elige en el momento, como cualquier selector de dos.
 *
 * Cambiar de vista cambia la dirección (la lista es "/" y el mapa "/?vista=mapa"): se puede
 * compartir, y el botón de atrás vuelve a la otra. Además se recuerda la última elegida.
 *
 * Si el cambio se hizo con el teclado, la otra vista tiene que devolver el foco a su selector: si
 * no, el foco se perdía en el cambio y había que volver a tabular desde el principio. La otra
 * vista se entera por el estado de la navegación.
 */
export function SelectorDeVista({ vista, className = '' }: { vista: Vista; className?: string }) {
  const navigate = useNavigate()
  const ubicacion = useLocation()
  const botones = useRef<Record<Vista, HTMLButtonElement | null>>({ lista: null, mapa: null })

  useEffect(() => {
    const estado = ubicacion.state as { enfocarElSelector?: boolean } | null
    if (estado?.enfocarElSelector) botones.current[vista]?.focus()
    // Solo cuando se entra a la vista: después el foco es de quien lo mueva.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function elegir(nueva: Vista, conTeclado = false) {
    if (nueva === vista) return
    navigate(direccionDeLaVista(nueva), { state: conTeclado ? { enfocarElSelector: true } : undefined })
  }

  function alTocarUnaTecla(e: KeyboardEvent<HTMLDivElement>) {
    const otra: Vista = vista === 'lista' ? 'mapa' : 'lista'
    if (['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(e.key)) {
      e.preventDefault()
      elegir(otra, true)
    } else if (e.key === 'Home' && vista !== 'lista') {
      e.preventDefault()
      elegir('lista', true)
    } else if (e.key === 'End' && vista !== 'mapa') {
      e.preventDefault()
      elegir('mapa', true)
    }
  }

  return (
    <div
      role="radiogroup"
      aria-label="Ver los locales como"
      onKeyDown={alTocarUnaTecla}
      className={`grid grid-cols-2 rounded-[0.875rem] bg-base-100 p-1 shadow-[var(--sombra-control)] ${className}`}
    >
      {OPCIONES.map(({ vista: esta, nombre, Icono }) => {
        const elegida = esta === vista
        return (
          <button
            key={esta}
            ref={(el) => {
              botones.current[esta] = el
            }}
            type="button"
            role="radio"
            aria-checked={elegida}
            // Una sola parada de teclado para todo el grupo, como en un radiogroup.
            tabIndex={elegida ? 0 : -1}
            onClick={() => elegir(esta)}
            className={`flex min-h-11 items-center justify-center gap-2 rounded-[0.6875rem] font-display text-[0.9375rem] font-extrabold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${
              elegida ? 'bg-base-content text-base-100' : 'text-base-content/70 hover:text-base-content'
            }`}
          >
            <Icono size={18} />
            {nombre}
          </button>
        )
      })}
    </div>
  )
}
