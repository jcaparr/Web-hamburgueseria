import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import type { BurgerJoint, PageResponse } from '../types'
import { JointPhoto } from './JointPhoto'
import { ScoreBadge } from './ScoreBadge'

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
        params: { area: barrio, conCadenas: false, size: CUANTAS + 1 },
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
    <section className="flex flex-col gap-3" aria-labelledby="mas-en-el-barrio">
      <h2 id="mas-en-el-barrio" className="font-display text-lg font-bold">
        Más hamburgueserías en {barrio}
      </h2>
      <ul className="-mx-4 flex snap-x gap-3 overflow-x-auto px-4 pb-2 md:mx-0 md:px-0">
        {otros.map((local) => (
          <li key={local.id} className="w-40 flex-none snap-start">
            <Link
              to={`/burger-joints/${local.id}`}
              className="flex h-full flex-col overflow-hidden rounded-box bg-base-100 ring-1 ring-inset ring-base-content/15 transition-shadow hover:shadow-md"
            >
              <JointPhoto src={local.photoUrl} name={local.name} className="aspect-[4/3] w-full object-cover" />
              <div className="flex flex-1 flex-col gap-1.5 p-2.5">
                <span className="line-clamp-2 text-sm font-semibold leading-snug">{local.name}</span>
                <span className="mt-auto">
                  {local.averageScore ? (
                    <ScoreBadge score={local.averageScore} size="sm" />
                  ) : (
                    <span className="text-xs text-base-content/70">Sin calificaciones</span>
                  )}
                </span>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
