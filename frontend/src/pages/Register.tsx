import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Campo, CampoDeContrasenia } from '../components/Campo'
import { CampoNombreDeUsuario } from '../components/CampoNombreDeUsuario'
import { GoogleSignInButton } from '../components/GoogleSignInButton'
import { PasoNombreDeGoogle } from '../components/PasoNombreDeGoogle'
import { useAuth } from '../context/useAuth'
import { useTitulo } from '../hooks/useTitulo'
import { useGoogleSignIn } from '../hooks/useGoogleSignIn'
import { useNombreDeUsuario } from '../hooks/useNombreDeUsuario'

export function Register() {
  const { register } = useAuth()
  useTitulo('Crear cuenta')
  const google = useGoogleSignIn()
  const { onCredential, googleError, setGoogleError } = google
  const navigate = useNavigate()
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
      await register(username.valor, email, password)
      // No session yet: the account is not usable until the emailed code is entered.
      // The message the server returns is not passed along: the verification screen
      // already says the same thing, with the address filled in.
      navigate('/verify-email', { state: { email } })
    } catch (err: any) {
      // Si el problema es el nombre de usuario, queda marcado en ese campo: es donde
      // se arregla, y un cartel arriba no diría dónde mirar.
      if (!username.rechazar(err)) {
        setError(err.response?.data?.error ?? 'No pudimos crear tu cuenta. Probá de nuevo en un rato.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  // Entró con Google y es la primera vez: falta que elija cómo lo van a encontrar.
  if (google.pendiente) return <PasoNombreDeGoogle google={google} />

  return (
    <div className="flex min-h-[70dvh] flex-col justify-center gap-4 p-4 md:mx-auto md:block md:min-h-0 md:max-w-sm md:p-0 md:pt-8">
      <div className="card card-border md:p-2">
        <div className="card-body gap-3">
          <h1 className="card-title font-display">Crear cuenta</h1>
          <form onSubmit={onSubmit} className="flex flex-col gap-3">
            <CampoNombreDeUsuario campo={username} />
            {/* Lo mismo que en iniciar sesión, y por lo mismo: en el teléfono sin esto
                el mail sale con mayúscula inicial y el gestor de contraseñas no ofrece
                guardar el par. Acá va "new-password", que es lo que hace que ofrezca
                una contraseña nueva en vez de rellenar una vieja. */}
            <Campo etiqueta="Email">
              {(campo) => (
                <input
                  {...campo}
                  type="email"
                  required
                  autoComplete="email"
                  autoCapitalize="none"
                  autoCorrect="off"
                  spellCheck={false}
                  enterKeyHint="next"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="input input-bordered w-full focus:border-primary"
                />
              )}
            </Campo>
            <CampoDeContrasenia
              etiqueta="Contraseña"
              ayuda="Mínimo 8 caracteres."
              required
              minLength={8}
              autoComplete="new-password"
              enterKeyHint="go"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            {error && <p role="alert" className="text-sm text-error">{error}</p>}
            <button type="submit" disabled={submitting} className="btn btn-primary">
              {submitting ? 'Creando tu cuenta…' : 'Crear cuenta'}
            </button>
          </form>

          {/* Same endpoint as on the login screen: Google users skip the emailed code
              entirely, because Google has already verified the address. */}
          {googleError && <p role="alert" className="text-sm text-error">{googleError}</p>}
          <GoogleSignInButton
            onCredential={onCredential}
            onError={setGoogleError}
            text="signup_with"
          />
          <p className="text-sm text-base-content/70">
            ¿Ya tenés cuenta? <Link to="/login" className="link text-primary">Iniciá sesión</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
