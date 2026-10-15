/**
 * El dibujo del logo de Burgómetro (#255, el del Canva "Logo hamburguesa"): el pan de
 * arriba hace de tablero, con siete marcas y la aguja roja apuntando alto. Abajo, el
 * cheddar que chorrea sobre el medallón y el pan de abajo.
 *
 * Los colores van fijos y no salen del tema: el logo es uno solo, en claro y en oscuro,
 * con su contorno negro. En oscuro lo rodea un borde crema, como un calco pegado: sin él
 * el contorno negro se pierde contra la pizarra del fondo. Ese borde es la silueta
 * dibujada primero, con un trazo grueso del color --calco-del-logo, que en claro es
 * transparente.
 *
 * Es decorativo: va siempre al lado del nombre escrito, que es lo que lee un lector de
 * pantalla.
 */
export function MarcaBurgometro({ className }: { className?: string }) {
  return (
    <svg viewBox="-5 -5 140 118" aria-hidden="true" focusable="false" className={className}>
      <g
        fill="var(--calco-del-logo)"
        stroke="var(--calco-del-logo)"
        strokeWidth="10"
        strokeLinejoin="round"
      >
        <path d="M9 57 C9 24 36 3 65 3 C94 3 121 24 121 57 C121 61 119 63 115 63 H15 C11 63 9 61 9 57 Z" />
        <rect x="7" y="64" width="116" height="22" rx="11" />
        <rect x="12" y="86" width="106" height="18" rx="9" />
      </g>

      <g stroke="#141110" strokeWidth="3.5" strokeLinejoin="round">
        <rect x="12" y="88" width="106" height="16" rx="8" fill="#e39f58" />
        <rect x="7" y="66" width="116" height="20" rx="10" fill="#5d302a" />
        <path
          d="M14 62 H116 V71 H98 C98 81 86 81 86 71 H46 C46 80 34 80 34 71 H14 Z"
          fill="#e5812c"
          strokeWidth="3"
        />
        <path
          d="M9 57 C9 24 36 3 65 3 C94 3 121 24 121 57 C121 61 119 63 115 63 H15 C11 63 9 61 9 57 Z"
          fill="#e39f58"
        />
      </g>

      {/* Los brillos del pan, como en el dibujo original. */}
      <g stroke="#ffffff" strokeOpacity="0.4" strokeLinecap="round" fill="none">
        <path d="M19 42 C21 32 26 24 33 18" strokeWidth="4" />
        <path d="M21 95 H30" strokeWidth="3" />
      </g>

      {/* Siete marcas sobre un arco alrededor del eje de la aguja, cada 24°, como las
          de un velocímetro. */}
      <g stroke="#141110" strokeWidth="3.6" strokeLinecap="round">
        <line x1="99.24" y1="46.88" x2="106.85" y2="44.40" />
        <line x1="91.75" y1="33.91" x2="97.70" y2="28.56" />
        <line x1="79.64" y1="25.11" x2="82.90" y2="17.80" />
        <line x1="65" y1="22" x2="65" y2="14" />
        <line x1="50.36" y1="25.11" x2="47.10" y2="17.80" />
        <line x1="38.25" y1="33.91" x2="32.30" y2="28.56" />
        <line x1="30.76" y1="46.88" x2="23.15" y2="44.40" />
      </g>

      <polygon
        points="94.7,31.2 69.01,62.46 60.99,53.54"
        fill="#e63240"
        stroke="#141110"
        strokeWidth="2.5"
        strokeLinejoin="round"
      />
      <circle cx="65" cy="58" r="7.5" fill="#5d302a" stroke="#141110" strokeWidth="3" />
    </svg>
  )
}
