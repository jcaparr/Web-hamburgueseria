import { NavLink } from 'react-router-dom'
import { useHideOnScroll } from '../hooks/useScrollDirection'
import { IconMedal, IconSearch, IconUser } from './icons'

const ITEMS = [
  { to: '/', label: 'Explorar', Icon: IconSearch },
  { to: '/ranking', label: 'Ranking', Icon: IconMedal },
  { to: '/profile', label: 'Perfil', Icon: IconUser },
]

export function BottomNav() {
  const hidden = useHideOnScroll()

  return (
    <div className="fixed inset-x-0 bottom-0 z-20 md:hidden">
      <div
        className={`flex items-center justify-around pb-[max(0.75rem,env(safe-area-inset-bottom))] pt-3 transition-colors duration-300 ${
          hidden ? 'bg-neutral/40 backdrop-blur-md' : 'bg-neutral'
        }`}
      >
        {ITEMS.map(({ to, label, Icon }) => (
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
