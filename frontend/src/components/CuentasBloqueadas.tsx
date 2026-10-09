import { useEffect, useState } from 'react'
import { apiClient } from '../api/client'
import { fechaYHora } from '../utils/fechaYHora'
import type { UsuarioBloqueado } from '../types'

/**
 * La lista de a quiénes bloqueaste, para poder deshacerlo.
 *
 * Es el único lugar de la app donde esos nombres vuelven a aparecer: en todos los
 * demás quedaron escondidos, así que sin esto un bloqueo sería para siempre.
 *
 * Arranca cerrada porque casi siempre está vacía, y una sección vacía más en el perfil
 * es ruido. La cuenta en el título es lo único que hace falta ver de un vistazo.
 */
export function CuentasBloqueadas() {
  const [bloqueados, setBloqueados] = useState<UsuarioBloqueado[]>([])
  const [desbloqueando, setDesbloqueando] = useState<string | null>(null)
  const [fallo, setFallo] = useState<string | null>(null)

  useEffect(() => {
    let vigente = true

    apiClient
      .get<UsuarioBloqueado[]>('/usuarios/mis-bloqueos')
      .then(({ data }) => vigente && setBloqueados(data))
      // Que no se pueda traer esta lista no tiene que romper el perfil entero: es una
      // sección de más, no lo que la persona vino a ver.
      .catch(() => undefined)

    return () => {
      vigente = false
    }
  }, [])

  function desbloquear(username: string) {
    setDesbloqueando(username)
    setFallo(null)
    apiClient
      .delete(`/usuarios/${username}/bloquear`)
      .then(() => setBloqueados((previos) => previos.filter((b) => b.username !== username)))
      // Antes fallaba en silencio: el botón volvía a decir "Desbloquear" y parecía que
      // no se había tocado.
      .catch(() => setFallo(username))
      .finally(() => setDesbloqueando(null))
  }

  if (bloqueados.length === 0) return null

  return (
    <details className="flex flex-col gap-3">
      <summary className="cursor-pointer rounded-field py-3 font-display text-base font-bold">
        Cuentas bloqueadas ({bloqueados.length})
      </summary>

      {fallo && (
        <p role="alert" className="text-sm text-error">
          No pudimos desbloquear a @{fallo}. Probá de nuevo.
        </p>
      )}
      <ul className="mt-1 flex flex-col gap-2">
        {bloqueados.map((b) => (
          <li
            key={b.userId}
            className="flex items-center gap-3 tarjeta p-3"
          >
            <div className="flex min-w-0 flex-1 flex-col">
              <span className="truncate text-sm font-semibold">@{b.username}</span>
              <span className="text-xs text-base-content/70">
                Bloqueada {fechaYHora(b.bloqueadoEl).toLowerCase()}
              </span>
            </div>
            <button
              type="button"
              onClick={() => desbloquear(b.username)}
              disabled={desbloqueando === b.username}
              className="btn btn-ghost btn-sm"
            >
              {desbloqueando === b.username ? 'Desbloqueando…' : 'Desbloquear'}
              <span className="sr-only"> a @{b.username}</span>
            </button>
          </li>
        ))}
      </ul>
    </details>
  )
}
