import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { LoadError } from '../components/LoadError'
import type { BurgerJoint } from '../types'
import { isNotFound } from '../utils/errors'
import { BurgerJointDetail } from './BurgerJointDetail'
import { NoEncontrada } from './NoEncontrada'

/**
 * La ficha de un local por su dirección linda, burgometro.com.ar/<slug> (#305).
 *
 * Solo averigua qué local es y le pasa el id a la ficha de siempre. Si no hay ninguno
 * con ese nombre es una dirección mal escrita como cualquier otra.
 */
export function FichaPorSlug() {
  const { slug } = useParams()
  const [resuelto, setResuelto] = useState<{ slug: string; id: number } | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    apiClient
      .get<BurgerJoint>(`/burger-joints/por-slug/${slug}`)
      .then(({ data }) => vigente && (setError(null), setResuelto({ slug: slug ?? '', id: data.id })))
      .catch((e) => vigente && setError(e))
    return () => {
      vigente = false
    }
  }, [slug, intento])

  if (isNotFound(error)) return <NoEncontrada />
  if (error) return <LoadError error={error} onRetry={() => setIntento((n) => n + 1)} />
  if (!resuelto || resuelto.slug !== slug) return null
  return <BurgerJointDetail key={resuelto.id} idDelLocal={String(resuelto.id)} />
}
