import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { useHideOnScroll } from '../hooks/useScrollDirection'
import { IconFeed, IconMedal, IconRoute, IconSearch, IconUser } from './icons'

const ITEMS = [
  { to: '/', label: 'Explorar', Icon: IconSearch },
  // Igual que arriba: sin sesión el feed no tiene "Siguiendo" que mostrar, y sacarlo
  // deja la barra en cuatro, que es lo que entra cómodo en un teléfono angosto.
  { to: '/feed', label: 'Feed', Icon: IconFeed, soloConSesion: true },
  { to: '/tour', label: 'Tour', Icon: IconRoute },
  { to: '/ranking', label: 'Ranking', Icon: IconMedal },
  { to: '/profile', label: 'Perfil', Icon: IconUser },
]

export function BottomNav() {
  const { user } = useAuth()
  const hidden = useHideOnScroll()
  const items = ITEMS.filter((item) => user || !item.soloConSesion)

  return (
    // Flota sobre la página, separada de los bordes: antes era un bloque marrón de borde
    // a borde que pesaba más que todo lo que había arriba. La distancia al borde de
    // abajo respeta la barra de gestos del teléfono.
    <nav
      aria-label="Principal"
      className="fixed inset-x-0 bottom-0 z-20 px-3 pb-[max(0.625rem,env(safe-area-inset-bottom))] md:hidden"
    >
      {/* Cada pestaña ocupa su columna entera, de borde a borde y de arriba abajo: antes
          el blanco era el ícono con su palabra, unos 25 px de ancho, y entre una y otra
          quedaba un hueco que no hacía nada. */}
      <div
        className={`flex rounded-[1.375rem] shadow-[var(--sombra-alzada)] backdrop-blur-md transition-colors duration-300 ${
          hidden ? 'bg-base-100/80' : 'bg-base-100/95'
        }`}
      >
        {items.map(({ to, label, Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              `flex flex-1 flex-col items-center gap-0.5 rounded-[1.375rem] pb-2 pt-2.5 focus-visible:-outline-offset-4 ${isActive ? 'text-primary' : 'text-base-content/70'}`
            }
          >
            <Icon size={21} />
            <span className="text-xs font-semibold">{label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  )
}
