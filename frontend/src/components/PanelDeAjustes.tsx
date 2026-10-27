import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { CuentasBloqueadas } from './CuentasBloqueadas'
import { IconSettings } from './icons'
import { LinksDeRedes } from './LinksDeRedes'
import { MostrarMisListas } from './MostrarMisListas'
import { PanelDeBarra } from './PanelDeBarra'
import { SelectorDeTema } from './SelectorDeTema'

/**
 * La tuerca de la barra, con lo que se cambia de a poco y no se mira todos los días:
 * el tema, si tu perfil es privado y a quiénes bloqueaste (#270).
 *
 * El tema está para todos, con o sin sesión: antes solo se podía cambiar desde el
 * perfil, y quien no tenía cuenta no podía. Lo que depende de la cuenta aparece solo
 * con sesión. Los textos legales viven en el pie de la página, no acá. "Cerrar sesión"
 * va al final del panel (#315): ya no ocupa lugar en la barra de arriba.
 *
 * El contenido se arma recién la primera vez que se abre: el estado de privacidad y la lista de bloqueados
 * son pedidos al servidor y no tienen por qué hacerse en cada pantalla; después queda armado, así que no vuelve a decir "Cargando…".
 */
export function PanelDeAjustes() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [armado, setArmado] = useState(false)

  async function cerrarSesion() {
    // Primero se navega: si se borrara la sesión con una pantalla privada montada,
    // RequireAuth mandaría a /login y se terminaría en un lugar distinto según dónde estabas.
    navigate('/')
    await logout()
  }

  return (
    <PanelDeBarra key={user ? 'con-sesion' : 'sin-sesion'} etiqueta="Ajustes" icono={<IconSettings size={21} />} titulo="Ajustes" angosto onCambio={(abierto) => abierto && setArmado(true)}>
      {armado && (
        <div className="flex flex-col gap-1">
          <SelectorDeTema />
          <div className="px-2">
            <h3 className="text-sm font-semibold">Seguinos</h3>
            <LinksDeRedes className="-mx-3" />
          </div>
          {user && (
            <>
              <MostrarMisListas />
              <CuentasBloqueadas />
              {/* Lo último y en kétchup: es lo que se hace pocas veces y no se toca sin querer. */}
              <div className="mt-1 border-t border-base-content/15 pt-1">
                <button type="button" onClick={cerrarSesion} className="btn btn-ghost w-full justify-start px-2 text-primary">
                  Cerrar sesión
                </button>
              </div>
            </>
          )}
        </div>
      )}
    </PanelDeBarra>
  )
}
