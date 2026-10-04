import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import { AvatarDeUsuario } from '../components/AvatarDeUsuario'
import { BotonSeguir } from '../components/BotonSeguir'
import { IconSearch } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { useTitulo } from '../hooks/useTitulo'
import type { UsuarioBuscado } from '../types'

/** Cuánto se espera después de la última tecla antes de preguntarle al servidor. */
const ESPERA_MS = 300

/** Abajo de esto el servidor no busca: con una letra devolvería medio padrón. */
const MINIMO = 2

export function BuscarGente() {
  const [texto, setTexto] = useState('')
  useTitulo('Buscar gente')
  const [resultados, setResultados] = useState<UsuarioBuscado[]>([])
  const [buscando, setBuscando] = useState(false)
  const [error, setError] = useState<unknown>(null)

  // Para reintentar sin tocar lo escrito. Cambiar el texto no sirve: al normalizarlo
  // volvería a ser el mismo, y el efecto no se enteraría de que hay que buscar de nuevo.
  const [intento, setIntento] = useState(0)

  // El campo muestra siempre lo que de verdad se está buscando. Dejarle escribir un
  // acento o un espacio y después ignorarlo por lo bajo es peor: vería su nombre
  // completo escrito, cero resultados, y ninguna pista de por qué.
  const limpio = texto

  useEffect(() => {
    if (limpio.length < MINIMO) {
      setResultados([])
      setBuscando(false)
      return
    }

    // Una consulta por tecla haría que escribir "juanca" dispare seis búsquedas. Se
    // espera a que pare de escribir; y si ya había salido una, su respuesta se ignora,
    // porque dos pedidos en vuelo pueden volver en cualquier orden y el que llega
    // último no es necesariamente el de lo que está escrito ahora.
    let vigente = true
    setBuscando(true)

    const reloj = setTimeout(() => {
      apiClient
        .get<UsuarioBuscado[]>('/usuarios', { params: { q: limpio } })
        .then(({ data }) => {
          if (!vigente) return
          setResultados(data)
          setError(null)
        })
        .catch((err) => {
          if (vigente) setError(err)
        })
        .finally(() => {
          if (vigente) setBuscando(false)
        })
    }, ESPERA_MS)

    return () => {
      vigente = false
      clearTimeout(reloj)
    }
  }, [limpio, intento])

  function cambioDeSeguimiento(username: string, loSigo: boolean) {
    setResultados((previos) =>
      previos.map((u) => (u.username === username ? { ...u, loSigo } : u)),
    )
  }

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
      <h1 className="font-display text-2xl font-bold leading-tight md:text-3xl">Buscar gente</h1>

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
        <LoadError error={error} onRetry={() => setIntento((n) => n + 1)} />
      ) : (
        <ul className="flex flex-col gap-2">
          {resultados.map((persona) => (
            <li
              key={persona.userId}
              className="flex items-center gap-3 rounded-box bg-base-100 p-3 ring-1 ring-inset ring-base-content/15"
            >
              <Link
                to={`/u/${persona.username}`}
                className="flex min-w-0 flex-1 items-center gap-3"
              >
                <AvatarDeUsuario username={persona.username} size={40} />
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
