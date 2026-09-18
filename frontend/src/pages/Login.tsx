import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await login(email, password)
      navigate('/')
    } catch {
      setError('Email o contraseña incorrectos')
    } finally {
      setSubmitting(false)
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
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Contraseña"
              className="input input-bordered focus:border-primary"
            />
            {error && <p className="text-xs text-error">{error}</p>}
            <button type="submit" disabled={submitting} className="btn btn-primary">
              {submitting ? 'Ingresando...' : 'Ingresar'}
            </button>
          </form>
          <p className="text-sm text-base-content/60">
            ¿No tenés cuenta? <Link to="/register" className="link text-primary">Registrate</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
