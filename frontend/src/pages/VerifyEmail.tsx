import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { Campo } from '../components/Campo'
import { useAuth } from '../context/useAuth'
import { useTitulo } from '../hooks/useTitulo'

const RESEND_COOLDOWN_SECONDS = 60

/**
 * Where a new account is activated. Reached from registration, and from login when
 * someone signed up but never entered their code.
 */
export function VerifyEmail() {
  const { verifyEmail } = useAuth()
  useTitulo('Revisá tu email')
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as { email?: string; notice?: string } | null

  const [email] = useState(state?.email ?? '')
  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(state?.notice ?? null)
  const [submitting, setSubmitting] = useState(false)
  // Matches the server's own cooldown, so the button is only enabled when a resend
  // would actually send something.
  const [cooldown, setCooldown] = useState(RESEND_COOLDOWN_SECONDS)
  const campoDelCodigo = useRef<HTMLInputElement>(null)

  // Landing here without an email means the page was opened directly; there is
  // nothing to verify and no way to ask for a code.
  useEffect(() => {
    if (!email) {
      navigate('/register', { replace: true })
    }
  }, [email, navigate])

  useEffect(() => {
    if (cooldown <= 0) return
    const timer = setTimeout(() => setCooldown((seconds) => seconds - 1), 1000)
    return () => clearTimeout(timer)
  }, [cooldown])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    // El botón queda siempre activo y el control se hace acá: deshabilitado hasta tener
    // los seis dígitos no explicaba por qué no se podía apretar.
    if (code.length !== 6) {
      setError('Escribí los 6 dígitos del código que te mandamos.')
      campoDelCodigo.current?.focus()
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await verifyEmail(email, code)
      navigate('/')
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'No pudimos verificar el código. Revisalo y probá de nuevo.')
    } finally {
      setSubmitting(false)
    }
  }

  async function onResend() {
    setError(null)
    setNotice(null)
    try {
      const { data } = await apiClient.post('/auth/resend-code', { email })
      setNotice(data.message)
      setCooldown(RESEND_COOLDOWN_SECONDS)
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'No pudimos reenviar el código. Probá de nuevo en un rato.')
    }
  }

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-sm md:p-0 md:pt-8">
      <div className="card card-border md:p-2">
        <div className="card-body gap-3">
          <h1 className="card-title font-display">Revisá tu email</h1>
          {/* Plain again: registration now refuses an address that already has an
              account, so everyone who reaches this screen really does have a code
              on the way. */}
          <p className="text-sm text-base-content/70">
            Te mandamos un código de 6 dígitos a <span className="font-medium">{email}</span>.
            Si no lo ves, fijate en la carpeta de spam.
          </p>

          <form onSubmit={onSubmit} noValidate className="flex flex-col gap-3">
            <Campo etiqueta="Código de 6 dígitos">
              {(campo) => (
                <input
                  {...campo}
                  ref={campoDelCodigo}
                  aria-invalid={error !== null && code.length !== 6}
                  required
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  pattern="\d{6}"
                  maxLength={6}
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                  placeholder="000000"
                  className="input input-bordered w-full text-center font-display text-2xl tracking-[0.5em] focus:border-primary"
                />
              )}
            </Campo>
            {error && (
              <p role="alert" className="text-sm text-error">
                {error}
              </p>
            )}
            {notice && (
              <p role="status" className="text-sm text-base-content/70">
                {notice}
              </p>
            )}
            <button type="submit" disabled={submitting} className="btn btn-primary">
              {submitting ? 'Activando…' : 'Activar cuenta'}
            </button>
          </form>

          {/* La espera en texto común y no adentro de un botón apagado, que la dejaba
              casi ilegible justo cuando es lo que hay que leer. */}
          {cooldown > 0 ? (
            <p className="text-center text-sm text-base-content/70">
              Si no te llega, vas a poder pedir otro código en {cooldown} s.
            </p>
          ) : (
            <button type="button" onClick={onResend} className="btn btn-ghost btn-sm">
              Reenviar código
            </button>
          )}

          <p className="text-sm text-base-content/70">
            ¿Te equivocaste de email?{' '}
            <Link to="/register" className="link text-primary">Volvé a crear tu cuenta</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
