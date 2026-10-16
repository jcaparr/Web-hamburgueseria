import { useState } from 'react'
import { apiClient } from '../api/client'
import { usePedido } from '../hooks/usePedido'
import { IconLock } from './icons'
import { Interruptor } from './Interruptor'

/**
 * Si tus guardadas y tus recorridos se ven en tu perfil público (#264).
 *
 * Se muestra como "Perfil privado" (#270): prendido es lo contrario de lo que guarda el
 * servidor. Las cuentas nuevas arrancan públicas, y el interruptor vive en el panel de la
 * tuerca de la barra (PanelDeAjustes), a un toque desde cualquier pantalla. Uno solo para las dos
 * listas y no uno por lista: es la misma pregunta —"¿muestro lo que pienso hacer?"— y
 * dos interruptores pedían pensar dos veces lo mismo.
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
    // Sin deshabilitar el campo: uno deshabilitado le saca el foco a quien usa teclado.
    if (guardando) return
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

  // Una sola fila de 44 px, sin título ni texto de ayuda.
  return (
    <fieldset className="flex flex-col px-2">
      {pedido.datos !== undefined ? (
        // Prendido = privado: es lo contrario de lo que guarda el servidor ("muestra").
        <Interruptor activo={!pedido.datos} onCambiar={(privado) => cambiar(!privado)} className="gap-2 text-base-content/80">
          <span className="flex items-center gap-1.5">
            <IconLock size={14} abierto={pedido.datos} />
            Perfil privado
          </span>
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
      {error && (
        <p role="alert" className="text-sm text-error">
          {error}
        </p>
      )}
    </fieldset>
  )
}
