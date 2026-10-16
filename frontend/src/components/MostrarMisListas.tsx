import { useEffect, useState } from 'react'
import { apiClient } from '../api/client'
import { Interruptor } from './Interruptor'

/**
 * Si tus guardadas y tus recorridos se ven en tu perfil público (#264).
 *
 * Arranca apagado: hasta ahora eran solo tuyos, y nadie aceptó mostrarlos. Uno solo para
 * las dos listas y no uno por lista: es la misma pregunta —"¿muestro lo que pienso
 * hacer?"— y dos interruptores pedían pensar dos veces lo mismo.
 *
 * Se marca enseguida y vuelve atrás si el servidor no lo acepta. Hasta saber cómo está
 * no se dibuja, para no mostrar "apagado" a quien lo tiene prendido.
 */
export function MostrarMisListas() {
  const [muestra, setMuestra] = useState<boolean | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    apiClient
      .get<{ muestra: boolean }>('/profile/muestra-listas')
      .then(({ data }) => setMuestra(data.muestra))
      .catch(() => setError('No pudimos ver si mostrás tus listas. Probá de nuevo en un rato.'))
  }, [])

  function cambiar(nuevo: boolean) {
    setMuestra(nuevo)
    setError(null)
    apiClient.put('/profile/muestra-listas', { muestra: nuevo }).catch(() => {
      setMuestra(!nuevo)
      setError('No pudimos guardar el cambio. Probá de nuevo.')
    })
  }

  return (
    <fieldset className="flex flex-col gap-1">
      <legend className="mb-1 text-sm font-semibold">Tu perfil público</legend>
      {muestra !== null && (
        <Interruptor activo={muestra} onCambiar={cambiar}>
          Mostrar mis guardadas y mis recorridos
        </Interruptor>
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
