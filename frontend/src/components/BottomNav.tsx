import { NavLink } from 'react-router-dom'
import { IconHeart, IconMedal, IconSearch } from './icons'

const ITEMS = [
  { to: '/', label: 'Explorar', Icon: IconSearch },
  { to: '/ranking', label: 'Ranking', Icon: IconMedal },
  { to: '/wishlist', label: 'Deseados', Icon: IconHeart },
]

export function BottomNav() {
  return (
    <div className="fixed inset-x-0 bottom-0 z-20 md:hidden">
      <div className="checker-strip" />
      <div className="flex items-center justify-around bg-neutral pb-[max(0.75rem,env(safe-area-inset-bottom))] pt-3">
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
