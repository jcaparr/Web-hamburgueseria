import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { useAuth } from '../context/AuthContext'

const RESEND_COOLDOWN_SECONDS = 60

/**
 * Where a new account is activated. Reached from registration, and from login when
 * someone signed up but never entered their code.
 */
export function VerifyEmail() {
  const { verifyEmail } = useAuth()
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
    setSubmitting(true)
    setError(null)
    try {
      await verifyEmail(email, code)
      navigate('/')
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'No pudimos verificar el código')
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
      setError(err.response?.data?.error ?? 'No pudimos reenviar el código')
    }
  }

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-sm md:p-0 md:pt-8">
      <div className="card card-border md:p-2">
        <div className="card-body gap-3">
          <h1 className="card-title font-display">Revisá tu email</h1>
          {/* Worded to be true either way. Someone who typed an address that already
              has an account gets no code, and the server cannot say so without
              turning this screen into a way to find out who is registered. Promising
              a code that is never coming just leaves them waiting for it. */}
          <p className="text-sm text-base-content/60">
            Te escribimos a <span className="font-medium">{email}</span>. Si es tu
            primera vez, el mail trae un código de 6 dígitos para activar la cuenta;
            si ya tenías una, te explica cómo entrar. Revisá también la carpeta de spam.
          </p>

          <form onSubmit={onSubmit} className="flex flex-col gap-3">
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
            {error && <p className="text-xs text-error">{error}</p>}
            {notice && <p className="text-xs text-base-content/60">{notice}</p>}
            <button
              type="submit"
              disabled={submitting || code.length !== 6}
              className="btn btn-primary"
            >
              {submitting ? 'Verificando...' : 'Activar cuenta'}
            </button>
          </form>

          <button
            type="button"
            onClick={onResend}
            disabled={cooldown > 0}
            className="btn btn-ghost btn-sm cursor-pointer disabled:cursor-not-allowed"
          >
            {cooldown > 0 ? `Reenviar código (${cooldown}s)` : 'Reenviar código'}
          </button>

          <p className="text-sm text-base-content/60">
            ¿Te equivocaste de email?{' '}
            <Link to="/register" className="link text-primary">Registrate de nuevo</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
