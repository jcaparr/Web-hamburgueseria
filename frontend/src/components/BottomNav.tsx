import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { useReseniaRapida } from '../context/useReseniaRapida'
import { useHideOnScroll } from '../hooks/useScrollDirection'
import { AvatarDeUsuario } from './AvatarDeUsuario'
import type { User } from '../types'
import { IconFeed, IconMedal, IconPencil, IconSearch, IconUser } from './icons'

// Tour ya no está acá: es el primer botón de la fila de filtros de Explorar (#337). En su
// lugar, al medio, el lápiz para reseñar desde cualquier pantalla.
const ANTES = [
  { to: '/', label: 'Explorar', Icon: IconSearch },
  // Igual que arriba: sin sesión el feed no tiene "Siguiendo" que mostrar.
  { to: '/feed', label: 'Feed', Icon: IconFeed, soloConSesion: true },
]
const DESPUES = [
  { to: '/ranking', label: 'Ranking', Icon: IconMedal },
  { to: '/profile', label: 'Perfil', Icon: IconUser },
]

type Item = { to: string; label: string; Icon: typeof IconSearch }

function Pestania({ to, label, Icon, user }: Item & { user: User | null }) {
  return (
    <NavLink
      to={to}
      end={to === '/'}
      className={({ isActive }) =>
        `flex flex-1 flex-col items-center gap-0.5 rounded-[1.375rem] pb-2 pt-2.5 focus-visible:-outline-offset-4 ${isActive ? 'text-primary' : 'text-base-content/70'}`
      }
    >
      {/* Un hueco fijo de 26 px que comparten las pestañas, así los íconos y los
          rótulos quedan alineados aunque el de Perfil sea la hamburguesa con su anillo. */}
      <span className="grid h-[26px] w-[26px] place-items-center" data-destino-xp={to === '/profile' ? '' : undefined}>
        {to === '/profile' && user ? (
          <AvatarDeUsuario username={user.username} hamburguesa={user.hamburguesa} xp={user.xp} sinCirculo size={18} />
        ) : (
          <Icon size={21} />
        )}
      </span>
      <span className="font-nav text-xs font-semibold">{label}</span>
    </NavLink>
  )
}

export function BottomNav() {
  const { user } = useAuth()
  const hidden = useHideOnScroll()
  const { abrir } = useReseniaRapida()
  const antes = ANTES.filter((item) => user || !item.soloConSesion)

  return (
    // Apoyada en el borde de abajo, de lado a lado (#216): cuando flotaba separada de los
    // bordes, por el hueco de abajo se veía pasar la página. Las puntas de arriba
    // redondeadas la siguen despegando de lo que tiene encima, y el relleno de abajo
    // corre la barra de gestos del teléfono sin dejar ver nada detrás.
    <nav
      aria-label="Principal"
      className={`fixed inset-x-0 bottom-0 z-20 rounded-t-[1.375rem] pb-[env(safe-area-inset-bottom)] shadow-[var(--sombra-de-la-barra)] backdrop-blur-md transition-colors duration-300 md:hidden ${
        hidden ? 'bg-base-100/80' : 'bg-base-100/95'
      }`}
    >
      {/* Cada pestaña ocupa su columna entera, de borde a borde y de arriba abajo: antes
          el blanco era el ícono con su palabra, unos 25 px de ancho, y entre una y otra
          quedaba un hueco que no hacía nada. */}
      <div className="flex px-1">
        {antes.map((item) => (
          <Pestania key={item.to} {...item} user={user} />
        ))}
        {/* Sin sesión Feed no está: un hueco del mismo ancho en su lugar deja el lápiz en el
            centro y cada pestaña donde siempre. */}
        {antes.length < ANTES.length && <div aria-hidden="true" className="flex-1" />}
        {/* El lápiz (#337): reseñar desde cualquier pantalla. Es lo único de la barra que
            no es una pantalla sino una acción, y por eso se destaca: 52 px y color
            primario, centrado con los demás íconos. Sin sesión lleva a entrar y vuelve. */}
        <div className="flex flex-none items-center justify-center px-1">
          <button
            type="button"
            onClick={abrir}
            aria-label="Escribir una reseña"
            aria-haspopup="dialog"
            className="grid h-[52px] w-[52px] place-items-center rounded-[1.125rem] bg-primary text-primary-content shadow-[var(--sombra-de-la-barra)] transition-transform active:scale-95 motion-reduce:transition-none"
          >
            <IconPencil size={24} />
          </button>
        </div>
        {DESPUES.map((item) => (
          <Pestania key={item.to} {...item} user={user} />
        ))}
      </div>
    </nav>
  )
}
