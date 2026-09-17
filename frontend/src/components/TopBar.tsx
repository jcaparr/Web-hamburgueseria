import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export function TopBar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <header className="flex items-center justify-between border-b border-neutral-200 px-4 py-3">
      <Link to="/" className="text-sm font-semibold text-neutral-800">
        🍔 Hamburgueserías
      </Link>

      {user ? (
        <div className="flex items-center gap-2 text-sm">
          <span className="font-medium text-neutral-700">{user.name}</span>
          <button onClick={handleLogout} className="text-xs text-neutral-400 hover:text-neutral-600 hover:underline">
            Salir
          </button>
        </div>
      ) : (
        <div className="flex items-center gap-2 text-sm">
          <Link to="/login" className="text-neutral-600 hover:underline">
            Iniciar sesión
          </Link>
          <Link
            to="/register"
            className="rounded-full bg-amber-500 px-3 py-1 text-xs font-medium text-white hover:bg-amber-600"
          >
            Registrarse
          </Link>
        </div>
      )}
    </header>
  )
}
