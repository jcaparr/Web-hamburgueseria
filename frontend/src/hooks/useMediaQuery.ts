import { useSyncExternalStore } from 'react'

/** Si la pantalla cumple una media query, y se actualiza cuando cambia. */
export function useMediaQuery(consulta: string): boolean {
  return useSyncExternalStore(
    (avisar) => {
      const lista = window.matchMedia(consulta)
      lista.addEventListener('change', avisar)
      return () => lista.removeEventListener('change', avisar)
    },
    () => window.matchMedia(consulta).matches,
    () => false,
  )
}
