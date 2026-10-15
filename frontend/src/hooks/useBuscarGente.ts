import { useEffect, useState } from 'react'
import { apiClient } from '../api/client'
import { usePedido } from './usePedido'
import type { UsuarioBuscado } from '../types'

/** Cuánto se espera después de la última tecla antes de preguntarle al servidor. */
const ESPERA_MS = 300

/** Abajo de esto el servidor no busca: con una letra devolvería medio padrón. */
export const MINIMO = 2

/**
 * La búsqueda de gente por nombre de usuario, la misma en /buscar y en el Feed (#252).
 */
export function useBuscarGente() {
  const [texto, setTextoCrudo] = useState('')

  // El campo muestra siempre lo que de verdad se está buscando. Dejarle escribir un
  // acento o un espacio y después ignorarlo por lo bajo es peor: vería su nombre
  // completo escrito, cero resultados, y ninguna pista de por qué.
  function setTexto(escrito: string) {
    const limpio = escrito.toLowerCase().replace(/[^a-z0-9_]/g, '')
    setTextoCrudo(limpio)
    return limpio
  }

  // Una consulta por tecla haría que escribir "juanca" dispare seis búsquedas: se busca
  // lo escrito recién cuando se deja de tipear un momento. Si ya había salido una
  // búsqueda, su respuesta se ignora: dos pedidos en vuelo pueden volver en cualquier
  // orden, y el que llega último no es necesariamente el de lo que está escrito ahora.
  const [buscado, setBuscado] = useState('')
  useEffect(() => {
    const reloj = setTimeout(() => setBuscado(texto), ESPERA_MS)
    return () => clearTimeout(reloj)
  }, [texto])

  const pedido = usePedido(buscado.length >= MINIMO ? `usuarios:${buscado}` : null, () =>
    apiClient.get<UsuarioBuscado[]>('/usuarios', { params: { q: buscado } }).then(({ data }) => data),
  )
  const alcanza = texto.length >= MINIMO
  const resultados = alcanza ? (pedido.datos ?? []) : []
  const buscando = alcanza && (texto !== buscado || pedido.cargando)
  const error = alcanza ? pedido.error : undefined

  function cambioDeSeguimiento(username: string, loSigo: boolean) {
    pedido.actualizar((previos) =>
      previos.map((u) => (u.username === username ? { ...u, loSigo } : u)),
    )
  }

  return { texto, setTexto, resultados, buscando, error, reintentar: pedido.reintentar, cambioDeSeguimiento }
}

export type BusquedaDeGente = ReturnType<typeof useBuscarGente>

/** Cómo va la búsqueda, en un renglón. */
export function estadoDeLaBusqueda({ texto, resultados, buscando, error }: BusquedaDeGente) {
  if (error) return ''
  if (texto.length < MINIMO) return `Escribí al menos ${MINIMO} letras del nombre de usuario que buscás.`
  if (buscando) return 'Buscando…'
  if (resultados.length === 0) return 'No hay nadie con ese nombre.'
  return resultados.length === 1 ? '1 persona' : `${resultados.length} personas`
}
