/**
 * El dibujo del logo de Burgómetro: el pan de arriba hace de tablero, las semillas de
 * sésamo son las marcas de la escala y la aguja de kétchup apunta alto. Abajo, el
 * cheddar, el medallón y el pan de abajo.
 *
 * Los colores van fijos y no salen del tema: el pan y la carne no existen en ningún otro
 * lado, y las semillas y la aguja son las de la hamburguesa, no las del tema de turno (en
 * el oscuro, el fondo de las tarjetas pintaría las semillas de negro). El contorno sí es
 * currentColor: marrón sobre crema y crema sobre la pizarra del modo oscuro.
 *
 * Es decorativo: va siempre al lado del nombre escrito, que es lo que lee un lector de
 * pantalla.
 */
export function MarcaBurgometro({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 120 100" aria-hidden="true" focusable="false" className={className}>
      <rect x="10" y="70" width="100" height="13" rx="6.5" fill="#7a4526" stroke="currentColor" strokeWidth="3" />
      <rect x="14" y="85" width="92" height="11" rx="5.5" fill="#cf8636" stroke="currentColor" strokeWidth="3" />
      <path
        d="M16 67 H104 L100 74 H80 C80 81 74 81 74 74 H40 L36 79 L32 74 H20 Z"
        className="fill-secondary"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinejoin="round"
      />
      <path
        d="M12 66 A48 48 0 0 1 108 66 Z"
        fill="#cf8636"
        stroke="currentColor"
        strokeWidth="3"
        strokeLinejoin="round"
      />
      {/* Siete semillas sobre un arco de radio 38, cada una alineada con el centro,
          como las marcas de un velocímetro. */}
      <g fill="#f7efd8" stroke="currentColor" strokeWidth="1.2">
        <ellipse cx="23.3" cy="56.17" rx="2.4" ry="5" transform="rotate(-75 23.3 56.17)" />
        <ellipse cx="30.89" cy="41.57" rx="2.4" ry="5" transform="rotate(-50 30.89 41.57)" />
        <ellipse cx="43.94" cy="31.56" rx="2.4" ry="5" transform="rotate(-25 43.94 31.56)" />
        <ellipse cx="60" cy="28" rx="2.4" ry="5" />
        <ellipse cx="76.06" cy="31.56" rx="2.4" ry="5" transform="rotate(25 76.06 31.56)" />
        <ellipse cx="89.11" cy="41.57" rx="2.4" ry="5" transform="rotate(50 89.11 41.57)" />
        <ellipse cx="96.7" cy="56.17" rx="2.4" ry="5" transform="rotate(75 96.7 56.17)" />
      </g>
      <polygon
        points="86.05,44.15 62.57,69.06 57.43,62.94"
        className="fill-ketchup"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinejoin="round"
      />
      <circle cx="60" cy="66" r="6" fill="currentColor" />
    </svg>
  )
}
