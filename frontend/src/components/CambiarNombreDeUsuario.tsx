import { useState, type FormEvent } from 'react'
import { apiClient } from '../api/client'
import { useAuth } from '../context/useAuth'
import { usePedido } from '../hooks/usePedido'
import { useNombreDeUsuario } from '../hooks/useNombreDeUsuario'
import { CampoNombreDeUsuario } from './CampoNombreDeUsuario'
import { IconPencil } from './icons'

/** Lo que contesta GET /profile/username: desde cuándo puede cambiarlo, o null si ya puede. */
interface EstadoDelNombre {
  puedeCambiarDesde: string | null
  texto: string | null
}

/**
 * Cambiar el nombre de usuario, una vez cada 30 días (#380).
 *
 * Cerrado es una fila con el nombre actual. Si todavía no puede cambiarlo, la fila dice
 * desde cuándo y no hay botón: el plazo lo decide el servidor, acá solo se avisa. Si
 * puede, el lápiz abre el campo, con el aviso del plazo antes de confirmar.
 */
export function CambiarNombreDeUsuario() {
  const { user, cambiarNombreDeUsuario } = useAuth()
  const estado = usePedido('nombre-de-usuario', () =>
    apiClient.get<EstadoDelNombre>('/profile/username').then(({ data }) => data),
  )
  const campo = useNombreDeUsuario(user?.username ?? '', user?.username)
  const [abierto, setAbierto] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!user) return null

  const igual = campo.valor === user.username

  function abrir() {
    campo.escribir(user!.username)
    setError(null)
    setAbierto(true)
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    if (guardando || igual || campo.valor.length < 3) return
    setGuardando(true)
    setError(null)
    try {
      await cambiarNombreDeUsuario(campo.valor)
      setAbierto(false)
      estado.reintentar()
    } catch (err: any) {
      const data = err?.response?.data
      if (campo.rechazar(err)) {
        // Queda marcado en el campo, que es donde se arregla.
      } else if (data?.code === 'USERNAME_COOLDOWN') {
        setError(data.error)
        estado.reintentar()
      } else {
        setError('No pudimos guardar el cambio. Probá de nuevo.')
      }
    } finally {
      setGuardando(false)
    }
  }

  const bloqueadoHasta = estado.datos?.texto ?? null

  if (!abierto) {
    return (
      <div className="px-2">
        <div className="flex min-h-11 items-center justify-between gap-2">
          <span className="min-w-0 truncate">
            <span className="text-sm text-base-content/70">Usuario </span>
            <span className="font-semibold">@{user.username}</span>
          </span>
          {estado.datos && !bloqueadoHasta && (
            <button
              type="button"
              onClick={abrir}
              aria-label="Cambiar nombre de usuario"
              className="btn btn-ghost btn-square min-h-11 min-w-11"
            >
              <IconPencil size={18} />
            </button>
          )}
        </div>
        {bloqueadoHasta && (
          <p className="pb-1 text-xs text-base-content/70">
            Podés cambiarlo de nuevo el {bloqueadoHasta}.
          </p>
        )}
        {estado.error != null && !estado.datos && (
          <p role="alert" className="pb-1 text-sm text-error">
            No pudimos ver si podés cambiarlo.{' '}
            <button type="button" onClick={estado.reintentar} className="link font-semibold">
              Reintentar
            </button>
          </p>
        )}
      </div>
    )
  }

  return (
    <form onSubmit={guardar} className="flex flex-col gap-2 px-2 pb-1">
      <CampoNombreDeUsuario
        campo={campo}
        autoFocus
        cambiando
        ayuda="Después no vas a poder cambiarlo hasta dentro de 30 días."
      />
      {error && (
        <p role="alert" className="text-sm text-error">
          {error}
        </p>
      )}
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={guardando || igual || campo.valor.length < 3 || campo.chequeando}
          className="btn btn-primary min-h-11 flex-1"
        >
          {guardando ? 'Guardando…' : 'Guardar'}
        </button>
        <button type="button" onClick={() => setAbierto(false)} className="btn btn-ghost min-h-11">
          Cancelar
        </button>
      </div>
    </form>
  )
}
