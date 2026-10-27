import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ModalDeResenia } from '../components/ModalDeResenia'
import { ReseniaRapidaContext } from './resenia'
import { useAuth } from './useAuth'

/** En la dirección, para que al volver de iniciar sesión la pantalla se abra sola. */
const PARAMETRO = 'resenar'

/**
 * La pantalla de reseña que abre el lápiz de la barra (#337), disponible en toda la app.
 *
 * Sin sesión el lápiz lleva a entrar y vuelve a donde estaba con `?resenar=1`; apenas hay
 * sesión, ese parámetro abre la pantalla y se saca de la dirección.
 *
 * Al publicar lleva a la ficha del local: es donde se ve la reseña recién escrita.
 */
export function ReseniaRapidaProvider({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [abierto, setAbierto] = useState(false)
  // Se cambia cada vez que se abre, así la pantalla arranca de cero.
  const [vez, setVez] = useState(0)

  const abrir = useCallback(() => {
    if (!user) {
      // Desde una pantalla de entrar o crear cuenta no se vuelve a ella: se vuelve al inicio.
      const estaEntrando = /^\/(login|register|verify-email|forgot-password)/.test(location.pathname)
      const destino = new URLSearchParams(estaEntrando ? '' : location.search)
      destino.set(PARAMETRO, '1')
      navigate('/login', { state: { volverA: `${estaEntrando ? '/' : location.pathname}?${destino.toString()}` } })
      return
    }
    setVez((n) => n + 1)
    setAbierto(true)
  }, [user, navigate, location.pathname, location.search])

  // Abierta por el enlace mientras el parámetro esté y haya sesión: se deduce en vez de
  // prenderla desde un efecto (#109). Al cerrar se saca de la dirección.
  const abiertaPorEnlace = !loading && user !== null && new URLSearchParams(location.search).has(PARAMETRO)

  function cerrar() {
    setAbierto(false)
    const parametros = new URLSearchParams(location.search)
    if (!parametros.has(PARAMETRO)) return
    parametros.delete(PARAMETRO)
    const resto = parametros.toString()
    navigate(`${location.pathname}${resto ? `?${resto}` : ''}`, { replace: true })
  }

  const valor = useMemo(() => ({ abrir }), [abrir])

  return (
    <ReseniaRapidaContext.Provider value={valor}>
      {children}
      {user && (
        <ModalDeResenia
          key={vez}
          abierto={abierto || abiertaPorEnlace}
          miResenia={null}
          onCerrar={cerrar}
          onGuardada={(localId) => navigate(`/burger-joints/${localId}`)}
          onSesionVencida={() => navigate('/login')}
        />
      )}
    </ReseniaRapidaContext.Provider>
  )
}
