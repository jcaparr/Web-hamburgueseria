import { useState } from 'react'
import { apiClient } from '../api/client'
import { usePedido } from '../hooks/usePedido'
import { Interruptor } from './Interruptor'

/**
 * Si tus guardadas y tus recorridos se ven en tu perfil público (#264).
 *
 * Arranca apagado: hasta ahora eran solo tuyos, y nadie aceptó mostrarlos. Uno solo para
 * las dos listas y no uno por lista: es la misma pregunta —"¿muestro lo que pienso
 * hacer?"— y dos interruptores pedían pensar dos veces lo mismo.
 *
 * Se marca enseguida, pero no se puede volver a tocar hasta que el servidor contesta, y
 * lo que queda es lo que él dice: prenderlo y apagarlo rápido mandaba dos pedidos que
 * podían llegar al revés, y la pantalla decía "apagado" con las listas a la vista.
 */
export function MostrarMisListas() {
  const pedido = usePedido('muestra-listas', () =>
    apiClient.get<{ muestra: boolean }>('/profile/muestra-listas').then(({ data }) => data.muestra),
  )
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function cambiar(nuevo: boolean) {
    pedido.actualizar(() => nuevo)
    setGuardando(true)
    setError(null)
    apiClient
      .put<{ muestra: boolean }>('/profile/muestra-listas', { muestra: nuevo })
      .then(({ data }) => pedido.actualizar(() => data.muestra))
      .catch(() => {
        pedido.actualizar(() => !nuevo)
        setError('No pudimos guardar el cambio. Probá de nuevo.')
      })
      .finally(() => setGuardando(false))
  }

  return (
    <fieldset className="flex flex-col gap-1" disabled={guardando}>
      <legend className="mb-1 text-sm font-semibold">Tu perfil público</legend>
      {pedido.datos !== undefined ? (
        <Interruptor activo={pedido.datos} onCambiar={cambiar}>
          Mostrar mis guardadas y mis recorridos
        </Interruptor>
      ) : pedido.error ? (
        <p role="alert" className="flex min-h-11 flex-wrap items-center gap-x-2 text-sm text-error">
          No pudimos ver si mostrás tus listas.
          <button type="button" onClick={pedido.reintentar} className="link font-semibold">
            Reintentar
          </button>
        </p>
      ) : (
        <p role="status" className="flex min-h-11 items-center text-sm text-base-content/70">
          Cargando…
        </p>
      )}
      <p className="text-xs text-base-content/70">
        Si lo prendés, quien entre a tu perfil los ve y puede guardarse tus recorridos.
      </p>
      {error && (
        <p role="alert" className="text-sm text-error">
          {error}
        </p>
      )}
    </fieldset>
  )
}
