interface IconProps {
  size?: number
  className?: string
}

/** El feed: tarjetas apiladas, una atrás de otra. */
export function IconFeed({ size = 18, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} className={className}>
      <rect x="2.5" y="3" width="15" height="5.5" rx="1.5" />
      <rect x="2.5" y="11.5" width="15" height="5.5" rx="1.5" />
    </svg>
  )
}

export function IconSearch({ size = 18, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} className={className}>
      <circle cx="9" cy="9" r="6" />
      <line x1="14" y1="14" x2="18" y2="18" />
    </svg>
  )
}

export function IconMedal({ size = 18, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.7} className={className}>
      <circle cx="10" cy="12" r="5" />
      <path d="M7 8 5 3M13 8l2-5" />
    </svg>
  )
}

export function IconHeart({ size = 18, className, filled = false }: IconProps & { filled?: boolean }) {
  return (
    <svg
      viewBox="0 0 20 20"
      width={size}
      height={size}
      fill={filled ? 'currentColor' : 'none'}
      stroke="currentColor"
      strokeWidth={1.6}
      className={className}
    >
      <path d="M10 17s-6.5-4-6.5-8.5C3.5 5.8 5.3 4 7.5 4c1.2 0 2.1.6 2.5 1.4C10.4 4.6 11.3 4 12.5 4c2.2 0 4 1.8 4 4.5C16.5 13 10 17 10 17z" />
    </svg>
  )
}

export function IconPin({ size = 14, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.6} className={className}>
      <path d="M10 18s6-5.5 6-10a6 6 0 1 0-12 0c0 4.5 6 10 6 10z" />
      <circle cx="10" cy="8" r="2" />
    </svg>
  )
}

/** Un candado: cerrado cuando algo es privado, con el arco levantado cuando no. */
export function IconLock({ size = 14, abierto = false, className }: IconProps & { abierto?: boolean }) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} className={className}>
      <rect x="4.5" y="9" width="11" height="8" rx="1.8" />
      <path d={abierto ? 'M7 9V6.2a3 3 0 0 1 5.8-1' : 'M7 9V6a3 3 0 0 1 6 0v3'} />
    </svg>
  )
}

export function IconRoute({ size = 18, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} className={className}>
      <circle cx="5" cy="5" r="2.2" />
      <circle cx="15" cy="15" r="2.2" />
      <path d="M7.2 5h5.3a2.8 2.8 0 0 1 0 5.6H7.5a2.8 2.8 0 0 0 0 5.6h5.3" strokeDasharray="2.4 2.2" />
    </svg>
  )
}

export function IconUser({ size = 18, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.7} className={className}>
      <circle cx="10" cy="7" r="3.5" />
      <path d="M3.5 17c1-3.2 3.8-5 6.5-5s5.5 1.8 6.5 5" />
    </svg>
  )
}

/** La tuerca de ajustes (#270). Con dientes y no con rayos: los rayos se leían como un sol, o sea, como el tema. */
export function IconSettings({ size = 18, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" className={className}>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z" />
    </svg>
  )
}

/** La campana del buzón de notificaciones (#210). */
export function IconBell({ size = 20, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.6} strokeLinejoin="round" className={className}>
      <path d="M5 13.5V9a5 5 0 0 1 10 0v4.5l1.5 2h-13Z" />
      <path d="M8.2 17.2a2 2 0 0 0 3.6 0" strokeLinecap="round" />
    </svg>
  )
}

export function IconChevronRight({ size = 16, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} className={className}>
      <path d="M7.5 4.5 13 10l-5.5 5.5" />
    </svg>
  )
}

/** La cámara del selector de foto de la reseña. */
export function IconCamera({ size = 18, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.6} className={className}>
      <path d="M3 8.5A1.5 1.5 0 0 1 4.5 7h2.2l1.1-2h8.4l1.1 2h2.2A1.5 1.5 0 0 1 21 8.5v9a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17.5z" />
      <circle cx="12" cy="13" r="3.8" />
    </svg>
  )
}

/** El lápiz de editar la propia reseña. */
export function IconPencil({ size = 18, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" className={className}>
      <path d="M13.5 3.5l3 3-9 9H4.5v-3z" />
      <path d="M11.5 5.5l3 3" />
    </svg>
  )
}

/** Volver a la pantalla anterior. */
export function IconArrowLeft({ size = 18, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" className={className}>
      <path d="M16 10H4.5M9.5 5 4.5 10l5 5" />
    </svg>
  )
}

/** Compartir: una flecha que sale de una caja, como en los teléfonos. */
export function IconShare({ size = 18, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" className={className}>
      <path d="M10 12.5V3M6.5 6.5 10 3l3.5 3.5" />
      <path d="M6 9H5a1.5 1.5 0 0 0-1.5 1.5v5A1.5 1.5 0 0 0 5 17h10a1.5 1.5 0 0 0 1.5-1.5v-5A1.5 1.5 0 0 0 15 9h-1" />
    </svg>
  )
}

/** El horario de un local. */
export function IconClock({ size = 18, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.6} strokeLinecap="round" className={className}>
      <circle cx="10" cy="10" r="7" />
      <path d="M10 6v4l2.5 1.5" />
    </svg>
  )
}

/** Un enlace que abre otra página, fuera de la app. */
export function IconExternal({ size = 14, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" className={className}>
      <path d="M11.5 4.5h4v4M15.5 4.5 9 11M8 5.5H5.5a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V12" />
    </svg>
  )
}

/** Reaccionar: una cara sonriente con un más, como en los mensajes. */
export function IconReaccionar({ size = 18, className }: IconProps) {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.6} strokeLinecap="round" className={className}>
      <path d="M16.6 9.4A7 7 0 1 1 10.6 3.4" />
      <path d="M6.8 11.6c.8 1.2 1.9 1.8 3.2 1.8s2.4-.6 3.2-1.8" />
      <circle cx="7.4" cy="8" r=".6" fill="currentColor" />
      <circle cx="12.4" cy="8" r=".6" fill="currentColor" />
      <path d="M16 1.8v4.4M13.8 4h4.4" />
    </svg>
  )
}

/** Instagram: la cámara con su visor y el punto del flash. */
export function IconInstagram({ size = 20, className }: IconProps) {
  return (
    <svg aria-hidden="true" focusable="false" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" className={className}>
      <rect x="3.5" y="3.5" width="17" height="17" rx="5" />
      <circle cx="12" cy="12" r="4" />
      <circle cx="17.2" cy="6.8" r="0.6" fill="currentColor" />
    </svg>
  )
}

/** TikTok: la nota musical con su colita. */
export function IconTikTok({ size = 20, className }: IconProps) {
  return (
    <svg aria-hidden="true" focusable="false" viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" className={className}>
      <path d="M14 3.5v11.2a3.7 3.7 0 1 1-3.7-3.7" />
      <path d="M14 3.5c.3 2.4 1.9 4 4.5 4.2" />
    </svg>
  )
}

/**
 * El ícono de Logros (#315): una chapita dentada con una estrella, el mismo dibujo de las
 * medallas de la pantalla. Trazo fino, como los demás íconos de la barra.
 */
export function IconLogros({ size = 24, className }: IconProps) {
  return (
    <svg
      aria-hidden="true"
      viewBox="-3 -3 54 54"
      width={size}
      height={size}
      fill="none"
      stroke="currentColor"
      strokeWidth={2.6}
      strokeLinejoin="round"
      strokeLinecap="round"
      className={className}
    >
      <path d="M24.00 3.00Q27.91 -1.94 30.19 3.93Q35.38 0.36 35.83 6.65Q41.84 4.77 40.42 10.91Q46.72 10.88 43.55 16.33Q49.58 18.16 44.94 22.43Q50.16 25.96 44.47 28.67Q48.42 33.58 42.19 34.50Q44.51 40.36 38.28 39.39Q38.78 45.68 33.11 42.92Q31.73 49.07 27.13 44.77Q24.00 50.23 20.87 44.77Q16.27 49.07 14.89 42.92Q9.22 45.68 9.72 39.39Q3.49 40.36 5.81 34.50Q-0.42 33.58 3.53 28.67Q-2.16 25.96 3.06 22.43Q-1.58 18.16 4.45 16.33Q1.28 10.88 7.58 10.91Q6.16 4.77 12.17 6.65Q12.62 0.36 17.81 3.93Q20.09 -1.94 24.00 3.00Z" />
      <path d="M24 7l5 10 11 1.6-8 7.8 2 11-10-5.3-10 5.3 2-11-8-7.8L19 17z" transform="translate(9 9) scale(.625)" />
    </svg>
  )
}
