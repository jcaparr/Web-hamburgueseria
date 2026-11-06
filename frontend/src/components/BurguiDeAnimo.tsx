import type { ReactNode } from 'react'
import { CUADRO_DE_BURGUI, type Animo } from '../utils/animoDelMes'
import { Cuerpo } from './Personaje'

const TINTA = '#141110'

/** La aguja de la cara: 180° es el medidor vacío y 0° el lleno. */
function Aguja({ grados }: { grados: number }) {
  const a = (grados * Math.PI) / 180
  const punta = `${(65 + 32 * Math.cos(a)).toFixed(1)},${(54 - 32 * Math.sin(a)).toFixed(1)}`
  const dx = 5.5 * Math.sin(a)
  const dy = 5.5 * Math.cos(a)
  const base = `${(65 + dx).toFixed(1)},${(54 + dy).toFixed(1)} ${(65 - dx).toFixed(1)},${(54 - dy).toFixed(1)}`
  return (
    <>
      <polygon points={`${punta} ${base}`} fill="#e63240" stroke={TINTA} strokeWidth="3.5" strokeLinejoin="round"/>
      <circle cx="65" cy="54" r="8" fill="#5d302a" stroke={TINTA} strokeWidth="4"/>
    </>
  )
}

function Mano({ x, y, giro = 0, escala = 1.25 }: { x: number; y: number; giro?: number; escala?: number }) {
  return (
    <g transform={`translate(${x} ${y}) rotate(${giro}) scale(${escala})`}>
      <circle r="9" fill="#ffffff" stroke={TINTA} strokeWidth="3.2"/>
      <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke={TINTA} strokeWidth="1.8" strokeLinecap="round" fill="none"/>
    </g>
  )
}

function Zapatilla({ x, y, espejada = false }: { x: number; y: number; espejada?: boolean }) {
  return (
    <g transform={`translate(${x} ${y}) scale(${espejada ? -1 : 1} 1)`}>
      <path d="M-12 -6C-12-13-3-15 3-12C9-9 20-8 23 0C25 7 21 11 15 11H-9C-14 11-15 5-12-6Z" fill="#ffffff" stroke={TINTA} strokeWidth="4" strokeLinejoin="round"/>
      <path d="M-13 5H22" stroke={TINTA} strokeWidth="3"/>
    </g>
  )
}

const brazo = (d: string) => <path d={d} fill="none" stroke={TINTA} strokeWidth="6" strokeLinecap="round"/>

const PIEZAS: Record<Animo, { atras: ReactNode; cara: ReactNode; delante: ReactNode }> = {
  enojado: {
    atras: brazo('M8 108Q-14 112 -16 128M122 108Q144 112 146 128'),
    cara: (
      <>
        <path d="M26 34L50 44M104 34L80 44" stroke={TINTA} strokeWidth="5.5" strokeLinecap="round"/>
        <circle cx="40" cy="52" r="5" fill={TINTA}/>
        <circle cx="90" cy="52" r="5" fill={TINTA}/>
        <path d="M48 80Q65 66 82 80" stroke={TINTA} strokeWidth="5" fill="none" strokeLinecap="round"/>
        <Aguja grados={168}/>
      </>
    ),
    delante: (
      <>
        <Mano x={-16} y={132}/>
        <Mano x={146} y={132}/>
        <path d="M118 -2l8 -10M128 4l12 -4M106 -8l2 -12" stroke={TINTA} strokeWidth="4" strokeLinecap="round" fill="none"/>
      </>
    ),
  },
  serio: {
    atras: brazo('M6 108Q-12 120 -12 140M124 108Q142 120 142 140'),
    cara: (
      <>
        <path d="M30 48h18M82 48h18" stroke={TINTA} strokeWidth="5.5" strokeLinecap="round"/>
        <path d="M54 76h22" stroke={TINTA} strokeWidth="5" strokeLinecap="round"/>
        <Aguja grados={122}/>
      </>
    ),
    delante: (
      <>
        <Mano x={-12} y={144}/>
        <Mano x={142} y={144}/>
      </>
    ),
  },
  contento: {
    atras: brazo('M6 108Q-14 118 -14 136M124 100Q150 88 152 56'),
    cara: (
      <>
        <path d="M30 50Q40 38 50 50M80 50Q90 38 100 50" stroke={TINTA} strokeWidth="5" fill="none" strokeLinecap="round"/>
        <path d="M48 68Q65 86 82 68" stroke={TINTA} strokeWidth="5" fill="none" strokeLinecap="round"/>
        <Aguja grados={62}/>
      </>
    ),
    delante: (
      <>
        <Mano x={-14} y={140}/>
        <Mano x={152} y={50} giro={15} escala={1.35}/>
      </>
    ),
  },
  euforico: {
    atras: brazo('M6 100Q-22 80 -24 40M124 100Q152 80 154 40'),
    cara: (
      <>
        <path d="M48 66Q65 92 82 66Z" fill="#5d302a" stroke={TINTA} strokeWidth="4.5" strokeLinejoin="round"/>
        <rect x="22" y="36" width="34" height="20" rx="9" fill={TINTA}/>
        <rect x="74" y="36" width="34" height="20" rx="9" fill={TINTA}/>
        <path d="M56 42h18" stroke={TINTA} strokeWidth="5"/>
        <path d="M28 41h8M80 41h8" stroke="#ffffff" strokeWidth="3" strokeLinecap="round" opacity=".7"/>
        <Aguja grados={16}/>
      </>
    ),
    delante: (
      <>
        <Mano x={-24} y={32} giro={-20} escala={1.4}/>
        <Mano x={154} y={32} giro={20} escala={1.4}/>
        <g stroke={TINTA} strokeWidth="2">
          <rect x="-30" y="-8" width="8" height="8" fill="#f2b705" transform="rotate(20 -26 -4)"/>
          <rect x="150" y="-14" width="8" height="8" fill="#ff5a3c" transform="rotate(-25 154 -10)"/>
          <circle cx="-6" cy="-24" r="4" fill="#7aa33a"/>
          <circle cx="140" cy="10" r="4" fill="#faf3e3"/>
          <rect x="118" y="-34" width="7" height="7" fill="#7aa33a" transform="rotate(35 121 -30)"/>
        </g>
      </>
    ),
  },
}

/**
 * Burgui con el ánimo de un mes, para la imagen que se comparte de "Tu mes" (#421). La aguja de
 * su cara hace de medidor; la imagen lo dibuja desde acá, así que es el mismo dibujo de la app.
 */
export function BurguiDeAnimo({ animo }: { animo: Animo }) {
  const { atras, cara, delante } = PIEZAS[animo]
  const { x, y, ancho, alto } = CUADRO_DE_BURGUI
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox={`${x} ${y} ${ancho} ${alto}`} width={ancho * 4} height={alto * 4} aria-hidden="true">
      <ellipse cx="65" cy="194" rx="62" ry="6" fill="#000" opacity=".3"/>
      {atras}
      <path d="M48 140L44 176M82 140L86 176" stroke={TINTA} strokeWidth="6" strokeLinecap="round" fill="none"/>
      <Zapatilla x={44} y={182} espejada/>
      <Zapatilla x={86} y={182}/>
      <Cuerpo>{cara}</Cuerpo>
      {delante}
    </svg>
  )
}
