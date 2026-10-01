import { useEffect, useState } from 'react'
import { apiClient } from '../api/client'

/**
 * Los barrios que tienen al menos un local, para los selectores.
 *
 * Se piden una sola vez por pantalla: no cambian mientras alguien la mira. Si el pedido
 * falla queda la lista vacía y el selector en "todos", y el resto de la pantalla
 * funciona igual: filtrar por barrio es una comodidad, no lo que la sostiene.
 */
export function useBarrios(): string[] {
  const [barrios, setBarrios] = useState<string[]>([])

  useEffect(() => {
    apiClient
      .get<string[]>('/burger-joints/barrios')
      .then(({ data }) => setBarrios(data))
      .catch(() => setBarrios([]))
  }, [])

  return barrios
}
