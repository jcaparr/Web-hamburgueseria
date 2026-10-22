import { useState } from 'react'
import { Link } from 'react-router-dom'
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
 * con sesión, y sin ella quedan a mano los textos legales, que en el perfil están al
 * pie. "Cerrar sesión" no se repite acá: ya está en la barra.
 *
 * El contenido se arma recién la primera vez que se abre: el estado de privacidad y la lista de bloqueados
 * son pedidos al servidor y no tienen por qué hacerse en cada pantalla; después queda armado, así que no vuelve a decir "Cargando…".
 */
export function PanelDeAjustes() {
  const { user } = useAuth()
  const [armado, setArmado] = useState(false)

  return (
    <PanelDeBarra etiqueta="Ajustes" icono={<IconSettings size={21} />} titulo="Ajustes" angosto onCambio={(abierto) => abierto && setArmado(true)}>
      {armado && (
        <div className="flex flex-col gap-1">
          <SelectorDeTema />
          <div className="px-2">
            <h3 className="text-sm font-semibold">Seguinos</h3>
            <LinksDeRedes className="-mx-3" />
          </div>
          {user ? (
            <>
              <MostrarMisListas />
              <CuentasBloqueadas />
            </>
          ) : (
            <nav aria-label="Legales" className="flex flex-wrap gap-x-5 px-2 text-sm">
              <Link to="/terminos" className="inline-flex min-h-11 items-center rounded-field text-base-content/70 hover:text-base-content hover:underline">
                Términos y condiciones
              </Link>
              <Link to="/privacidad" className="inline-flex min-h-11 items-center rounded-field text-base-content/70 hover:text-base-content hover:underline">
                Política de privacidad
              </Link>
            </nav>
          )}
        </div>
      )}
    </PanelDeBarra>
  )
}
