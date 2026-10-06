import { useEffect, useRef, useState, type ReactNode } from 'react'
import { nota } from '../utils/numeros'

/**
 * El burgómetro grande: un tablero con la nota de un local, como en el logo.
 *
 * Es el momento en que la app hace honor al nombre, así que está en dos lugares nomás:
 * el resumen de la ficha y el primer puesto del ranking. En las listas va el sello
 * chico (ScoreBadge), que es el mismo tablero en miniatura.
 *
 * El arco se llena hasta la nota sobre cinco y la aguja apunta ahí. Las seis semillas de
 * sésamo son la escala, del cero al cinco, como las del pan del logo. Al aparecer, la
 * aguja sube desde el cero hasta la nota, una sola vez: es la única animación que no
 * responde a algo que tocó la persona, y dice qué es este número. Con "reducir
 * movimiento" aparece ya en su lugar.
 *
 * @param valor    la nota, de 1 a 5
 * @param tamanio  lg para la ficha, md para el primer puesto
 * @param etiqueta lo que se le antepone al número para un lector de pantalla
 * @param children lo que va debajo del número, como cuántas reseñas lo dicen
 */
export function MedidorGrande({
  valor,
  tamanio = 'lg',
  etiqueta = 'Promedio',
  children,
}: {
  valor: number
  tamanio?: 'md' | 'lg'
  etiqueta?: string
  children?: ReactNode
}) {
  const lleno = Math.min(Math.max(valor / 5, 0), 1) * 100
  // Arranca en cero y pasa a la nota recién cuando el tablero entra en pantalla: en el
  // teléfono el resumen de la ficha queda más abajo de lo que se ve al entrar, y si
  // arrancara al cargar, la aguja ya estaría quieta cuando alguien llega a verla.
  const tablero = useRef<SVGSVGElement>(null)
  const [mostrado, setMostrado] = useState(false)
  useEffect(() => {
    const elemento = tablero.current
    if (!elemento || typeof IntersectionObserver === 'undefined') {
      setMostrado(true)
      return
    }
    const observador = new IntersectionObserver(
      ([entrada]) => {
        if (entrada.isIntersecting) {
          setMostrado(true)
          observador.disconnect()
        }
      },
      { threshold: 0.6 },
    )
    observador.observe(elemento)
    return () => observador.disconnect()
  }, [])
  const ahora = mostrado ? lleno : 0
  const grande = tamanio === 'lg'

  return (
    <div className="flex flex-none flex-col items-center">
      <svg
        ref={tablero}
        viewBox="0 0 200 110"
        aria-hidden="true"
        className={grande ? 'w-36 md:w-40' : 'w-24'}
      >
        <path
          d="M22 100 A78 78 0 0 1 178 100"
          pathLength={100}
          fill="none"
          strokeWidth={14}
          strokeLinecap="round"
          className="stroke-base-content/10"
        />
        <path
          d="M22 100 A78 78 0 0 1 178 100"
          pathLength={100}
          fill="none"
          strokeWidth={14}
          strokeLinecap="round"
          className="stroke-primary transition-[stroke-dasharray] duration-1000 ease-out motion-reduce:transition-none"
          style={{ strokeDasharray: `${ahora} 100` }}
        />
        {/* La escala: del cero al cinco, cada semilla mirando al centro. */}
        <g className="fill-base-content/35">
          <ellipse cx="41" cy="100" rx="2.4" ry="5" transform="rotate(-90 41 100)" />
          <ellipse cx="52.27" cy="65.32" rx="2.4" ry="5" transform="rotate(-54 52.27 65.32)" />
          <ellipse cx="81.77" cy="43.89" rx="2.4" ry="5" transform="rotate(-18 81.77 43.89)" />
          <ellipse cx="118.23" cy="43.89" rx="2.4" ry="5" transform="rotate(18 118.23 43.89)" />
          <ellipse cx="147.73" cy="65.32" rx="2.4" ry="5" transform="rotate(54 147.73 65.32)" />
          <ellipse cx="159" cy="100" rx="2.4" ry="5" transform="rotate(90 159 100)" />
        </g>
        <g
          className="transition-transform duration-1000 motion-reduce:transition-none"
          style={{
            transform: `rotate(${-90 + ahora * 1.8}deg)`,
            transformOrigin: '100px 100px',
            // Un poco de rebote al final, como una aguja de verdad que se pasa y vuelve.
            transitionTimingFunction: 'cubic-bezier(.3, 1.35, .5, 1)',
          }}
        >
          <polygon points="100,42 105,100 95,100" className="fill-neutral" />
        </g>
        <circle cx="100" cy="100" r="9" className="fill-neutral" />
        <circle cx="100" cy="100" r="3.5" className="fill-base-100" />
      </svg>
      <span
        className={`font-display font-extrabold leading-none tabular-nums ${grande ? 'mt-1 text-5xl' : 'mt-0.5 text-2xl'}`}
      >
        <span className="sr-only">{etiqueta}: </span>
        {nota(valor)}
        <span className="sr-only"> de 5</span>
      </span>
      {children}
    </div>
  )
}
