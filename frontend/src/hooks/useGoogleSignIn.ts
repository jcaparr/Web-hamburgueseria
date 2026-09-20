import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

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

  async function onCredential(credential: string) {
    setError(null)
    try {
      await loginWithGoogle(credential)
      navigate('/')
    } catch (err: any) {
      const data = err.response?.data

      // WRONG_SIGN_IN_METHOD means the address has a password account. The server's
      // message already says what to do, so it is shown as-is rather than replaced
      // by a generic failure.
      setError(data?.error ?? 'No pudimos iniciar sesión con Google')
    }
  }

  return { onCredential, googleError: error, setGoogleError: setError }
}
