import { NavLink } from 'react-router-dom'

const ITEMS = [
  { to: '/', label: 'Explorar', icon: '🔍' },
  { to: '/ranking', label: 'Ranking', icon: '🏆' },
  { to: '/wishlist', label: 'Deseados', icon: '❤️' },
]

export function BottomNav() {
  return (
    <div className="dock md:hidden">
      {ITEMS.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === '/'}
          className={({ isActive }) => (isActive ? 'dock-active' : '')}
        >
          <span className="text-lg">{item.icon}</span>
          <span className="dock-label">{item.label}</span>
        </NavLink>
      ))}
    </div>
  )
}
