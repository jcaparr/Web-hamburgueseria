import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { GoogleSignInButton } from '../components/GoogleSignInButton'
import { useAuth } from '../context/AuthContext'

export function Login() {
  const { login, loginWithGoogle } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as { notice?: string } | null

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(state?.notice ?? null)
  const [submitting, setSubmitting] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    setNotice(null)
    try {
      await login(email, password)
      navigate('/')
    } catch (err: any) {
      const data = err.response?.data

      // The password was right but the account was never activated. The server has
      // already sent a fresh code, so send them straight to the screen to enter it.
      if (data?.code === 'EMAIL_NOT_VERIFIED') {
        navigate('/verify-email', { state: { email, notice: data.error } })
        return
      }

      if (data?.code === 'RATE_LIMITED') {
        setError(data.error)
        return
      }

      setError('Email o contraseña incorrectos')
    } finally {
      setSubmitting(false)
    }
  }

  async function onGoogleCredential(credential: string) {
    setError(null)
    setNotice(null)
    try {
      await loginWithGoogle(credential)
      navigate('/')
    } catch (err: any) {
      const data = err.response?.data

      // This email already has an account here, so linking has to be confirmed by
      // the code the server just sent before Google gets control of it.
      if (data?.code === 'GOOGLE_LINK_REQUIRED') {
        navigate('/link-google', { state: { credential, notice: data.error } })
        return
      }

      setError(data?.error ?? 'No pudimos iniciar sesión con Google')
    }
  }

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-sm md:p-0 md:pt-8">
      <div className="card card-border md:p-2">
        <div className="card-body gap-3">
          <h1 className="card-title font-display">Iniciar sesión</h1>
          <form onSubmit={onSubmit} className="flex flex-col gap-3">
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Email"
              className="input input-bordered focus:border-primary"
            />
            <input
              type="password"
              required
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Contraseña"
              className="input input-bordered focus:border-primary"
            />
            {error && <p className="text-xs text-error">{error}</p>}
            {notice && <p className="text-xs text-success">{notice}</p>}
            <button type="submit" disabled={submitting} className="btn btn-primary">
              {submitting ? 'Ingresando...' : 'Ingresar'}
            </button>
          </form>

          <GoogleSignInButton onCredential={onGoogleCredential} onError={setError} />

          <p className="text-sm text-base-content/60">
            <Link to="/forgot-password" className="link text-primary">
              ¿Olvidaste tu contraseña?
            </Link>
          </p>
          <p className="text-sm text-base-content/60">
            ¿No tenés cuenta? <Link to="/register" className="link text-primary">Registrate</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
