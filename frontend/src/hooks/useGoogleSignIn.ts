import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'

/**
 * Shared by the login and register screens, which are the same action as far as
 * Google is concerned: the server looks the account up and creates it only if it is
 * not there. Keeping one handler means the link-confirmation path cannot be
 * implemented on one screen and forgotten on the other.
 */
export function useGoogleSignIn() {
  const { loginWithGoogle } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  /**
   * Lo que Google devolvió, guardado mientras la persona elige su nombre.
   *
   * Es la primera vez que entra: el servidor todavía no creó nada, así que hace falta
   * volver con el mismo credential y el nombre juntos. Google los da con una hora de
   * validez, de sobra para completar un campo.
   */
  const [pendiente, setPendiente] = useState<{ credential: string; sugerencia: string } | null>(
    null,
  )

  async function onCredential(credential: string) {
    setError(null)
    try {
      await loginWithGoogle(credential)
      navigate('/')
    } catch (err: any) {
      const data = err.response?.data

      if (data?.code === 'NEEDS_USERNAME') {
        setPendiente({ credential, sugerencia: data.suggestion ?? '' })
        return
      }

      // WRONG_SIGN_IN_METHOD means the address has a password account. The server's
      // message already says what to do, so it is shown as-is rather than replaced
      // by a generic failure.
      setError(data?.error ?? 'No pudimos iniciar sesión con Google')
    }
  }

  /**
   * La segunda vuelta, ya con el nombre elegido.
   *
   * Deja salir el error en vez de guardarlo: si el nombre resultó estar tomado, eso se
   * marca en el campo y no en el cartel de arriba.
   */
  async function confirmarNombre(username: string) {
    if (!pendiente) return
    await loginWithGoogle(pendiente.credential, username)
    setPendiente(null)
    navigate('/')
  }

  function cancelar() {
    setPendiente(null)
    setError(null)
  }

  return {
    onCredential,
    confirmarNombre,
    pendiente,
    cancelar,
    googleError: error,
    setGoogleError: setError,
  }
}

export type GoogleSignIn = ReturnType<typeof useGoogleSignIn>
