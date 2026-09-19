import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

/**
 * Confirms linking Google to an account that already existed with the same email.
 *
 * Google proving the address is not enough on its own: the account here may have
 * been created and used long before, so we ask for a code sent to that address
 * before handing the Google account control of it.
 */
export function LinkGoogle() {
  const { linkGoogle } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as { credential?: string; notice?: string } | null

  const [credential] = useState(state?.credential ?? '')
  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // The credential only arrives through the sign-in flow. Opening this page directly
  // leaves nothing to confirm.
  useEffect(() => {
    if (!credential) {
      navigate('/login', { replace: true })
    }
  }, [credential, navigate])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await linkGoogle(credential, code)
      navigate('/')
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'No pudimos vincular la cuenta')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-sm md:p-0 md:pt-8">
      <div className="card card-border md:p-2">
        <div className="card-body gap-3">
          <h1 className="card-title font-display">Vincular con Google</h1>
          <p className="text-sm text-base-content/60">
            {state?.notice ??
              'Ya tenés una cuenta con ese email. Te mandamos un código para vincularla.'}
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
            <button
              type="submit"
              disabled={submitting || code.length !== 6}
              className="btn btn-primary"
            >
              {submitting ? 'Vinculando...' : 'Vincular cuentas'}
            </button>
          </form>

          <p className="text-sm text-base-content/60">
            <Link to="/login" className="link text-primary">
              Mejor entro con mi contraseña
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
