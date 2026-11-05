import type { KeyboardEvent } from 'react'

/**
 * Enter en un buscador que filtra mientras se escribe: no hay nada más que ejecutar, así
 * que cierra el teclado del celular para que se vean los resultados. `blur()` y no
 * `preventDefault()` solo: sin sacar el foco, el teclado sigue tapando la lista.
 * Mientras se compone con IME (`isComposing`) Enter confirma la palabra, no busca.
 */
export function cerrarTecladoConEnter(e: KeyboardEvent<HTMLInputElement>) {
  if (e.key !== 'Enter' || e.nativeEvent.isComposing) return
  e.preventDefault()
  e.currentTarget.blur()
}
