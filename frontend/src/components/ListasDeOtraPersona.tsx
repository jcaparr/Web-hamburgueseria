import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { usePedido } from '../hooks/usePedido'
import type { BurgerJoint, SavedTour } from '../types'
import { isSessionExpired } from '../utils/errors'
import { LoadError } from './LoadError'
import { GrillaDeLocales } from './PestaniasDePerfil'
import { SavedTourCard } from './SavedTourCard'
import { AvisoVacio } from './Seccion'
import { TarjetaChicaDeLocal } from './TarjetaChicaDeLocal'

/**
 * Las guardadas y los recorridos de otra persona, en su perfil (#264). Solo llegan acá
 * si esa persona eligió mostrarlos: el servidor contesta 404 si no.
 *
 * Cada una se pide al abrir su pestaña, no con el perfil: la mayoría entra a ver las
 * reseñas, y traer dos listas que no va a mirar sería pedir de más.
 */
export function GuardadasDeOtraPersona({ username }: { username: string }) {
  const pedido = usePedido(`guardadas:${username}`, () =>
    apiClient.get<BurgerJoint[]>(`/usuarios/${username}/guardadas`).then(({ data }) => data),
  )
  if (pedido.error) return <LoadError error={pedido.error} onRetry={pedido.reintentar} />
  if (!pedido.datos) return <Cargando />
  if (pedido.datos.length === 0) {
    return <AvisoVacio>Todavía no guardó ninguna hamburguesería.</AvisoVacio>
  }
  return (
    <GrillaDeLocales>
      {pedido.datos.map((b) => (
        <li key={b.id}>
          <TarjetaChicaDeLocal id={b.id} nombre={b.name} foto={b.photoUrl} nota={b.averageScore} />
        </li>
      ))}
    </GrillaDeLocales>
  )
}

export function RecorridosDeOtraPersona({ username, soyYo }: { username: string; soyYo: boolean }) {
  const pedido = usePedido(`recorridos:${username}`, () =>
    apiClient.get<SavedTour[]>(`/usuarios/${username}/recorridos`).then(({ data }) => data),
  )
  if (pedido.error) return <LoadError error={pedido.error} onRetry={pedido.reintentar} />
  if (!pedido.datos) return <Cargando />
  if (pedido.datos.length === 0) {
    return <AvisoVacio>Todavía no guardó ningún recorrido.</AvisoVacio>
  }
  return (
    <ul className="grid gap-3 md:grid-cols-2">
      {pedido.datos.map((tour) => (
        <li key={tour.id}>
          {/* El propio ya está en tus recorridos: guardarlo de nuevo lo duplicaría. */}
          <SavedTourCard tour={tour} accion={soyYo ? undefined : <GuardarEnMisRecorridos tour={tour} />} />
        </li>
      ))}
    </ul>
  )
}

/**
 * Se guarda una copia con las mismas paradas y el mismo modo, por el mismo camino que
 * al armar uno: el servidor recalcula los kilómetros y respeta el tope de recorridos.
 * Si después la otra persona borra el suyo, tu copia sigue.
 */
function GuardarEnMisRecorridos({ tour }: { tour: SavedTour }) {
  const navigate = useNavigate()
  const [estado, setEstado] = useState<'listo' | 'guardando' | 'guardado'>('listo')
  const [error, setError] = useState<string | null>(null)

  function guardar() {
    setEstado('guardando')
    setError(null)
    apiClient
      .post('/tours', { paradas: tour.paradas.map((p) => p.local.id), modo: tour.modo })
      .then(() => setEstado('guardado'))
      .catch((err) => {
        if (isSessionExpired(err)) {
          navigate('/login')
          return
        }
        setEstado('listo')
        // El servidor dice por qué: por ejemplo, que llegaste al tope de recorridos.
        setError(err.response?.data?.error ?? 'No pudimos guardarlo. Probá de nuevo.')
      })
  }

  return (
    <div className="flex flex-col gap-1.5">
      <button
        type="button"
        onClick={guardar}
        disabled={estado !== 'listo'}
        className="btn btn-outline h-11 min-h-11"
      >
        {estado === 'guardado' ? 'Guardado en tus recorridos' : estado === 'guardando' ? 'Guardando…' : 'Guardar en mis recorridos'}
        <span className="sr-only"> {tour.name}</span>
      </button>
      {error && (
        <p role="alert" className="text-sm text-error">
          {error}
        </p>
      )}
    </div>
  )
}

function Cargando() {
  return (
    <p role="status" className="text-sm text-base-content/70">
      Cargando…
    </p>
  )
}
