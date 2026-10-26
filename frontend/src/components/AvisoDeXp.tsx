import { useEffect, useRef, useState } from 'react'
import { EVENTO_DE_XP, type XpGanada } from '../utils/avisosDeXp'

/**
 * La XP que acaba de sumar una reseña (#312): un "+N XP" que vuela hasta el avatar y,
 * si subió de nivel, el aviso "Subiste al nivel N".
 *
 * Se monta una vez en App y escucha el evento de {@link avisarXpGanada}, así salta en
 * cualquier pantalla. Si el modal de reseña sigue abierto espera a que se cierre: el
 * `<dialog>` modal tapa todo lo demás, y un aviso detrás no lo vería nadie.
 *
 * Con "reducir movimiento" no hay vuelo: queda el texto quieto unos segundos, que es lo
 * que importa.
 */
export function AvisoDeXp() {
  const [aviso, setAviso] = useState<XpGanada | null>(null)
  const pendiente = useRef<XpGanada | null>(null)
  const chip = useRef<HTMLSpanElement>(null)

  useEffect(() => {
    function mostrarCuandoSePueda() {
      const dialogoAbierto = document.querySelector('dialog[open]')
      if (dialogoAbierto) {
        window.setTimeout(mostrarCuandoSePueda, 150)
        return
      }
      if (pendiente.current) {
        setAviso(pendiente.current)
        pendiente.current = null
      }
    }
    function alGanar(e: Event) {
      pendiente.current = (e as CustomEvent<XpGanada>).detail
      mostrarCuandoSePueda()
    }
    window.addEventListener(EVENTO_DE_XP, alGanar)
    return () => window.removeEventListener(EVENTO_DE_XP, alGanar)
  }, [])

  // El vuelo hacia el avatar, y se limpia solo al terminar.
  useEffect(() => {
    if (!aviso) return
    const elemento = chip.current
    const quieto = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    const destino = [...document.querySelectorAll<HTMLElement>('[data-destino-xp]')].find(
      (el) => el.getClientRects().length > 0,
    )
    let animacion: Animation | undefined
    if (elemento && destino && !quieto) {
      const hasta = destino.getBoundingClientRect()
      const desde = elemento.getBoundingClientRect()
      const dx = hasta.left + hasta.width / 2 - (desde.left + desde.width / 2)
      const dy = hasta.top + hasta.height / 2 - (desde.top + desde.height / 2)
      animacion = elemento.animate(
        [
          { transform: 'translate(0, 0) scale(1)', opacity: 1, offset: 0 },
          { transform: 'translate(0, 0) scale(1)', opacity: 1, offset: 0.35 },
          { transform: `translate(${dx}px, ${dy}px) scale(0.5)`, opacity: 0.2, offset: 1 },
        ],
        { duration: 1500, easing: 'ease-in-out', fill: 'forwards' },
      )
    }
    const cuanto = aviso.nivelNuevo ? 4500 : 2200
    const timer = window.setTimeout(() => setAviso(null), cuanto)
    return () => {
      animacion?.cancel()
      window.clearTimeout(timer)
    }
  }, [aviso])

  if (!aviso) return null

  return (
    <div
      role="status"
      aria-live="polite"
      className="pointer-events-none fixed inset-x-0 bottom-24 z-50 flex flex-col items-center gap-3 px-4 md:bottom-12"
    >
      <span
        ref={chip}
        className="rounded-full bg-[#f2b705] px-4 py-1.5 font-display text-base font-bold tabular-nums text-[#3a2a1e] shadow-lg"
      >
        +{aviso.ganada} XP
      </span>
      {aviso.nivelNuevo && (
        <span className="rounded-box bg-neutral px-4 py-2.5 text-sm font-semibold text-neutral-content shadow-lg">
          Subiste al nivel {aviso.nivelNuevo}
        </span>
      )}
    </div>
  )
}
