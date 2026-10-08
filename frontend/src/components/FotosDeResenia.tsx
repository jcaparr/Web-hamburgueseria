import { useRef, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { FotoDeResenia } from './FotoDeResenia'
import { IconChevronRight } from './icons'

/**
 * Las fotos de una reseña, una al lado de la otra, para pasar con el dedo (#185).
 *
 * Con una sola se ve como siempre, sin nada alrededor: los puntitos y las flechas
 * aparecen recién cuando hay algo más para ver.
 *
 * Se pasan deslizando, que es como se pasan las fotos en cualquier red, y con las
 * flechas para quien está con el mouse o el teclado. Los puntitos de abajo dicen
 * cuántas hay y en cuál se está.
 *
 * @param enlace a dónde lleva tocar una foto. Va fuera del Tab y del lector de
 *   pantalla, porque en la tarjeta hay otro enlace al mismo lugar.
 */
export function FotosDeResenia({
  fotos,
  autorUsername,
  enlace,
}: {
  fotos: string[]
  autorUsername: string
  enlace?: string
}) {
  const tira = useRef<HTMLDivElement>(null)
  const [actual, setActual] = useState(0)
  // Las que no cargaron salen de la tira: un cuadro vacío en el medio se ve como una
  // foto que todavía está cargando, y no carga nunca.
  const [fallidas, setFallidas] = useState<string[]>([])
  const visibles = fotos.filter((foto) => !fallidas.includes(foto))

  if (visibles.length === 0) return null

  const cuantas = visibles.length
  const enCual = Math.min(actual, cuantas - 1)
  const fallo = (foto: string) => () => setFallidas((previas) => [...previas, foto])

  function envolver(contenido: ReactNode) {
    if (!enlace) return contenido
    return (
      <Link to={enlace} tabIndex={-1} aria-hidden="true" className="block w-full">
        {contenido}
      </Link>
    )
  }

  if (cuantas === 1) {
    return envolver(<FotoDeResenia src={visibles[0]} autorUsername={autorUsername} onFallo={fallo(visibles[0])} />)
  }

  function ir(indice: number) {
    const elemento = tira.current
    if (!elemento) return
    const sinMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    elemento.scrollTo({ left: indice * elemento.clientWidth, behavior: sinMovimiento ? 'auto' : 'smooth' })
  }

  // La foto en la que se está se deduce de cuánto se deslizó la tira, y no se guarda al
  // tocar una flecha: así da lo mismo si se llegó con el dedo o con el botón.
  function alDeslizar() {
    const elemento = tira.current
    if (!elemento || elemento.clientWidth === 0) return
    setActual(Math.round(elemento.scrollLeft / elemento.clientWidth))
  }

  const flecha = 'btn btn-circle btn-sm absolute top-1/2 -translate-y-1/2 border-0 bg-black/55 text-white hover:bg-black/75'

  return (
    <div role="group" aria-roledescription="carrusel" aria-label={`Fotos de la reseña de @${autorUsername}`} className="relative">
      <div ref={tira} onScroll={alDeslizar} className="carousel w-full">
        {visibles.map((foto, i) => (
          <div key={foto} className="carousel-item w-full">
            {envolver(
              <FotoDeResenia
                src={foto}
                autorUsername={autorUsername}
                alt={`Foto ${i + 1} de ${cuantas} de la reseña de @${autorUsername}`}
                onFallo={fallo(foto)}
              />,
            )}
          </div>
        ))}
      </div>

      {enCual > 0 && (
        <button type="button" onClick={() => ir(enCual - 1)} aria-label="Foto anterior" className={`${flecha} left-2`}>
          <IconChevronRight size={18} className="rotate-180" />
        </button>
      )}
      {enCual < cuantas - 1 && (
        <button type="button" onClick={() => ir(enCual + 1)} aria-label="Foto siguiente" className={`${flecha} right-2`}>
          <IconChevronRight size={18} />
        </button>
      )}

      <div aria-hidden="true" className="pointer-events-none absolute inset-x-0 bottom-2.5 flex justify-center gap-1.5">
        {visibles.map((foto, i) => (
          <span
            key={foto}
            className={`size-1.5 rounded-full shadow-sm transition-colors ${i === enCual ? 'bg-white' : 'bg-white/50'}`}
          />
        ))}
      </div>
    </div>
  )
}
