import { useState } from 'react'
import { apiClient } from '../api/client'

/**
 * Seguir o dejar de seguir a alguien.
 *
 * Cambia en pantalla antes de que conteste el servidor, y vuelve atrás si falla. Es
 * una acción que se toca al pasar, muchas veces seguidas en una lista de resultados;
 * esperar medio segundo por cada una haría sentir la lista trabada.
 */
export function BotonSeguir({
  username,
  loSigo,
  onCambio,
  chico,
}: {
  username: string
  loSigo: boolean
  /** Para que quien lo muestra actualice su propia copia, y los contadores si los tiene. */
  onCambio: (loSigo: boolean) => void
  chico?: boolean
}) {
  const [enViaje, setEnViaje] = useState(false)

  function alternar() {
    const queria = !loSigo
    setEnViaje(true)
    onCambio(queria)

    const pedido = queria
      ? apiClient.post(`/usuarios/${username}/seguir`)
      : apiClient.delete(`/usuarios/${username}/seguir`)

    pedido
      .catch(() => onCambio(!queria))
      .finally(() => setEnViaje(false))
  }

  return (
    <button
      type="button"
      onClick={alternar}
      disabled={enViaje}
      className={`btn ${chico ? 'btn-sm' : ''} ${loSigo ? 'btn-outline' : 'btn-primary'}`}
    >
      {loSigo ? 'Siguiendo' : 'Seguir'}
    </button>
  )
}
