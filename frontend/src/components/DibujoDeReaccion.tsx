import type { ReactNode } from 'react'
import type { TipoDeReaccion } from '../types'
import { Cuerpo } from './Personaje'

/**
 * Burgómetro haciendo cada una de las cinco reacciones (#365).
 *
 * Es la versión chica, pensada para ir en un chip de 40 px: solo el cuerpo con la cara
 * y lo que se pega a la cabeza (el arcoíris de fondo, el signo de admiración), sin
 * brazos ni objetos, que a ese tamaño son manchas. Lo que distingue a cada una está en
 * la cara: la baba del hambre, los ojos brillantes, los de corazón, las lágrimas de la
 * risa y los ojos grandes de la sorpresa.
 *
 * Decorativo: el nombre de la reacción lo da el botón que lo contiene.
 */
const DIBUJOS: Record<TipoDeReaccion, { fondo: ReactNode; arriba: ReactNode; cara: ReactNode }> = {
  HAMBRE: {
    fondo: null,
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
  },
  ENCANTA: {
    fondo: null,
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
  },
  RISA: {
    fondo: null,
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
  },
  SORPRESA: {
    fondo: null,
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
  },
}

export function DibujoDeReaccion({ tipo, className = '' }: { tipo: TipoDeReaccion; className?: string }) {
  const { fondo, arriba, cara } = DIBUJOS[tipo]
  return (
    <svg
      viewBox="-12 -40 154 190"
      aria-hidden="true"
      focusable="false"
      className={`personaje shrink-0 overflow-hidden ${className}`}
    >
      {fondo}
      {arriba}
      <Cuerpo>{cara}</Cuerpo>
    </svg>
  )
}
