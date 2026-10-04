import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Campo, CampoDeContrasenia } from '../components/Campo'
import { apiClient } from '../api/client'
import { useTitulo } from '../hooks/useTitulo'

/**
 * Both halves of the recovery flow on one screen: ask for a code, then use it.
 * Splitting them across routes would mean re-typing the email, and a reload would
 * lose the only thing tying the two steps together.
 */
export function ForgotPassword() {
  const navigate = useNavigate()
  const [step, setStep] = useState<'request' | 'reset'>('request')
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const campoDelCodigo = useRef<HTMLInputElement>(null)
  const formulario = useRef<HTMLFormElement>(null)
  useTitulo('Recuperar contraseña')

  // Al pasar al segundo paso, el botón que se tocó desaparece y el foco quedaba en la
  // nada. Va al campo del código, que es lo próximo que hay que llenar.
  useEffect(() => {
    if (step === 'reset') campoDelCodigo.current?.focus()
  }, [step])

  async function onRequest(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const { data } = await apiClient.post('/auth/forgot-password', { email })
      setNotice(data.message)
      // Always moves on, even for an address with no account: stopping here for
      // unknown emails would turn this screen into a way to find out who is registered.
      setStep('reset')
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'No pudimos mandarte el código. Probá de nuevo en un rato.')
    } finally {
      setSubmitting(false)
    }
  }

  async function onReset(e: FormEvent) {
    e.preventDefault()
    // Como en la activación de la cuenta: el botón siempre activo, y si falta algo se
    // dice qué y se va a ese campo.
    if (code.length !== 6) {
      setError('Escribí los 6 dígitos del código que te mandamos.')
      campoDelCodigo.current?.focus()
      return
    }
    if (newPassword.length < 8) {
      setError('La contraseña nueva necesita al menos 8 caracteres.')
      formulario.current?.querySelector<HTMLInputElement>('input[autocomplete="new-password"]')?.focus()
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await apiClient.post('/auth/reset-password', { email, code, newPassword })
      navigate('/login', {
        state: { notice: 'Listo, ya podés iniciar sesión con tu contraseña nueva.' },
      })
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'No pudimos cambiar la contraseña. Revisá el código y probá de nuevo.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-sm md:p-0 md:pt-8">
      <div className="card card-border md:p-2">
        <div className="card-body gap-3">
          <h1 className="card-title font-display">Recuperar contraseña</h1>

          {step === 'request' ? (
            <>
              <p className="text-sm text-base-content/70">
                Poné tu email y te mandamos un código para elegir una contraseña nueva.
              </p>
              <form onSubmit={onRequest} className="flex flex-col gap-3">
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
                      enterKeyHint="send"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="input input-bordered w-full focus:border-primary"
                    />
                  )}
                </Campo>
                {error && <p role="alert" className="text-sm text-error">{error}</p>}
                <button type="submit" disabled={submitting} className="btn btn-primary">
                  {submitting ? 'Enviando…' : 'Enviarme el código'}
                </button>
              </form>
            </>
          ) : (
            <>
              {/* The server message already mentions checking spam. */}
              <p role="status" className="text-sm text-base-content/70">
                {notice}
              </p>
              <form ref={formulario} onSubmit={onReset} noValidate className="flex flex-col gap-3">
                <Campo etiqueta="Código de 6 dígitos">
                  {(campo) => (
                    <input
                      {...campo}
                      ref={campoDelCodigo}
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
                <CampoDeContrasenia
                  etiqueta="Contraseña nueva"
                  ayuda="Mínimo 8 caracteres."
                  required
                  minLength={8}
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
                {error && <p role="alert" className="text-sm text-error">{error}</p>}
                <button type="submit" disabled={submitting} className="btn btn-primary">
                  {submitting ? 'Cambiando…' : 'Cambiar contraseña'}
                </button>
              </form>
            </>
          )}

          <p className="text-sm text-base-content/70">
            <Link to="/login" className="link text-primary">Volver a iniciar sesión</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
