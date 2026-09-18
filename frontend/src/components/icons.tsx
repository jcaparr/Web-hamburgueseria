interface IconProps {
  size?: number
  className?: string
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
