import type { ReactNode } from 'react'
import type { TipoDeReaccion } from '../types'
import { Cuerpo } from './Personaje'

/**
 * Burgómetro haciendo cada una de las cinco reacciones (#365): el personaje entero, con
 * sus manos y lo que lleva cada una (el arcoíris de fondo, las estrellas, los corazones,
 * los cubiertos del hambre, el signo de admiración), como en la maqueta que aprobó Juan.
 *
 * El recuadro se pasa de largo a propósito (overflow visible): los brazos, las estrellas
 * y el signo salen del cuerpo, y recortarlos a un rectángulo los dejaba cortados.
 *
 * Decorativo: el nombre de la reacción lo da el botón que lo contiene.
 */
const DIBUJOS: Record<
  TipoDeReaccion,
  { fondo: ReactNode; brazos: ReactNode; arriba: ReactNode; cara: ReactNode; delante: ReactNode }
> = {
  HAMBRE: {
    fondo: null,
    brazos: (
      <>
        <path d="M4 118Q-15 102 -14 70" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
        <path d="M126 118Q145 102 144 70" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
      </>
    ),
    arriba: null,
    cara: (
      <>
        <ellipse cx="40" cy="46" rx="5.4" ry="7.4" fill="#141110"/>
        <circle cx="41.3" cy="44" r="1.4" fill="#fff"/>
        <ellipse cx="90" cy="46" rx="5.4" ry="7.4" fill="#141110"/>
        <circle cx="91.3" cy="44" r="1.4" fill="#fff"/>
        <path d="M32 32q8-5 16 0M82 32q8-5 16 0" stroke="#141110" strokeWidth="3.5" fill="none" strokeLinecap="round"/>
        <polygon points="75.26,25.81 70.17,55.88 62.26,61.52 59.83,52.12" fill="#e63240" stroke="#141110" strokeWidth="3.5" strokeLinejoin="round"/>
        <circle cx="65" cy="54" r="8" fill="#5d302a" stroke="#141110" strokeWidth="4"/>
        <path d="M50 66H80Q78 80 65 80T50 66Z" fill="#141110" stroke="#141110" strokeWidth="3" strokeLinejoin="round"/>
        <path d="M57 77Q65 71 73 77Q65 82 57 77Z" fill="#e0564a"/>
        <path d="M84 70q4 6 0 9q-4-3 0-9z" fill="#6bb6e8" stroke="#141110" strokeWidth="2.5"/>
      </>
    ),
    delante: (
      <>
        <g transform="translate(-16 68) rotate(-8) scale(-1.24 1.24)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="3.21"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.77" strokeLinecap="round" fill="none"/>
        </g>
        <g transform="translate(146 68) rotate(8) scale(1.24 1.24)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="3.21"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.77" strokeLinecap="round" fill="none"/>
        </g>
        <g stroke="#141110" strokeWidth="3.5" strokeLinecap="round" fill="#cfd3d6">
          <path d="M-16 50V24M-22 24V36M-10 24V36" fill="none"/>
          <path d="M-22 36H-10" />
        </g>
        <path d="M146 52V22q9 4 9 18q0 8-9 10z" fill="#cfd3d6" stroke="#141110" strokeWidth="3.5" strokeLinejoin="round"/>
      </>
    ),
  },
  MARAVILLOSO: {
    fondo: (
      <>
        <path d="M-8 124A73 115 0 0 1 138 124" fill="none" stroke="#141110" strokeWidth="58"/>
        <path d="M-31 124A96 138 0 0 1 161 124" fill="none" stroke="#e63240" strokeWidth="9.4"/>
        <path d="M-22 124A87 129 0 0 1 152 124" fill="none" stroke="#f28a1c" strokeWidth="9.4"/>
        <path d="M-13 124A78 120 0 0 1 143 124" fill="none" stroke="#f2b705" strokeWidth="9.4"/>
        <path d="M-4 124A69 111 0 0 1 134 124" fill="none" stroke="#7aa33a" strokeWidth="9.4"/>
        <path d="M5 124A60 102 0 0 1 125 124" fill="none" stroke="#4aa3df" strokeWidth="9.4"/>
        <path d="M14 124A51 93 0 0 1 116 124" fill="none" stroke="#8b5fbf" strokeWidth="9.4"/>
      </>
    ),
    brazos: (
      <>
        <path d="M4 118Q-15 105 -14 76" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
        <path d="M126 118Q145 105 144 76" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
      </>
    ),
    arriba: null,
    cara: (
      <>
        <circle cx="40" cy="44" r="10.5" fill="#fff" stroke="#141110" strokeWidth="3.6"/>
        <circle cx="40" cy="45" r="6.4" fill="#141110"/>
        <circle cx="37.4" cy="42" r="2.6" fill="#fff"/>
        <circle cx="42.4" cy="47" r="1.3" fill="#fff"/>
        <circle cx="90" cy="44" r="10.5" fill="#fff" stroke="#141110" strokeWidth="3.6"/>
        <circle cx="90" cy="45" r="6.4" fill="#141110"/>
        <circle cx="87.4" cy="42" r="2.6" fill="#fff"/>
        <circle cx="92.4" cy="47" r="1.3" fill="#fff"/>
        <polygon points="74.27,25.47 70.23,55.7 62.53,61.61 59.77,52.3" fill="#e63240" stroke="#141110" strokeWidth="3.5" strokeLinejoin="round"/>
        <circle cx="65" cy="54" r="8" fill="#5d302a" stroke="#141110" strokeWidth="4"/>
        <path d="M46 64H84Q82 82 65 82T46 64Z" fill="#141110" stroke="#141110" strokeWidth="3" strokeLinejoin="round"/>
        <path d="M54 79Q65 70 76 79Q65 84 54 79Z" fill="#e0564a"/>
      </>
    ),
    delante: (
      <>
        <g transform="translate(-18 70) rotate(-14) scale(-1.37 1.37)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="2.92"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.61" strokeLinecap="round" fill="none"/>
        </g>
        <g transform="translate(148 70) rotate(14) scale(1.37 1.37)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="2.92"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.61" strokeLinecap="round" fill="none"/>
        </g>
        <path transform="translate(-26 34) scale(1)" d="M0-10C1.5-3 3-1.5 10 0C3 1.5 1.5 3 0 10C-1.5 3-3 1.5-10 0C-3-1.5-1.5-3 0-10Z" fill="#fff" stroke="#141110" strokeWidth="2.4" strokeLinejoin="round"/>
        <path transform="translate(152 30) scale(0.8)" d="M0-10C1.5-3 3-1.5 10 0C3 1.5 1.5 3 0 10C-1.5 3-3 1.5-10 0C-3-1.5-1.5-3 0-10Z" fill="#fff" stroke="#141110" strokeWidth="3" strokeLinejoin="round"/>
        <path transform="translate(24 -8) scale(0.7)" d="M0-10C1.5-3 3-1.5 10 0C3 1.5 1.5 3 0 10C-1.5 3-3 1.5-10 0C-3-1.5-1.5-3 0-10Z" fill="#fff" stroke="#141110" strokeWidth="3.43" strokeLinejoin="round"/>
        <path transform="translate(108 -12) scale(0.9)" d="M0-10C1.5-3 3-1.5 10 0C3 1.5 1.5 3 0 10C-1.5 3-3 1.5-10 0C-3-1.5-1.5-3 0-10Z" fill="#fff" stroke="#141110" strokeWidth="2.67" strokeLinejoin="round"/>
      </>
    ),
  },
  ENCANTA: {
    fondo: null,
    brazos: (
      <>
        <path d="M4 118Q17 125 50 116" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
        <path d="M126 118Q113 125 80 116" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
      </>
    ),
    arriba: null,
    cara: (
      <>
        <path transform="translate(40 46) scale(1.35)" d="M0 7C-13-2-8-12 0-6C8-12 13-2 0 7Z" fill="#e63240" stroke="#141110" strokeWidth="2.22" strokeLinejoin="round"/>
        <path transform="translate(90 46) scale(1.35)" d="M0 7C-13-2-8-12 0-6C8-12 13-2 0 7Z" fill="#e63240" stroke="#141110" strokeWidth="2.22" strokeLinejoin="round"/>
        <polygon points="76.24,26.18 70.1,56.06 62,61.42 59.9,51.94" fill="#e63240" stroke="#141110" strokeWidth="3.5" strokeLinejoin="round"/>
        <circle cx="65" cy="54" r="8" fill="#5d302a" stroke="#141110" strokeWidth="4"/>
        <path d="M50 66Q65 84 80 66Z" fill="#141110" stroke="#141110" strokeWidth="3" strokeLinejoin="round"/>
      </>
    ),
    delante: (
      <>
        <g transform="translate(51 118) rotate(22) scale(-1.37 1.37)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="2.92"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.61" strokeLinecap="round" fill="none"/>
        </g>
        <g transform="translate(79 118) rotate(-22) scale(1.37 1.37)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="2.92"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.61" strokeLinecap="round" fill="none"/>
        </g>
        <path transform="translate(-8 30) scale(1)" d="M0 7C-13-2-8-12 0-6C8-12 13-2 0 7Z" fill="#e63240" stroke="#141110" strokeWidth="3" strokeLinejoin="round"/>
        <path transform="translate(138 14) scale(0.8)" d="M0 7C-13-2-8-12 0-6C8-12 13-2 0 7Z" fill="#e63240" stroke="#141110" strokeWidth="3.75" strokeLinejoin="round"/>
      </>
    ),
  },
  RISA: {
    fondo: null,
    brazos: (
      <>
        <path d="M4 118Q9 126 34 118" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
        <path d="M126 118Q121 126 96 118" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
      </>
    ),
    arriba: null,
    cara: (
      <>
        <path d="M28 38L52 46L28 54M102 38L78 46L102 54" stroke="#141110" strokeWidth="5" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
        <polygon points="74.27,25.47 70.23,55.7 62.53,61.61 59.77,52.3" fill="#e63240" stroke="#141110" strokeWidth="3.5" strokeLinejoin="round"/>
        <circle cx="65" cy="54" r="8" fill="#5d302a" stroke="#141110" strokeWidth="4"/>
        <path d="M46 64H84Q82 82 65 82T46 64Z" fill="#141110" stroke="#141110" strokeWidth="3" strokeLinejoin="round"/>
        <path d="M54 79Q65 70 76 79Q65 84 54 79Z" fill="#e0564a"/>
        <path d="M14 52q-8 8-6 16q8-2 6-16zM116 52q8 8 6 16q-8-2-6-16z" fill="#6bb6e8" stroke="#141110" strokeWidth="3"/>
      </>
    ),
    delante: (
      <>
        <g transform="translate(34 118) rotate(82) scale(-1.37 1.37)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="2.92"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.61" strokeLinecap="round" fill="none"/>
        </g>
        <g transform="translate(96 118) rotate(-82) scale(1.37 1.37)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="2.92"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.61" strokeLinecap="round" fill="none"/>
        </g>
        <g stroke="#141110" strokeWidth="5" strokeLinecap="round" fill="none">
          <path d="M-8 80Q-14 92-8 104M138 80Q144 92 138 104"/>
        </g>
      </>
    ),
  },
  SORPRESA: {
    fondo: null,
    brazos: (
      <>
        <path d="M4 118Q-5 97 6 60" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
        <path d="M126 118Q135 97 124 60" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
      </>
    ),
    arriba: (
      <>
        <path d="M58-8L62-34H68L72-8Z" fill="#f2b705" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
        <circle cx="65" cy="-3" r="5" fill="#f2b705" stroke="#141110" strokeWidth="3"/>
        <g stroke="#141110" strokeWidth="5" strokeLinecap="round">
          <path d="M34 -6L26 -16M96 -6L104 -16"/>
        </g>
      </>
    ),
    cara: (
      <>
        <circle cx="40" cy="45" r="10" fill="#fff" stroke="#141110" strokeWidth="4"/>
        <circle cx="90" cy="45" r="10" fill="#fff" stroke="#141110" strokeWidth="4"/>
        <circle cx="40" cy="46" r="4" fill="#141110"/>
        <circle cx="90" cy="46" r="4" fill="#141110"/>
        <polygon points="66.05,24.02 70.5,54.19 64.72,62 59.5,53.81" fill="#e63240" stroke="#141110" strokeWidth="3.5" strokeLinejoin="round"/>
        <circle cx="65" cy="54" r="8" fill="#5d302a" stroke="#141110" strokeWidth="4"/>
        <ellipse cx="65" cy="73" rx="7" ry="8" fill="#141110"/>
      </>
    ),
    delante: (
      <>
        <g transform="translate(4 54) rotate(-14) scale(-1.37 1.37)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="2.92"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.61" strokeLinecap="round" fill="none"/>
        </g>
        <g transform="translate(126 54) rotate(14) scale(1.37 1.37)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="2.92"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.61" strokeLinecap="round" fill="none"/>
        </g>
      </>
    ),
  },
}

export function DibujoDeReaccion({ tipo, className = '' }: { tipo: TipoDeReaccion; className?: string }) {
  const { fondo, brazos, arriba, cara, delante } = DIBUJOS[tipo]
  return (
    <svg
      viewBox="-34 -8 198 160"
      aria-hidden="true"
      focusable="false"
      className={`personaje shrink-0 overflow-visible ${className}`}
    >
      {fondo}
      {brazos}
      {arriba}
      <Cuerpo>{cara}</Cuerpo>
      {delante}
    </svg>
  )
}
