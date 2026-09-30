interface IconProps {
  size?: number
  className?: string
}

/** El feed: tarjetas apiladas, una atrás de otra. */
export function IconFeed({ size = 18, className }: IconProps) {
  return (
    <svg viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} className={className}>
      <rect x="2.5" y="3" width="15" height="5.5" rx="1.5" />
      <rect x="2.5" y="11.5" width="15" height="5.5" rx="1.5" />
    </svg>
  )
}

export function IconSearch({ size = 18, className }: IconProps) {
  return (
    <svg viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} className={className}>
      <circle cx="9" cy="9" r="6" />
      <line x1="14" y1="14" x2="18" y2="18" />
    </svg>
  )
}

export function IconMedal({ size = 18, className }: IconProps) {
  return (
    <svg viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.7} className={className}>
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
    <svg viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.6} className={className}>
      <path d="M10 18s6-5.5 6-10a6 6 0 1 0-12 0c0 4.5 6 10 6 10z" />
      <circle cx="10" cy="8" r="2" />
    </svg>
  )
}

export function IconRoute({ size = 18, className }: IconProps) {
  return (
    <svg viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} className={className}>
      <circle cx="5" cy="5" r="2.2" />
      <circle cx="15" cy="15" r="2.2" />
      <path d="M7.2 5h5.3a2.8 2.8 0 0 1 0 5.6H7.5a2.8 2.8 0 0 0 0 5.6h5.3" strokeDasharray="2.4 2.2" />
    </svg>
  )
}

export function IconUser({ size = 18, className }: IconProps) {
  return (
    <svg viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.7} className={className}>
      <circle cx="10" cy="7" r="3.5" />
      <path d="M3.5 17c1-3.2 3.8-5 6.5-5s5.5 1.8 6.5 5" />
    </svg>
  )
}

export function IconSettings({ size = 18, className }: IconProps) {
  return (
    <svg viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.6} className={className}>
      <circle cx="10" cy="10" r="2.6" />
      <path d="M10 2.5v2M10 15.5v2M2.5 10h2M15.5 10h2M4.6 4.6l1.4 1.4M14 14l1.4 1.4M15.4 4.6 14 6M6 14l-1.4 1.4" />
    </svg>
  )
}

export function IconChevronRight({ size = 16, className }: IconProps) {
  return (
    <svg viewBox="0 0 20 20" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.8} className={className}>
      <path d="M7.5 4.5 13 10l-5.5 5.5" />
    </svg>
  )
}

/** La cámara del selector de foto de la reseña. */
export function IconCamera({ size = 18, className }: IconProps) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor" strokeWidth={1.6} className={className}>
      <path d="M3 8.5A1.5 1.5 0 0 1 4.5 7h2.2l1.1-2h8.4l1.1 2h2.2A1.5 1.5 0 0 1 21 8.5v9a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17.5z" />
      <circle cx="12" cy="13" r="3.8" />
    </svg>
  )
}
