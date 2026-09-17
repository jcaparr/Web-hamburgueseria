import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function Registro() {
  const { registrar } = useAuth()
  const navigate = useNavigate()
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      await registrar(nombre, email, password)
      navigate('/')
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'No pudimos crear tu cuenta')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="flex flex-col gap-4 p-4">
      <h1 className="text-xl font-semibold">Crear cuenta</h1>
      <form onSubmit={onSubmit} className="flex flex-col gap-3">
        <input
          required
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          placeholder="Nombre"
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-amber-500"
        />
        <input
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="Email"
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-amber-500"
        />
        <input
          type="password"
          required
          minLength={8}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Contraseña (mínimo 8 caracteres)"
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-amber-500"
        />
        {error && <p className="text-xs text-red-600">{error}</p>}
        <button
          type="submit"
          disabled={enviando}
          className="rounded-full bg-amber-500 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {enviando ? 'Creando...' : 'Crear cuenta'}
        </button>
      </form>
      <p className="text-sm text-neutral-500">
        ¿Ya tenés cuenta? <Link to="/login" className="text-amber-600">Iniciá sesión</Link>
      </p>
    </div>
  )
}
