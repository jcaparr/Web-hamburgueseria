import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { useReseniaRapida } from '../context/useReseniaRapida'
import { useHideOnScroll } from '../hooks/useScrollDirection'
import { BotonDeLogros } from './BotonDeLogros'
import { CampanaDeNotificaciones } from './CampanaDeNotificaciones'
import { MarcaBurgometro } from './MarcaBurgometro'
import { IconPencil } from './icons'
import { PanelDeAjustes } from './PanelDeAjustes'

const NAV_LINKS = [
  { to: '/', label: 'Explorar' },
  // Tour ya no está: es el botón "Recorrido" de Explorar (#337), igual que en el celular.
  { to: '/ranking', label: 'Ranking' },
  // El feed pide sesión: sin ella no hay a quién seguir y la pestaña "Siguiendo" no
  // significa nada, así que aparece recién cuando hay alguien adentro.
  { to: '/feed', label: 'Feed', soloConSesion: true },
  { to: '/profile', label: 'Perfil' },
]

export function TopBar() {
  const { user } = useAuth()
  const links = NAV_LINKS.filter((link) => user || !link.soloConSesion)
  const hidden = useHideOnScroll()
  const { abrir } = useReseniaRapida()

  return (
    <div
      className={`sticky top-0 z-20 transition-colors duration-300 ${hidden ? 'bg-base-200/85 backdrop-blur-md' : 'bg-base-200'}`}
    >
      <header className="flex items-center justify-between gap-1 py-2 pl-3 pr-1.5 sm:gap-2 sm:px-4 md:px-6">
        <NavLink to="/" end className="flex min-h-11 min-w-0 items-center gap-1.5 rounded-field sm:gap-2">
          <MarcaBurgometro className="h-[29px] w-[34px] flex-none sm:h-8 sm:w-[38px]" />
          {/* Con mayúscula y en Lexend, como en el logo nuevo (#255): es lo que tiene
              que reconocerse igual en la barra, en la pestaña y en un mail.

              Sin sesión, en los teléfonos de menos de 408 px los tres botones (con la tuerca) no dejan
              lugar para el nombre, y cortado ("burgóme…") se ve peor que no estar: queda
              el dibujo, y el nombre sigue ahí para los lectores de pantalla. */}
          <span
            className={`font-logo truncate text-lg tracking-tight sm:text-xl lg:text-2xl ${user ? '' : 'max-[407px]:sr-only'}`}
          >
            Burgómetro
          </span>
        </NavLink>

        {/* Cada opción es un blanco de 44 px de alto con aire a los costados (#244): lo
            que se toca es toda la píldora, no solo la palabra, así que la letra puede
            quedar en 14 px sin que cueste acertarle. */}
        <nav aria-label="Principal" className="hidden flex-1 items-center justify-center gap-3 md:flex">
          <ul className="flex items-center gap-1 rounded-full bg-base-100 p-1 shadow-[var(--sombra-tarjeta)]">
            {links.map((link) => (
              <li key={link.to}>
                <NavLink
                  to={link.to}
                  end={link.to === '/'}
                  data-destino-xp={link.to === '/profile' ? '' : undefined}
                  className={({ isActive }) =>
                    `font-nav flex min-h-11 items-center rounded-full px-5 text-sm font-semibold transition-colors lg:px-7 ${
                      isActive ? 'bg-neutral text-secondary' : 'text-base-content hover:text-primary'
                    }`
                  }
                >
                  {link.label}
                </NavLink>
              </li>
            ))}
          </ul>
          {/* Reseñar desde cualquier pantalla (#337), igual que el lápiz del celular. Va junto
              al menú y no junto a la campana y los logros, que quedarían apretados. */}
          <button type="button" onClick={abrir} aria-haspopup="dialog" className="btn btn-primary h-11 min-h-11 gap-2 rounded-full px-5">
            <IconPencil size={18} />
            Escribir reseña
          </button>
        </nav>

        {/* Botones de alto completo (44 px): son de lo que más se toca, y en la barra
            de arriba, lejos del pulgar, un blanco chico se erra más. Las palabras cortas
            son para el teléfono angosto: es la misma acción con su nombre entero. */}
        <div className="flex flex-none items-center gap-1 text-sm sm:gap-2">
          {user ? (
            <>
              {/* La campana del buzón (#210): arriba y no en la barra de abajo del
                  teléfono, que ya tiene cinco botones. */}
              <BotonDeLogros />
              <CampanaDeNotificaciones />
              <PanelDeAjustes />
            </>
          ) : (
            <>
              {/* El tema también se cambia sin cuenta (#270). */}
              <PanelDeAjustes />
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
