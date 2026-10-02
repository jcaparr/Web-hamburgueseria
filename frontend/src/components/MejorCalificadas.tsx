import { useEffect, useState } from 'react'
import { apiClient } from '../api/client'
import type { PageResponse, RankingItem } from '../types'
import { Seccion } from './Seccion'
import { FilaDeTarjetas, LugarEnLaFila, TarjetaChicaDeLocal } from './TarjetaChicaDeLocal'

/** Las que se asoman; el ranking entero está a un toque. */
const CUANTAS = 8

/**
 * Las mejor calificadas, arriba de Explorar.
 *
 * Quien entra sin saber qué busca se encontraba con 1.548 hamburgueserías en el orden
 * de la base, y la mayoría sin calificar. Esta fila le da por dónde empezar. Sale del
 * mismo ranking que la pestaña Ranking, así que no es otra lista que mantener.
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

  return (
    <Seccion titulo="Las mejor calificadas" verTodas="/ranking" textoDeVerTodas="Ver ranking">
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
  )
}
