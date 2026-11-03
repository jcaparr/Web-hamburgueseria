import { Suspense, lazy, useEffect } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { leerVistaGuardada } from '../utils/vistaDeExplorar'
import { Explore } from './Explore'

// MapLibre pesa unos 250 KB: se baja recién al elegir el mapa. La lista no carga nada de esto (#395).
const Mapa = lazy(() => import('./Mapa').then((m) => ({ default: m.Mapa })))

/** Marca de que ya se aplicó la vista recordada en esta pestaña: se aplica al abrir, y solo ahí. */
const CLAVE_APLICADA = 'explorar.vistaAplicada'

function yaSeAplico(): boolean {
  try {
    return window.sessionStorage.getItem(CLAVE_APLICADA) === '1'
  } catch {
    return true
  }
}

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
 * Al abrir la app en la raíz sin nada más en la dirección, si la última vez se eligió el mapa se
 * abre el mapa. Eso pasa una sola vez por pestaña: después, volver a la raíz (el botón de atrás, el
 * logo) lleva a la lista, y el selector es lo que cambia de vista. Una búsqueda o un filtro en la
 * dirección (por ejemplo "/?q=burger") manda a la lista: quien llega con eso quiere la lista.
 */
export function EntradaDeExplorar() {
  const [params] = useSearchParams()
  const aplicada = yaSeAplico()

  useEffect(() => {
    try {
      window.sessionStorage.setItem(CLAVE_APLICADA, '1')
    } catch {
      // Sin almacenamiento se abre en la lista, sin recordar la vista.
    }
  }, [])

  if (params.get('vista') === 'mapa') {
    return (
      <Suspense fallback={<CargandoElMapa />}>
        <Mapa />
      </Suspense>
    )
  }
  if (!aplicada && params.toString() === '' && leerVistaGuardada() === 'mapa') {
    return <Navigate to="/?vista=mapa" replace />
  }
  return <Explore />
}
