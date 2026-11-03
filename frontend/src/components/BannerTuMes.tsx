import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import type { TuMes } from '../types'
import { anioDelMes, nombreDelMes } from '../utils/tuMes'
import { IconChevronRight } from './icons'
import { Personaje } from './Personaje'

/** Qué resúmenes ya abrió esta persona, para no insistir con el mismo (solo en este navegador). */
const CLAVE = 'burgometro.tuMesVisto'

function yaLoVio(mes: string): boolean {
  try {
    return localStorage.getItem(CLAVE) === mes
  } catch {
    return false
  }
}

function marcarComoVisto(mes: string) {
  try {
    localStorage.setItem(CLAVE, mes)
  } catch {
    // Sin almacenamiento, el aviso vuelve a aparecer: no pasa nada.
  }
}

/**
 * El aviso de "Tu mes en Burgómetro" (#308) en el perfil propio: aparece desde el 1 del
 * mes, con el resumen del mes anterior, y solo si hubo al menos una reseña. Una vez
 * abierto, no vuelve hasta el resumen siguiente. Pide un solo resumen, filtrado por
 * usuario y por fecha en el servidor.
 */
export function BannerTuMes() {
  const [resumen, setResumen] = useState<TuMes | null>(null)
  const [visto, setVisto] = useState(false)

  useEffect(() => {
    let vigente = true
    apiClient
      .get<TuMes>('/profile/tu-mes')
      .then((res) => {
        if (!vigente || res.data.resenias === 0) return
        setResumen(res.data)
        setVisto(yaLoVio(res.data.mes))
      })
      // Es un extra: si no llega, el perfil sigue igual.
      .catch(() => undefined)
    return () => {
      vigente = false
    }
  }, [])

  if (!resumen || visto) return null
  const mes = nombreDelMes(resumen.mes)
  return (
    <Link
      to="/tu-mes"
      onClick={() => marcarComoVisto(resumen.mes)}
      className="flex min-h-11 items-center gap-3 rounded-box bg-primary p-4 text-primary-content shadow-sm"
    >
      <Personaje pose="mapa" className="h-14 w-auto shrink-0" />
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="font-bold">Tu {mes} en Burgómetro</span>
        <span className="text-sm">
          Ya está el resumen de {mes} de {anioDelMes(resumen.mes)}.
        </span>
      </span>
      <IconChevronRight size={20} className="shrink-0" />
    </Link>
  )
}
