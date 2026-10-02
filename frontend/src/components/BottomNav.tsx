import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
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
    <div className="fixed inset-x-0 bottom-0 z-20 md:hidden">
      <div
        className={`flex items-center justify-around pb-[max(0.75rem,env(safe-area-inset-bottom))] pt-3 transition-colors duration-300 ${
          hidden ? 'bg-neutral/85 backdrop-blur-md' : 'bg-neutral'
        }`}
      >
        {items.map(({ to, label, Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              `flex flex-col items-center gap-1 ${isActive ? 'text-secondary' : 'text-base-100/70'}`
            }
          >
            <Icon size={21} />
            <span className="text-[11px] font-semibold">{label}</span>
          </NavLink>
        ))}
      </div>
    </div>
  )
}
