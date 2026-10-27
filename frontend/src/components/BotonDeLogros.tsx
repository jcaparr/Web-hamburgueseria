import { useEffect, useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { apiClient } from '../api/client'
import { revisarLogros } from '../utils/avisosDeLogros'
import { IconLogros } from './icons'

/**
 * El acceso a Logros, en la barra de arriba al lado de la campana (#315).
 *
 * Lleva un punto cuando hay algún logro sin ver. El número lo trae el mismo pedido que
 * usa la campana para contar lo nuevo; si el servidor todavía no lo manda (o el pedido
 * falla), no hay punto: es lo mismo que nada nuevo, y la próxima pantalla vuelve a probar.
 */
export function BotonDeLogros() {
  const { pathname } = useLocation()
  const [sinVer, setSinVer] = useState(0)

  useEffect(() => {
    let vigente = true
    apiClient
      .get<{ logrosSinVer?: number }>('/notificaciones/nuevas')
      .then(({ data }) => {
        if (!vigente) return
        setSinVer(data.logrosSinVer ?? 0)
        // La campana trae logros sin ver: se pide el detalle para mostrar el aviso (#317).
        if ((data.logrosSinVer ?? 0) > 0) revisarLogros()
      })
      .catch(() => {})
    return () => {
      vigente = false
    }
  }, [pathname])

  return (
    <NavLink
      to="/logros"
      aria-label={sinVer > 0 ? 'Logros, hay novedades' : 'Logros'}
      className={({ isActive }) =>
        `btn btn-ghost btn-square relative ${isActive ? 'text-primary' : ''}`
      }
    >
      <IconLogros size={28} />
      {sinVer > 0 && (
        <span
          aria-hidden="true"
          className="absolute right-1.5 top-1.5 size-2.5 rounded-full border-2 border-base-200 bg-primary"
        />
      )}
    </NavLink>
  )
}
