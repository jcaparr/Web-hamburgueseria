import { createContext } from 'react'

export interface ReseniaRapidaValue {
  /** Abre la pantalla de reseña con el buscador de local. Sin sesión, lleva a entrar y vuelve. */
  abrir: () => void
}

/** El lápiz de la barra de abajo y la pantalla de reseña que abre (#337). */
export const ReseniaRapidaContext = createContext<ReseniaRapidaValue | null>(null)

/** Se dispara al guardar o borrar una reseña desde el lápiz, para que la ficha abierta se recargue. */
export const RESENIA_GUARDADA = 'burgometro:resenia-guardada'
