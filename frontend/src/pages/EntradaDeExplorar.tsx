import { Suspense, lazy } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Explore } from './Explore'

// MapLibre pesa unos 300 KB comprimido: se baja recién al elegir el mapa. La lista no carga nada de esto (#395).
const Mapa = lazy(() => import('./Mapa').then((m) => ({ default: m.Mapa })))

function CargandoElMapa() {
  return (
    <p role="status" className="p-6 text-center text-sm text-base-content/70">
      Cargando el mapa…
    </p>
  )
}

/**
 * La puerta de Explorar: la lista o el mapa, según "?vista=mapa" en la dirección.
 *
 * Abrir la app en la raíz abre siempre la lista (#415): no se recuerda la última vista. El mapa
 * se pide con "?vista=mapa" y el selector Lista | Mapa es lo que cambia de vista.
 */
export function EntradaDeExplorar() {
  const [params] = useSearchParams()

  if (params.get('vista') === 'mapa') {
    return (
      <Suspense fallback={<CargandoElMapa />}>
        <Mapa />
      </Suspense>
    )
  }
  return <Explore />
}
