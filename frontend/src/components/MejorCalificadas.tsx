import { useEffect, useState } from 'react'
import { apiClient } from '../api/client'
import type { PageResponse, RankingItem } from '../types'
import { Seccion } from './Seccion'
import { FilaDeTarjetas, LugarEnLaFila, TarjetaChicaDeLocal } from './TarjetaChicaDeLocal'

/** Las que se asoman; el ranking entero está a un toque. */
const CUANTAS = 8

/**
 * Las mejor calificadas, al final de la primera página de Explorar.
 *
 * Para quien recorrió la lista sin decidirse: veinte hamburgueserías en el orden de la
 * base, la mayoría sin calificar, y esta fila le da por dónde empezar. Sale del mismo
 * ranking que la pestaña Ranking, así que no es otra lista que mantener.
 *
 * Si no llega, no se muestra: Explorar funciona igual sin ella.
 */
export function MejorCalificadas() {
  const [locales, setLocales] = useState<RankingItem[]>([])

  useEffect(() => {
    let vigente = true
    apiClient
      .get<PageResponse<RankingItem>>('/ranking/general', { params: { order: 'score', size: CUANTAS } })
      .then(({ data }) => {
        if (vigente) setLocales(data.content.slice(0, CUANTAS))
      })
      .catch(() => {
        // Es un atajo arriba de la lista: sin él, la lista está igual.
      })
    return () => {
      vigente = false
    }
  }, [])

  if (locales.length === 0) return null

  // El separador va acá y no en Explorar: si la fila no llega, no queda una línea suelta.
  return (
    <div className="border-t border-base-content/10 pt-6">
      <Seccion
        titulo="Las mejor calificadas"
        bajada="¿No te decidiste? Estas son las que mejor puntuó la gente."
        verTodas="/ranking"
        textoDeVerTodas="Ver ranking"
      >
        <FilaDeTarjetas>
          {locales.map((local) => (
            <LugarEnLaFila key={local.burgerJointId}>
              <TarjetaChicaDeLocal
                id={local.burgerJointId}
                nombre={local.name}
                foto={local.photoUrl}
                nota={local.averageScore}
                detalle={local.area}
              />
            </LugarEnLaFila>
          ))}
        </FilaDeTarjetas>
      </Seccion>
    </div>
  )
}
