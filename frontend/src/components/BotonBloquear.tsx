import { useState } from 'react'
import { apiClient } from '../api/client'

/**
 * Bloquear a alguien, con una confirmación antes.
 *
 * No es una acción que convenga que salga de un toque al pasar: el que la busca la va
 * a encontrar igual, y el que la tocó sin querer no se queda con una persona
 * desaparecida de la app sin entender por qué.
 *
 * La confirmación dice qué va a pasar y no solo "¿estás seguro?", porque lo que hay
 * que decidir es si eso es lo que se quiere.
 */
export function BotonBloquear({
  username,
  onBloqueado,
}: {
  username: string
  /** Se llama al terminar: el perfil ya no se puede mostrar, así que hay que irse. */
  onBloqueado: () => void
}) {
  const [confirmando, setConfirmando] = useState(false)
  const [enViaje, setEnViaje] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function bloquear() {
    setEnViaje(true)
    setError(null)
    apiClient
      .post(`/usuarios/${username}/bloquear`)
      .then(onBloqueado)
      .catch(() => {
        setError('No pudimos bloquearlo. Probá de nuevo.')
        setEnViaje(false)
      })
  }

  if (!confirmando) {
    return (
      <button
        type="button"
        onClick={() => setConfirmando(true)}
        className="btn btn-ghost btn-sm text-error"
      >
        Bloquear
      </button>
    )
  }

  return (
    <div className="flex flex-col gap-2 rounded-box bg-base-100 p-4 ring-1 ring-inset ring-error/30">
      <p className="text-sm text-base-content/80">
        Si bloqueás a @{username}, dejan de seguirse y ninguno de los dos va a ver al
        otro en el feed ni en el buscador. Podés deshacerlo desde tu perfil.
      </p>
      {error && <p className="text-xs text-error">{error}</p>}
      <div className="flex gap-2">
        <button
          type="button"
          onClick={bloquear}
          disabled={enViaje}
          className="btn btn-sm btn-error"
        >
          {enViaje ? 'Bloqueando…' : 'Bloquear'}
        </button>
        <button
          type="button"
          onClick={() => setConfirmando(false)}
          disabled={enViaje}
          className="btn btn-sm btn-ghost"
        >
          Cancelar
        </button>
      </div>
    </div>
  )
}
