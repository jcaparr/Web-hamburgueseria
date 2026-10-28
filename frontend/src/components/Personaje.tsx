import { useId, type ReactNode } from 'react'

/**
 * Burgómetro, el personaje de la marca: una hamburguesa con el medidor como cara (#365).
 *
 * Es el dibujo del logo con ojos, boca, brazos y patas. Lo usan las reacciones de las
 * reseñas (ver DibujoDeReaccion) y las pantallas de estado: la página que no existe, el
 * error de carga, las listas vacías y el "¡Listo, gracias!" de la reseña publicada. El logo de la barra y el ícono de la pestaña siguen
 * siendo los de siempre.
 *
 * Es decorativo: el texto de la pantalla ya dice todo, así que el dibujo va oculto para
 * el lector de pantalla. En oscuro lleva un filo crema (ver .personaje en index.css),
 * porque el trazo negro se pierde contra el fondo.
 */

/**
 * El cuerpo de Burgómetro: la hamburguesa con el medidor de cara. La cara (ojos, boca y
 * aguja) la pone quien lo usa, y va dentro del pan, en el mismo lugar para todos.
 *
 * Los ids de los recortes llevan un prefijo propio: en una pantalla hay varios dibujos a
 * la vez (una reacción por chip) y con ids repetidos todos recortarían con el primero.
 */
export function Cuerpo({ children }: { children: ReactNode }) {
  const id = useId().replace(/:/g, '')
  return (
    <>
      <defs>
        <clipPath id={`${id}cD`}>
          <path d="M2 62C2 24 32 6 65 6S128 24 128 62C128 72 126 76 120 76H10C4 76 2 72 2 62Z"/>
        </clipPath>
        <clipPath id={`${id}cP`}>
          <rect x="2" y="102" width="126" height="26" rx="13"/>
        </clipPath>
        <clipPath id={`${id}cB`}>
          <rect x="10" y="126" width="110" height="20" rx="10"/>
        </clipPath>
      </defs>
      <rect x="10" y="126" width="110" height="20" rx="10" fill="#e39f58" stroke="#141110" strokeWidth="4"/>
      <g clipPath={`url(#${id}cB)`}>
        <rect x="8" y="136" width="116" height="12" fill="#a85f1c" opacity=".45"/>
      </g>
      <rect x="10" y="126" width="110" height="20" rx="10" fill="none" stroke="#141110" strokeWidth="4"/>
      <rect x="2" y="102" width="126" height="26" rx="13" fill="#5d302a" stroke="#141110" strokeWidth="4"/>
      <g clipPath={`url(#${id}cP)`}>
        <rect x="0" y="118" width="130" height="12" fill="#2b140f" opacity=".5"/>
        <rect x="0" y="102" width="130" height="5" fill="#8a4a3a" opacity=".45"/>
      </g>
      <rect x="2" y="102" width="126" height="26" rx="13" fill="none" stroke="#141110" strokeWidth="4"/>
      <path d="M18 112h8M44 118h9M72 112h8M98 119h8" stroke="#8a4a3a" strokeWidth="3" strokeLinecap="round"/>
      <path d="M4 94H126V101H98C98 113 86 113 86 101H52C52 108 44 108 44 101H4Z" fill="#f2b705" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
      <rect x="6" y="84" width="118" height="12" rx="6" fill="#d9412c" stroke="#141110" strokeWidth="4"/>
      <path d="M4 78H126V84q-3.81 9 -7.63 3q-3.81 7.2 -7.63 -3q-3.81 9 -7.63 3q-3.81 7.2 -7.63 -3q-3.81 9 -7.63 3q-3.81 7.2 -7.63 -3q-3.81 9 -7.63 3q-3.81 7.2 -7.63 -3q-3.81 9 -7.63 3q-3.81 7.2 -7.63 -3q-3.81 9 -7.63 3q-3.81 7.2 -7.63 -3q-3.81 9 -7.63 3q-3.81 7.2 -7.63 -3q-3.81 9 -7.63 3q-3.81 7.2 -7.63 -3Z" fill="#7aa33a" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
      <path d="M2 62C2 24 32 6 65 6S128 24 128 62C128 72 126 76 120 76H10C4 76 2 72 2 62Z" fill="#e39f58" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
      <g clipPath={`url(#${id}cD)`}>
        <ellipse cx="98" cy="74" rx="84" ry="46" fill="#b8641c" opacity=".38"/>
        <rect x="0" y="66" width="130" height="12" fill="#7a3d12" opacity=".35"/>
      </g>
      <path d="M2 62C2 24 32 6 65 6S128 24 128 62C128 72 126 76 120 76H10C4 76 2 72 2 62Z" fill="none" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
      <path d="M10 50C10 40 14 33 20 29" stroke="#fff3d0" strokeWidth="4" strokeLinecap="round" fill="none" opacity=".75"/>
      <g fill="#fff3d0" stroke="#141110" strokeWidth="1.6">
        <ellipse cx="34" cy="20" rx="3.4" ry="2" transform="rotate(-40 34 20)"/>
        <ellipse cx="96" cy="20" rx="3.4" ry="2" transform="rotate(40 96 20)"/>
        <ellipse cx="50" cy="12" rx="3.4" ry="2" transform="rotate(-15 50 12)"/>
        <ellipse cx="80" cy="12" rx="3.4" ry="2" transform="rotate(15 80 12)"/>
        <ellipse cx="116" cy="44" rx="3.4" ry="2" transform="rotate(70 116 44)"/>
      </g>
      <g transform="translate(9 3) scale(.86)">
        <g stroke="#141110" strokeWidth="5" strokeLinecap="round">
          <line x1="65" y1="14" x2="65" y2="6"/>
        </g>
        <ellipse cx="22" cy="64" rx="7" ry="4.5" fill="#e8604a" opacity=".45"/>
        <ellipse cx="108" cy="64" rx="7" ry="4.5" fill="#e8604a" opacity=".45"/>
        {children}
      </g>
    </>
  )
}

type Pose = 'lupa' | 'mapa' | 'confundido' | 'celu'

const PIEZAS: Record<Pose, { viewBox: string; brazos: ReactNode; cara: ReactNode; delante: ReactNode }> = {
  lupa: {
    viewBox: '-64 -26 262 236',
    brazos: (
      <>
        <path d="M4 118Q-13 110 -10 86" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
        <path d="M126 118Q145 142 144 150" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
      </>
    ),
    cara: (
      <>
        <ellipse cx="40" cy="46" rx="5.4" ry="7.4" fill="#141110"/>
        <circle cx="41.3" cy="44" r="1.4" fill="#fff"/>
        <circle cx="90" cy="45" r="9" fill="#fff" stroke="#141110" strokeWidth="3.5"/>
        <circle cx="88" cy="46" r="3.6" fill="#141110"/>
        <path d="M54 70q11 6 22-2" stroke="#141110" strokeWidth="4" fill="none" strokeLinecap="round"/>
        <polygon points="72.26,24.89 70.34,55.33 63.06,61.76 59.66,52.67" fill="#e63240" stroke="#141110" strokeWidth="3.5" strokeLinejoin="round"/>
        <circle cx="65" cy="54" r="8" fill="#5d302a" stroke="#141110" strokeWidth="4"/>
      </>
    ),
    delante: (
      <>
        <circle cx="-24" cy="56" r="17" fill="#e4f3f8" stroke="#141110" strokeWidth="5"/>
        <path d="M-12 68L2 82" stroke="#141110" strokeWidth="7" strokeLinecap="round"/>
        <path d="M-33 50q3-6 9-7" stroke="#fff" strokeWidth="3" fill="none" strokeLinecap="round"/>
        <g transform="translate(-12 84) rotate(10) scale(-1.12 1.12)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="3.57"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.96" strokeLinecap="round" fill="none"/>
        </g>
        <g transform="translate(146 154) rotate(188) scale(-1.24 1.24)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="3.21"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.77" strokeLinecap="round" fill="none"/>
        </g>
        <path d="M48 140L42 178" stroke="#141110" strokeWidth="6" strokeLinecap="round" fill="none"/>
        <g transform="translate(42 184) scale(-1 1)">
          <path d="M-12 -6C-12-13-3-15 3-12C9-9 20-8 23 0C25 7 21 11 15 11H-9C-14 11-15 5-12-6Z" fill="#ffffff" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
          <path d="M-13 5H22" stroke="#141110" strokeWidth="3"/>
          <path d="M-3 -9l3 5M3 -8l3 5" stroke="#141110" strokeWidth="2" strokeLinecap="round"/>
        </g>
        <path d="M82 140L88 178" stroke="#141110" strokeWidth="6" strokeLinecap="round" fill="none"/>
        <g transform="translate(88 184) scale(1 1)">
          <path d="M-12 -6C-12-13-3-15 3-12C9-9 20-8 23 0C25 7 21 11 15 11H-9C-14 11-15 5-12-6Z" fill="#ffffff" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
          <path d="M-13 5H22" stroke="#141110" strokeWidth="3"/>
          <path d="M-3 -9l3 5M3 -8l3 5" stroke="#141110" strokeWidth="2" strokeLinecap="round"/>
        </g>
      </>
    ),
  },
  mapa: {
    viewBox: '-64 -26 262 236',
    brazos: (
      <>
        <path d="M4 118Q-15 142 -14 150" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
        <path d="M126 118Q148 120 150 106" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
      </>
    ),
    cara: (
      <>
        <ellipse cx="40" cy="46" rx="5.4" ry="7.4" fill="#141110"/>
        <circle cx="41.3" cy="44" r="1.4" fill="#fff"/>
        <ellipse cx="90" cy="46" rx="5.4" ry="7.4" fill="#141110"/>
        <circle cx="91.3" cy="44" r="1.4" fill="#fff"/>
        <path d="M52 68Q65 82 78 68" stroke="#141110" strokeWidth="4" fill="none" strokeLinecap="round"/>
        <polygon points="78.15,27.04 69.94,56.41 61.49,61.19 60.06,51.59" fill="#e63240" stroke="#141110" strokeWidth="3.5" strokeLinejoin="round"/>
        <circle cx="65" cy="54" r="8" fill="#5d302a" stroke="#141110" strokeWidth="4"/>
      </>
    ),
    delante: (
      <>
        <g transform="translate(-16 154) rotate(172) scale(1.24 1.24)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="3.21"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.77" strokeLinecap="round" fill="none"/>
        </g>
        <g transform="rotate(8 160 84)">
          <path d="M138 58l15 6 15-6 15 6v44l-15-6-15 6-15-6z" fill="#fff3d0" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
          <path d="M153 64v44M168 58v44" stroke="#141110" strokeWidth="2.5"/>
          <path d="M144 92q8-18 16-4t14-12" stroke="#e63240" strokeWidth="3.5" strokeDasharray="1 6" strokeLinecap="round" fill="none"/>
          <path d="M174 54q8 0 8 8c0 6-8 14-8 14s-8-8-8-14q0-8 8-8z" fill="#e63240" stroke="#141110" strokeWidth="3"/>
        </g>
        <g transform="translate(150 108) rotate(-8) scale(1.12 1.12)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="3.57"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.96" strokeLinecap="round" fill="none"/>
        </g>
        <path d="M48 140L42 178" stroke="#141110" strokeWidth="6" strokeLinecap="round" fill="none"/>
        <g transform="translate(42 184) scale(-1 1)">
          <path d="M-12 -6C-12-13-3-15 3-12C9-9 20-8 23 0C25 7 21 11 15 11H-9C-14 11-15 5-12-6Z" fill="#ffffff" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
          <path d="M-13 5H22" stroke="#141110" strokeWidth="3"/>
          <path d="M-3 -9l3 5M3 -8l3 5" stroke="#141110" strokeWidth="2" strokeLinecap="round"/>
        </g>
        <path d="M82 140L88 178" stroke="#141110" strokeWidth="6" strokeLinecap="round" fill="none"/>
        <g transform="translate(88 184) scale(1 1)">
          <path d="M-12 -6C-12-13-3-15 3-12C9-9 20-8 23 0C25 7 21 11 15 11H-9C-14 11-15 5-12-6Z" fill="#ffffff" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
          <path d="M-13 5H22" stroke="#141110" strokeWidth="3"/>
          <path d="M-3 -9l3 5M3 -8l3 5" stroke="#141110" strokeWidth="2" strokeLinecap="round"/>
        </g>
      </>
    ),
  },
  confundido: {
    viewBox: '-64 -26 262 236',
    brazos: (
      <>
        <path d="M4 118Q-16 109 -16 84" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
        <path d="M126 118Q146 109 146 84" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
      </>
    ),
    cara: (
      <>
        <ellipse cx="40" cy="46" rx="5.4" ry="7.4" fill="#141110"/>
        <circle cx="41.3" cy="44" r="1.4" fill="#fff"/>
        <ellipse cx="90" cy="46" rx="5.4" ry="7.4" fill="#141110"/>
        <circle cx="91.3" cy="44" r="1.4" fill="#fff"/>
        <path d="M28 37L46 31M102 37L84 31" stroke="#141110" strokeWidth="4.5" strokeLinecap="round"/>
        <path d="M50 72q7-7 15 0t15 0" stroke="#141110" strokeWidth="4" fill="none" strokeLinecap="round"/>
        <polygon points="70.21,24.46 70.42,54.96 63.61,61.88 59.58,53.04" fill="#e63240" stroke="#141110" strokeWidth="3.5" strokeLinejoin="round"/>
        <circle cx="65" cy="54" r="8" fill="#5d302a" stroke="#141110" strokeWidth="4"/>
      </>
    ),
    delante: (
      <>
        <g transform="translate(-20 76) rotate(-20) scale(-1.24 1.24)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="3.21"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.77" strokeLinecap="round" fill="none"/>
        </g>
        <g transform="translate(150 76) rotate(20) scale(1.24 1.24)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="3.21"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.77" strokeLinecap="round" fill="none"/>
        </g>
        <path d="M48 140L42 178" stroke="#141110" strokeWidth="6" strokeLinecap="round" fill="none"/>
        <g transform="translate(42 184) scale(-1 1)">
          <path d="M-12 -6C-12-13-3-15 3-12C9-9 20-8 23 0C25 7 21 11 15 11H-9C-14 11-15 5-12-6Z" fill="#ffffff" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
          <path d="M-13 5H22" stroke="#141110" strokeWidth="3"/>
          <path d="M-3 -9l3 5M3 -8l3 5" stroke="#141110" strokeWidth="2" strokeLinecap="round"/>
        </g>
        <path d="M82 140L88 178" stroke="#141110" strokeWidth="6" strokeLinecap="round" fill="none"/>
        <g transform="translate(88 184) scale(1 1)">
          <path d="M-12 -6C-12-13-3-15 3-12C9-9 20-8 23 0C25 7 21 11 15 11H-9C-14 11-15 5-12-6Z" fill="#ffffff" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
          <path d="M-13 5H22" stroke="#141110" strokeWidth="3"/>
          <path d="M-3 -9l3 5M3 -8l3 5" stroke="#141110" strokeWidth="2" strokeLinecap="round"/>
        </g>
        <text x="108" y="6" fontFamily="Lexend,sans-serif" fontWeight="800" fontSize="40" fill="#f2b705" stroke="#141110" strokeWidth="5" paintOrder="stroke" strokeLinejoin="round">
          ?
        </text>
      </>
    ),
  },
  celu: {
    viewBox: '-64 -26 262 236',
    brazos: (
      <>
        <path d="M4 118Q-18 114 -20 94" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
        <path d="M126 118Q148 113 150 92" fill="none" stroke="#141110" strokeWidth="6" strokeLinecap="round"/>
      </>
    ),
    cara: (
      <>
        <path d="M30 50Q40 38 50 50M80 50Q90 38 100 50" stroke="#141110" strokeWidth="5" fill="none" strokeLinecap="round"/>
        <path d="M52 68Q65 82 78 68" stroke="#141110" strokeWidth="4" fill="none" strokeLinecap="round"/>
        <polygon points="75.26,25.81 70.17,55.88 62.26,61.52 59.83,52.12" fill="#e63240" stroke="#141110" strokeWidth="3.5" strokeLinejoin="round"/>
        <circle cx="65" cy="54" r="8" fill="#5d302a" stroke="#141110" strokeWidth="4"/>
      </>
    ),
    delante: (
      <>
        <g transform="translate(-24 84) rotate(-14) scale(-1.24 1.24)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="3.21"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.77" strokeLinecap="round" fill="none"/>
        </g>
        <rect x="143" y="42" width="26" height="46" rx="6" fill="#141110"/>
        <rect x="146" y="47" width="20" height="34" rx="3" fill="#fff3d0"/>
        <path d="M156 52l2.4 5 5.4.6-4 3.7 1.1 5.4-4.9-2.7-4.9 2.7 1.1-5.4-4-3.7 5.4-.6z" fill="#f2b705" stroke="#141110" strokeWidth="1.5" strokeLinejoin="round"/>
        <path d="M150 74h12" stroke="#141110" strokeWidth="2.5" strokeLinecap="round"/>
        <g transform="translate(150 94) rotate(0) scale(1.12 1.12)">
          <circle r="9" fill="#ffffff" stroke="#141110" strokeWidth="3.57"/>
          <path d="M-3.8 -8.4V-2.2M0 -9V-2M3.8 -8.4V-2.2M-8.4 -1.5Q-4.6 -0.6 -4 3.8" stroke="#141110" strokeWidth="1.96" strokeLinecap="round" fill="none"/>
        </g>
        <path d="M48 140L42 178" stroke="#141110" strokeWidth="6" strokeLinecap="round" fill="none"/>
        <g transform="translate(42 184) scale(-1 1)">
          <path d="M-12 -6C-12-13-3-15 3-12C9-9 20-8 23 0C25 7 21 11 15 11H-9C-14 11-15 5-12-6Z" fill="#ffffff" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
          <path d="M-13 5H22" stroke="#141110" strokeWidth="3"/>
          <path d="M-3 -9l3 5M3 -8l3 5" stroke="#141110" strokeWidth="2" strokeLinecap="round"/>
        </g>
        <path d="M82 140L88 178" stroke="#141110" strokeWidth="6" strokeLinecap="round" fill="none"/>
        <g transform="translate(88 184) scale(1 1)">
          <path d="M-12 -6C-12-13-3-15 3-12C9-9 20-8 23 0C25 7 21 11 15 11H-9C-14 11-15 5-12-6Z" fill="#ffffff" stroke="#141110" strokeWidth="4" strokeLinejoin="round"/>
          <path d="M-13 5H22" stroke="#141110" strokeWidth="3"/>
          <path d="M-3 -9l3 5M3 -8l3 5" stroke="#141110" strokeWidth="2" strokeLinecap="round"/>
        </g>
      </>
    ),
  },
}

/**
 * La lupa para lo que no se encontró, el mapa para lo que se perdió, las manos arriba para
 * un error y el celular para cuando la reseña salió.
 */
export function Personaje({ pose, className = '' }: { pose: Pose; className?: string }) {
  const { viewBox, brazos, cara, delante } = PIEZAS[pose]
  return (
    <svg viewBox={viewBox} aria-hidden="true" focusable="false" className={`personaje ${className}`}>
      <g className="sombra-suelo">
        <ellipse cx="65" cy="196" rx="70" ry="6" fill="#000" opacity=".22"/>
        <ellipse cx="65" cy="196" rx="44" ry="3.4" fill="#000" opacity=".2"/>
      </g>
      {brazos}
      <Cuerpo>{cara}</Cuerpo>
      {delante}
    </svg>
  )
}
