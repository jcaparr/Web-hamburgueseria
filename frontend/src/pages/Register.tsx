import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { CampoNombreDeUsuario } from '../components/CampoNombreDeUsuario'
import { GoogleSignInButton } from '../components/GoogleSignInButton'
import { PasoNombreDeGoogle } from '../components/PasoNombreDeGoogle'
import { useAuth } from '../context/AuthContext'
import { useGoogleSignIn } from '../hooks/useGoogleSignIn'
import { useNombreDeUsuario } from '../hooks/useNombreDeUsuario'

export function Register() {
  const { register } = useAuth()
  const google = useGoogleSignIn()
  const { onCredential, googleError, setGoogleError } = google
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const username = useNombreDeUsuario()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await register(name, username.valor, email, password)
      // No session yet: the account is not usable until the emailed code is entered.
      // The message the server returns is not passed along: the verification screen
      // already says the same thing, with the address filled in.
      navigate('/verify-email', { state: { email } })
    } catch (err: any) {
      // Si el problema es el nombre de usuario, queda marcado en ese campo: es donde
      // se arregla, y un cartel arriba no diría dónde mirar.
      if (!username.rechazar(err)) {
        setError(err.response?.data?.error ?? 'No pudimos crear tu cuenta')
      }
    } finally {
      setSubmitting(false)
    }
  }

  // Entró con Google y es la primera vez: falta que elija cómo lo van a encontrar.
  if (google.pendiente) return <PasoNombreDeGoogle google={google} />

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-sm md:p-0 md:pt-8">
      <div className="card card-border md:p-2">
        <div className="card-body gap-3">
          <h1 className="card-title font-display">Crear cuenta</h1>
          <form onSubmit={onSubmit} className="flex flex-col gap-3">
            <input
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Nombre"
              className="input input-bordered focus:border-primary"
            />
            <CampoNombreDeUsuario campo={username} />
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
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Contraseña (mínimo 8 caracteres)"
              className="input input-bordered focus:border-primary"
            />
            {error && <p className="text-xs text-error">{error}</p>}
            <button type="submit" disabled={submitting} className="btn btn-primary">
              {submitting ? 'Creando...' : 'Crear cuenta'}
            </button>
          </form>

          {/* Same endpoint as on the login screen: Google users skip the emailed code
              entirely, because Google has already verified the address. */}
          {googleError && <p className="text-xs text-error">{googleError}</p>}
          <GoogleSignInButton
            onCredential={onCredential}
            onError={setGoogleError}
            text="signup_with"
          />
          <p className="text-sm text-base-content/60">
            ¿Ya tenés cuenta? <Link to="/login" className="link text-primary">Iniciá sesión</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
