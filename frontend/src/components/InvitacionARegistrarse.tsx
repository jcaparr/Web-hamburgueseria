import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { AvisoDeTerminos } from './AvisoDeTerminos'
import { GoogleSignInButton } from './GoogleSignInButton'
import { PasoNombreDeGoogle } from './PasoNombreDeGoogle'
import { useAuth } from '../context/useAuth'
import { useGoogleSignIn } from '../hooks/useGoogleSignIn'
import {
  SEGUNDOS_DE_ESPERA,
  esPantallaSinInvitacion,
  estaEnPausa,
  marcarMostradaEnEstaVisita,
  registrarCierre,
  yaSeMostroEnEstaVisita,
} from '../utils/invitacionARegistrarse'

/**
 * La hoja que invita a crear la cuenta a quien entra sin sesión (#346): sube desde abajo
 * en el teléfono y es una tarjeta en la esquina en escritorio, para no tapar la web.
 *
 * No es un formulario: el botón de Google es el de Google, y el email lleva a las
 * pantallas de siempre, así hay un solo lugar donde se arma una cuenta.
 *
 * Cuándo sale está en utils/invitacionARegistrarse. Aparece a los 5 segundos o al
 * pasar a una segunda pantalla; si se entró por un link directo a un local o una reseña,
 * solo al pasar a otra, para que primero vea lo que le mandaron. Se arma en el
 * navegador, así que los bots y las vistas previas no la ven.
 */
export function InvitacionARegistrarse() {
  const { user, loading } = useAuth()
  const { pathname, search } = useLocation()
  const [visible, setVisible] = useState(false)
  const hoja = useRef<HTMLDivElement>(null)
  const google = useGoogleSignIn(pathname + search)

  // Por dónde se entró, y si ya se pasó a otra pantalla.
  const primera = useRef<string | null>(null)
  const pantallasVistas = useRef(0)

  const elegible = !loading && !user && !esPantallaSinInvitacion(pathname)

  useEffect(() => {
    if (!elegible) return
    if (primera.current === null) primera.current = pathname
    pantallasVistas.current += 1
    // La segunda pantalla vale como pedido; la primera, solo si fue la portada.
    const pasoAOtra = pantallasVistas.current >= 2
    const entroPorLaPortada = primera.current === '/'

    if (yaSeMostroEnEstaVisita() || estaEnPausa()) return
    if (pasoAOtra) {
      setVisible(true)
      return
    }
    if (!entroPorLaPortada) return
    const espera = window.setTimeout(() => setVisible(true), SEGUNDOS_DE_ESPERA * 1000)
    return () => window.clearTimeout(espera)
  }, [elegible, pathname])

  const mostrar = visible && elegible

  // Cuenta como mostrada recién cuando se ve, no cuando se programa: si la sesión se
  // estaba comprobando, programada pero oculta no tiene que gastar la visita.
  useEffect(() => {
    if (mostrar) marcarMostradaEnEstaVisita()
  }, [mostrar])

  function cerrar() {
    registrarCierre()
    setVisible(false)
    google.cancelar()
  }

  useEffect(() => {
    if (!mostrar) return
    function conEscape(e: KeyboardEvent) {
      if (e.key === 'Escape') cerrar()
    }
    function alTocarAfuera(e: PointerEvent) {
      if (hoja.current && !hoja.current.contains(e.target as Node)) cerrar()
    }
    document.addEventListener('keydown', conEscape)
    document.addEventListener('pointerdown', alTocarAfuera)
    return () => {
      document.removeEventListener('keydown', conEscape)
      document.removeEventListener('pointerdown', alTocarAfuera)
    }
    // cerrar solo usa funciones estables y un cierre de más no pasa de una escritura.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mostrar])

  if (!mostrar) return null

  return (
    <div
      ref={hoja}
      role="dialog"
      aria-labelledby={google.pendiente ? undefined : 'invitacion-titulo'}
      aria-label={google.pendiente ? 'Elegí tu nombre de usuario' : undefined}
      // Un enlace de la hoja lleva a Crear cuenta, Iniciar sesión o los términos: ahí ya
      // no hace falta, y al volver no tiene que reaparecer. No cuenta como cierre.
      onClick={(e) => {
        if ((e.target as HTMLElement).closest('a')) setVisible(false)
      }}
      className="fixed inset-x-0 bottom-0 z-40 max-h-[90dvh] overflow-y-auto rounded-t-3xl bg-base-100 px-5 pb-[max(1rem,env(safe-area-inset-bottom))] pt-3 shadow-[0_-10px_40px_rgb(0_0_0/0.25)] md:inset-x-auto md:bottom-5 md:right-5 md:w-96 md:rounded-2xl md:p-5"
    >
      {google.pendiente ? (
        // Si eligió Google y es la primera vez, falta el nombre: ese paso va en la hoja.
        <PasoNombreDeGoogle google={google} />
      ) : (
        <>
          <div className="mx-auto mb-2 h-1.5 w-11 rounded-full bg-base-300 md:hidden" aria-hidden="true" />
          <button
            type="button"
            onClick={cerrar}
            aria-label="Cerrar"
            className="btn btn-circle btn-ghost absolute right-1 top-1 min-h-11 min-w-11 text-xl"
          >
            ×
          </button>
          <div className="flex flex-col gap-3">
            <h2 id="invitacion-titulo" className="font-display text-xl font-bold">
              Sumate a Burgómetro
            </h2>
            <p className="text-sm text-base-content/70">Guardá tus favoritas y contá qué tal estuvo.</p>
            {google.googleError && (
              <p role="alert" className="text-sm text-error">
                {google.googleError}
              </p>
            )}
            <GoogleSignInButton
              onCredential={google.onCredential}
              onError={google.setGoogleError}
              text="continue_with"
              separador={false}
            />
            <Link to="/register" className="btn btn-outline min-h-12">
              Seguir con email
            </Link>
            <AvisoDeTerminos accion="Al crear tu cuenta" />
            <p className="text-sm text-base-content/70">
              ¿Ya tenés cuenta?{' '}
              <Link to="/login" className="link text-primary">
                Iniciá sesión
              </Link>
            </p>
            <button type="button" onClick={cerrar} className="btn btn-ghost btn-sm min-h-11 self-center">
              Ahora no
            </button>
          </div>
        </>
      )}
    </div>
  )
}
