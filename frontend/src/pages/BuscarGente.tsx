import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import { AvatarDeUsuario } from '../components/AvatarDeUsuario'
import { BotonSeguir } from '../components/BotonSeguir'
import { IconSearch } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { usePedido } from '../hooks/usePedido'
import { useTitulo } from '../hooks/useTitulo'
import type { UsuarioBuscado } from '../types'

/** Cuánto se espera después de la última tecla antes de preguntarle al servidor. */
const ESPERA_MS = 300

/** Abajo de esto el servidor no busca: con una letra devolvería medio padrón. */
const MINIMO = 2

export function BuscarGente() {
  const [texto, setTexto] = useState('')
  useTitulo('Buscar gente')

  // El campo muestra siempre lo que de verdad se está buscando. Dejarle escribir un
  // acento o un espacio y después ignorarlo por lo bajo es peor: vería su nombre
  // completo escrito, cero resultados, y ninguna pista de por qué.
  const limpio = texto

  // Una consulta por tecla haría que escribir "juanca" dispare seis búsquedas: se busca
  // lo escrito recién cuando se deja de tipear un momento. Si ya había salido una
  // búsqueda, su respuesta se ignora: dos pedidos en vuelo pueden volver en cualquier
  // orden, y el que llega último no es necesariamente el de lo que está escrito ahora.
  const [buscado, setBuscado] = useState('')
  useEffect(() => {
    const reloj = setTimeout(() => setBuscado(limpio), ESPERA_MS)
    return () => clearTimeout(reloj)
  }, [limpio])

  const pedido = usePedido(buscado.length >= MINIMO ? `usuarios:${buscado}` : null, () =>
    apiClient.get<UsuarioBuscado[]>('/usuarios', { params: { q: buscado } }).then(({ data }) => data),
  )
  const alcanza = limpio.length >= MINIMO
  const resultados = alcanza ? (pedido.datos ?? []) : []
  const buscando = alcanza && (limpio !== buscado || pedido.cargando)
  const error = alcanza ? pedido.error : undefined

  function cambioDeSeguimiento(username: string, loSigo: boolean) {
    pedido.actualizar((previos) =>
      previos.map((u) => (u.username === username ? { ...u, loSigo } : u)),
    )
  }

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
      <h1 className="titulo-pagina">Buscar gente</h1>

      <label className="input input-bordered flex items-center gap-2 focus-within:border-primary">
        <IconSearch size={16} className="text-base-content/70" />
        <input
          autoFocus
          type="search"
          value={texto}
          onChange={(e) => setTexto(e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, ''))}
          placeholder="Nombre de usuario…"
          className="grow"
          aria-label="Buscar por nombre de usuario"
          autoComplete="off"
          autoCapitalize="none"
          autoCorrect="off"
          spellCheck={false}
          enterKeyHint="search"
        />
      </label>

      {error ? (
        <LoadError error={error} onRetry={pedido.reintentar} />
      ) : (
        <ul className="flex flex-col gap-2">
          {resultados.map((persona) => (
            <li
              key={persona.userId}
              className="flex items-center gap-3 tarjeta p-3"
            >
              <Link
                to={`/u/${persona.username}`}
                className="flex min-w-0 flex-1 items-center gap-3"
              >
                <AvatarDeUsuario username={persona.username} hamburguesa={persona.hamburguesa} size={40} />
                <div className="flex min-w-0 flex-col">
                  <span className="truncate font-semibold">@{persona.username}</span>
                  <span className="text-xs text-base-content/70">
                    {persona.resenias === 1 ? '1 reseña' : `${persona.resenias} reseñas`}
                  </span>
                </div>
              </Link>
              <BotonSeguir
                chico
                username={persona.username}
                loSigo={persona.loSigo}
                onCambio={(loSigo) => cambioDeSeguimiento(persona.username, loSigo)}
              />
            </li>
          ))}
        </ul>
      )}

      {/* Un solo renglón que dice cómo va la búsqueda, y que el lector de pantalla
          anuncia: antes los resultados aparecían y desaparecían sin que se dijera. */}
      <p role="status" className="text-sm text-base-content/70">
        {error
          ? ''
          : limpio.length < MINIMO
            ? `Escribí al menos ${MINIMO} letras del nombre de usuario que buscás.`
            : buscando
              ? 'Buscando…'
              : resultados.length === 0
                ? 'No hay nadie con ese nombre.'
                : resultados.length === 1
                  ? '1 persona'
                  : `${resultados.length} personas`}
      </p>
    </div>
  )
}
