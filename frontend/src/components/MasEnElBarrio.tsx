import { useEffect, useState } from 'react'
import { apiClient } from '../api/client'
import type { BurgerJoint, PageResponse } from '../types'
import { Seccion } from './Seccion'
import { FilaDeTarjetas, LugarEnLaFila, TarjetaChicaDeLocal } from './TarjetaChicaDeLocal'

/** Cuántas se muestran: entran dos y media en un teléfono, que es lo que invita a deslizar. */
const CUANTAS = 6

/**
 * Otras hamburgueserías del mismo barrio, al final de la ficha.
 *
 * Quien llegó hasta abajo de una ficha ya leyó todo, y la pregunta que sigue suele ser
 * "¿y qué más hay por acá?". Antes la respuesta era volver a Explorar y filtrar por el
 * barrio a mano.
 *
 * Sin cadenas: las sucursales de McDonald's no son lo que se viene a descubrir. Sale
 * del listado de Explorar, así que no cuesta nada nuevo, y si falla no se muestra.
 */
export function MasEnElBarrio({ barrio, sinEste }: { barrio: string | null; sinEste: number }) {
  const [locales, setLocales] = useState<{ de: string; lista: BurgerJoint[] } | null>(null)

  useEffect(() => {
    if (!barrio) return
    let vigente = true
    apiClient
      .get<PageResponse<BurgerJoint>>('/burger-joints', {
        params: { area: barrio, size: CUANTAS + 1 },
      })
      .then(({ data }) => {
        if (vigente) setLocales({ de: barrio, lista: data.content })
      })
      .catch(() => {
        // Es un agregado al final de la ficha: si no llega, no se muestra.
      })
    return () => {
      vigente = false
    }
  }, [barrio])

  // Los de otro barrio no se muestran mientras llegan los del nuevo: pasa al ir de una
  // ficha a otra desde esta misma lista.
  const otros = locales?.de === barrio
    ? locales.lista.filter((local) => local.id !== sinEste).slice(0, CUANTAS)
    : []
  if (!barrio || otros.length === 0) return null

  return (
    <Seccion titulo={`Más hamburgueserías en ${barrio}`}>
      <FilaDeTarjetas>
        {otros.map((local) => (
          <LugarEnLaFila key={local.id}>
            <TarjetaChicaDeLocal
              id={local.id}
              nombre={local.name}
              foto={local.photoUrl}
              nota={local.averageScore}
            />
          </LugarEnLaFila>
        ))}
      </FilaDeTarjetas>
    </Seccion>
  )
}
