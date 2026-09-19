import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'

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
      setError(err.response?.data?.error ?? 'No pudimos procesar el pedido')
    } finally {
      setSubmitting(false)
    }
  }

  async function onReset(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await apiClient.post('/auth/reset-password', { email, code, newPassword })
      navigate('/login', {
        state: { notice: 'Listo, ya podés entrar con tu nueva contraseña.' },
      })
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'No pudimos cambiar la contraseña')
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
              <p className="text-sm text-base-content/60">
                Poné tu email y te mandamos un código para elegir una contraseña nueva.
              </p>
              <form onSubmit={onRequest} className="flex flex-col gap-3">
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Email"
                  className="input input-bordered focus:border-primary"
                />
                {error && <p className="text-xs text-error">{error}</p>}
                <button type="submit" disabled={submitting} className="btn btn-primary">
                  {submitting ? 'Enviando...' : 'Mandarme el código'}
                </button>
              </form>
            </>
          ) : (
            <>
              {/* The server message already mentions checking spam. */}
              <p className="text-sm text-base-content/60">{notice}</p>
              <form onSubmit={onReset} className="flex flex-col gap-3">
                <input
                  required
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  pattern="\d{6}"
                  maxLength={6}
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                  placeholder="000000"
                  aria-label="Código de 6 dígitos"
                  className="input input-bordered text-center font-display text-2xl tracking-[0.5em] focus:border-primary"
                />
                <input
                  type="password"
                  required
                  minLength={8}
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Nueva contraseña (mínimo 8 caracteres)"
                  className="input input-bordered focus:border-primary"
                />
                {error && <p className="text-xs text-error">{error}</p>}
                <button
                  type="submit"
                  disabled={submitting || code.length !== 6}
                  className="btn btn-primary"
                >
                  {submitting ? 'Cambiando...' : 'Cambiar contraseña'}
                </button>
              </form>
            </>
          )}

          <p className="text-sm text-base-content/60">
            <Link to="/login" className="link text-primary">Volver a iniciar sesión</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
