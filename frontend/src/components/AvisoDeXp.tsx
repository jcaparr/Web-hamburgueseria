import { useEffect, useRef, useState } from 'react'
import { EVENTO_DE_XP, type XpGanada } from '../utils/avisosDeXp'

/**
 * La XP que acaba de sumar una reseña (#312): un "+N XP" que vuela hasta el avatar y,
 * si subió de nivel, el aviso "Subiste al nivel N".
 *
 * Se monta una vez en App y escucha el evento de {@link avisarXpGanada}, así salta en
 * cualquier pantalla. Si el modal de reseña sigue abierto espera a que se cierre: el
 * `<dialog>` modal tapa todo lo demás, y un aviso detrás no lo vería nadie. Los avisos
 * que llegan mientras tanto se suman en uno solo, sin perder el de nivel.
 *
 * Con "reducir movimiento" no hay vuelo: queda el texto quieto unos segundos, que es lo
 * que importa.
 *
 * El contenedor `role="status"` está siempre montado y solo cambia lo de adentro: un
 * aviso que aparece ya con su contenido no siempre lo anuncian los lectores de pantalla.
 */
export function AvisoDeXp() {
  const [aviso, setAviso] = useState<XpGanada | null>(null)
  const pendiente = useRef<XpGanada | null>(null)
  const espera = useRef<number | undefined>(undefined)
  const chip = useRef<HTMLSpanElement>(null)

  useEffect(() => {
    function mostrarCuandoSePueda() {
      espera.current = undefined
      if (!pendiente.current) return
      if (document.querySelector('dialog[open]')) {
        espera.current = window.setTimeout(mostrarCuandoSePueda, 150)
        return
      }
      setAviso(pendiente.current)
      pendiente.current = null
    }
    function alGanar(e: Event) {
      const nuevo = (e as CustomEvent<XpGanada>).detail
      const previo = pendiente.current
      pendiente.current = previo
        ? {
            ganada: previo.ganada + nuevo.ganada,
            nivelNuevo: Math.max(previo.nivelNuevo ?? 0, nuevo.nivelNuevo ?? 0) || null,
          }
        : nuevo
      if (espera.current === undefined) mostrarCuandoSePueda()
    }
    window.addEventListener(EVENTO_DE_XP, alGanar)
    return () => {
      window.removeEventListener(EVENTO_DE_XP, alGanar)
      window.clearTimeout(espera.current)
    }
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
          { transform: `translate(${dx}px, ${dy}px) scale(0.5)`, opacity: 0, offset: 1 },
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

  return (
    <div
      role="status"
      aria-live="polite"
      className="pointer-events-none fixed inset-x-0 bottom-24 z-50 flex flex-col items-center gap-3 px-4 md:bottom-12"
    >
      {aviso && (
        <>
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
        </>
      )}
    </div>
  )
}
