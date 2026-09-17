import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const NAV_LINKS = [
  { to: '/', label: 'Explorar' },
  { to: '/ranking', label: 'Ranking' },
  { to: '/wishlist', label: 'Deseados' },
]

export function TopBar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <header className="navbar border-b border-base-300 px-4 md:px-6">
      <div className="navbar-start">
        <NavLink to="/" className="text-sm font-semibold" end>
          🍔 Hamburgueserías
        </NavLink>
      </div>

      <div className="navbar-center hidden md:flex">
        <ul className="menu menu-horizontal gap-1 px-1">
          {NAV_LINKS.map((link) => (
            <li key={link.to}>
              <NavLink
                to={link.to}
                end={link.to === '/'}
                className={({ isActive }) => (isActive ? 'menu-active' : '')}
              >
                {link.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </div>

      <div className="navbar-end gap-2 text-sm">
        {user ? (
          <>
            <span className="hidden font-medium sm:inline">{user.name}</span>
            <button onClick={handleLogout} className="btn btn-ghost btn-sm">
              Salir
            </button>
          </>
        ) : (
          <>
            <NavLink to="/login" className="btn btn-ghost btn-sm">
              Iniciar sesión
            </NavLink>
            <NavLink to="/register" className="btn btn-primary btn-sm">
              Registrarse
            </NavLink>
          </>
        )}
      </div>
    </header>
  )
}
