import { NavLink } from 'react-router-dom'

const ITEMS = [
  { to: '/', label: 'Explorar', icon: '🔍' },
  { to: '/ranking', label: 'Ranking', icon: '🏆' },
  { to: '/deseados', label: 'Deseados', icon: '❤️' },
]

export function BottomNav() {
  return (
    <nav className="sticky bottom-0 border-t border-neutral-200 bg-white">
      <ul className="flex justify-around py-2">
        {ITEMS.map((item) => (
          <li key={item.to}>
            <NavLink
              to={item.to}
              className={({ isActive }) =>
                `flex flex-col items-center gap-0.5 px-3 py-1 text-xs ${
                  isActive ? 'text-amber-600 font-semibold' : 'text-neutral-500'
                }`
              }
            >
              <span className="text-lg">{item.icon}</span>
              {item.label}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}
