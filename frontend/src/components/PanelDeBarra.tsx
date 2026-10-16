import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useLocation } from 'react-router-dom'

/**
 * El botón de la barra de arriba con su panel colgado: lo comparten la campana de
 * notificaciones (#210) y la tuerca de ajustes (#270), para que las dos se abran, se
 * vean y se cierren igual.
 *
 * Un panel y no otra pantalla: se mira de pasada, sin perder dónde se estaba. Con
 * details, como el selector de orden de Explorar, y lo que details no trae —cerrarse al
 * tocar o enfocar afuera, con Escape o al ir a otra pantalla— va a mano. Ir al botón del
 * otro panel cuenta como ir afuera, así que nunca quedan los dos abiertos.
 *
 * @param etiqueta  lo que dice el botón para un lector de pantalla
 * @param icono     lo que se ve en el botón
 * @param titulo    el encabezado del panel
 * @param insignia  lo que va montado sobre el botón, como el número de la campana
 * @param onCambio  avisa cuando el panel se abre o se cierra
 */
export function PanelDeBarra({
  etiqueta,
  icono,
  titulo,
  insignia,
  onCambio,
  children,
}: {
  etiqueta: string
  icono: ReactNode
  titulo: string
  insignia?: ReactNode
  onCambio?: (abierto: boolean) => void
  children: ReactNode
}) {
  const desplegable = useRef<HTMLDetailsElement>(null)
  const { pathname } = useLocation()
  const [abierto, setAbierto] = useState(false)

  function cerrar() {
    if (desplegable.current) desplegable.current.open = false
  }

  // Tocar un enlace del panel lleva a otra pantalla: el panel se cierra solo.
  useEffect(() => {
    if (desplegable.current) desplegable.current.open = false
  }, [pathname])

  useEffect(() => {
    if (!abierto) return
    function cerrarSiEsAfuera(evento: Event) {
      if (!desplegable.current?.contains(evento.target as Node)) cerrar()
    }
    function cerrarConEscape(evento: KeyboardEvent) {
      if (evento.key !== 'Escape') return
      cerrar()
      desplegable.current?.querySelector('summary')?.focus()
    }
    document.addEventListener('pointerdown', cerrarSiEsAfuera)
    // Con el teclado no hay pointerdown: llegar al botón del otro panel cuenta igual.
    document.addEventListener('focusin', cerrarSiEsAfuera)
    document.addEventListener('keydown', cerrarConEscape)
    return () => {
      document.removeEventListener('pointerdown', cerrarSiEsAfuera)
      document.removeEventListener('focusin', cerrarSiEsAfuera)
      document.removeEventListener('keydown', cerrarConEscape)
    }
  }, [abierto])

  return (
    // En el teléfono, details deja de ser la referencia (static) y el panel se ubica
    // contra la barra de arriba, que es sticky: así arranca justo en su borde.
    <details
      ref={desplegable}
      className="dropdown dropdown-end max-md:static"
      onToggle={(evento) => {
        setAbierto(evento.currentTarget.open)
        onCambio?.(evento.currentTarget.open)
      }}
    >
      <summary
        aria-label={etiqueta}
        className="btn btn-ghost btn-square relative list-none [&::-webkit-details-marker]:hidden"
      >
        {icono}
        {/* El pico que dice de dónde sale el panel (#217). Un cuadrado girado, con el
            centro en el borde de arriba del panel y por detrás de él: se ve solo la
            mitad de arriba, apuntando al botón. Debajo del botón quedan 11 px hasta el
            borde de la barra (8 de relleno y 3 de la tira a cuadros); el cuadrado, de
            16 px, arranca 8 antes: la mitad de su lado. */}
        {abierto && (
          <span
            aria-hidden="true"
            className="absolute left-1/2 top-[calc(100%+3px)] z-[29] size-4 -translate-x-1/2 rotate-45 rounded-[3px] bg-base-100 shadow-[var(--sombra-alzada)]"
          />
        )}
        {insignia}
      </summary>

      {/* En el teléfono, de borde a borde y pegado al borde de la barra: anclado al
          botón se salía por la izquierda, y con un hueco arriba se asomaba lo de atrás.
          En la compu, colgado del botón, bajado los 11 px que faltan hasta el borde de
          la barra y corrido a la derecha para que el pico no caiga en la esquina
          redondeada. */}
      <div className="dropdown-content z-30 flex max-h-[70dvh] flex-col overflow-hidden rounded-box bg-base-100 shadow-[var(--sombra-alzada)] max-md:inset-x-4 max-md:top-full md:-right-4 md:mt-[11px] md:w-96">
        <h2 className="px-4 pb-2 pt-3 font-display text-lg font-bold">{titulo}</h2>
        <div className="overflow-y-auto px-2 pb-2">{children}</div>
      </div>
    </details>
  )
}
