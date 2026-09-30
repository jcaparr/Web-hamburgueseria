import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useHideOnScroll } from '../hooks/useScrollDirection'

const NAV_LINKS = [
  { to: '/', label: 'Explorar' },
  // El feed pide sesión: sin ella no hay a quién seguir y la pestaña "Siguiendo" no
  // significa nada, así que aparece recién cuando hay alguien adentro.
  { to: '/feed', label: 'Feed', soloConSesion: true },
  { to: '/tour', label: 'Tour' },
  { to: '/ranking', label: 'Ranking' },
  { to: '/profile', label: 'Perfil' },
]

export function TopBar() {
  const { user, logout } = useAuth()
  const links = NAV_LINKS.filter((link) => user || !link.soloConSesion)
  const navigate = useNavigate()
  const hidden = useHideOnScroll()

  async function handleLogout() {
    // Navigate first. Clearing the user while a private page is still mounted lets
    // RequireAuth fire and send the person to /login, so logging out would land
    // somewhere different depending on which page you were on. The revocation below
    // happens either way.
    navigate('/')
    await logout()
  }

  return (
    <div
      className={`sticky top-0 z-20 transition-colors duration-300 ${hidden ? 'bg-base-100/40 backdrop-blur-md' : 'bg-base-100'}`}
    >
      <header className="flex items-center justify-between gap-2 px-4 py-2 md:px-6">
        <div className="flex min-w-0 items-center gap-2.5">
          <div className="flex h-8 w-8 flex-none items-center justify-center rounded-xl bg-neutral">
            <span className="font-display text-base font-extrabold text-secondary">H</span>
          </div>
          <NavLink to="/" className="truncate font-display text-lg font-bold" end>
            Hamburgueserías
          </NavLink>
        </div>

        <div className="hidden flex-1 justify-center md:flex">
          <ul className="flex items-center gap-1 rounded-full border border-base-300/70 bg-base-100/60 p-1">
            {links.map((link) => (
              <li key={link.to}>
                <NavLink
                  to={link.to}
                  end={link.to === '/'}
                  className={({ isActive }) =>
                    `rounded-full px-5 py-2 text-sm font-semibold transition-colors ${
                      isActive ? 'bg-neutral text-secondary' : 'text-base-content hover:text-primary'
                    }`
                  }
                >
                  {link.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </div>

        <div className="flex flex-none items-center gap-2 text-sm">
          {user ? (
            <>
              <span className="hidden font-medium sm:inline">@{user.username}</span>
              <button onClick={handleLogout} className="btn btn-ghost btn-sm">
                Salir
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className="btn btn-ghost btn-sm hidden sm:inline-flex">
                Iniciar sesión
              </NavLink>
              <NavLink to="/register" className="btn btn-primary btn-sm">
                Registrarse
              </NavLink>
            </>
          )}
        </div>
      </header>
      <div className="checker-strip" />
    </div>
  )
}
