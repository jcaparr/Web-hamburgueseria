import { useContext } from 'react'
import { ReseniaRapidaContext } from './resenia'

/** Para abrir la pantalla de reseña desde cualquier lado. Solo funciona debajo de ReseniaRapidaProvider. */
export function useReseniaRapida() {
  const ctx = useContext(ReseniaRapidaContext)
  if (!ctx) throw new Error('useReseniaRapida must be used within ReseniaRapidaProvider')
  return ctx
}
