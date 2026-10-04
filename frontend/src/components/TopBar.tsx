import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
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
      className={`sticky top-0 z-20 transition-colors duration-300 ${hidden ? 'bg-base-100/85 backdrop-blur-md' : 'bg-base-100'}`}
    >
      <header className="flex items-center justify-between gap-2 px-4 py-2 md:px-6">
        <NavLink to="/" end className="flex min-h-11 min-w-0 items-center gap-1.5 rounded-field sm:gap-2.5">
          <span
            aria-hidden="true"
            className="flex h-8 w-8 flex-none items-center justify-center rounded-xl bg-neutral font-display text-base font-extrabold text-secondary"
          >
            H
          </span>
          {/* Sin sesión, en los teléfonos de menos de 360 px los dos botones no dejan
              lugar para el nombre, y cortado ("Hamburgu…") se ve peor que no estar: queda
              el logo, y el nombre sigue ahí para los lectores de pantalla. */}
          <span
            className={`truncate font-display text-base font-bold sm:text-lg ${user ? '' : 'max-[359px]:sr-only'}`}
          >
            Hamburgueserías
          </span>
        </NavLink>

        <nav aria-label="Principal" className="hidden flex-1 justify-center md:flex">
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
        </nav>

        {/* Botones de alto completo (44 px): son de lo que más se toca, y en la barra
            de arriba, lejos del pulgar, un blanco chico se erra más. Las palabras cortas
            son para el teléfono angosto: es la misma acción con su nombre entero. */}
        <div className="flex flex-none items-center gap-1 text-sm sm:gap-2">
          {user ? (
            <>
              <span className="hidden font-medium sm:inline">@{user.username}</span>
              <button type="button" onClick={handleLogout} className="btn btn-ghost px-3">
                <span className="sm:hidden">Salir</span>
                <span className="hidden sm:inline">Cerrar sesión</span>
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className="btn btn-ghost px-2.5 sm:px-3">
                <span className="sm:hidden">Entrar</span>
                <span className="hidden sm:inline">Iniciar sesión</span>
              </NavLink>
              <NavLink to="/register" className="btn btn-primary px-2.5 sm:px-3">
                Crear cuenta
              </NavLink>
            </>
          )}
        </div>
      </header>
      <div className="checker-strip" />
    </div>
  )
}
