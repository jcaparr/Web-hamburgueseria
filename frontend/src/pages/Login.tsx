import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { GoogleSignInButton } from '../components/GoogleSignInButton'
import { PasoNombreDeGoogle } from '../components/PasoNombreDeGoogle'
import { useAuth } from '../context/AuthContext'
import { useGoogleSignIn } from '../hooks/useGoogleSignIn'

export function Login() {
  const { login } = useAuth()
  const google = useGoogleSignIn()
  const { onCredential, googleError, setGoogleError } = google
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

  // Entrar con Google la primera vez crea la cuenta, y para eso falta elegir el nombre.
  if (google.pendiente) return <PasoNombreDeGoogle google={google} />

  return (
    <div className="flex min-h-[70dvh] flex-col justify-center gap-4 p-4 md:mx-auto md:block md:min-h-0 md:max-w-sm md:p-0 md:pt-8">
      <div className="card card-border md:p-2">
        <div className="card-body gap-3">
          <h1 className="card-title font-display">Iniciar sesión</h1>
          <form onSubmit={onSubmit} className="flex flex-col gap-3">
            {/* En el teléfono estas cuatro cosas son la diferencia entre entrar de una
                y pelearse con el teclado. Sin autoComplete el gestor de contraseñas no
                ofrece el mail, y no lo ofrece aunque la contraseña sí esté anotada:
                necesita el par. Sin autoCapitalize, iOS escribe "Juan@..." con mayúscula
                y el login falla sin que se vea por qué. Y enterKeyHint cambia la tecla
                del teclado, que es cómo se avanza en un formulario desde el celular. */}
            <input
              type="email"
              required
              autoComplete="email"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              enterKeyHint="next"
              aria-label="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Email"
              className="input input-bordered focus:border-primary"
            />
            <input
              type="password"
              required
              autoComplete="current-password"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              enterKeyHint="go"
              aria-label="Contraseña"
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

          {googleError && <p className="text-xs text-error">{googleError}</p>}
          <GoogleSignInButton onCredential={onCredential} onError={setGoogleError} text="signin_with" />

          <p className="text-sm text-base-content/70">
            <Link to="/forgot-password" className="link text-primary">
              ¿Olvidaste tu contraseña?
            </Link>
          </p>
          <p className="text-sm text-base-content/70">
            ¿No tenés cuenta? <Link to="/register" className="link text-primary">Registrate</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
