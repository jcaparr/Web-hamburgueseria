import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import { IconSearch } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { TarjetaDeFeed } from '../components/TarjetaDeFeed'
import { useAuth } from '../context/AuthContext'
import type { ItemDeFeed, PaginaDeFeed } from '../types'

type Fuente = 'TODOS' | 'SIGUIENDO'

const PESTANIAS: { fuente: Fuente; texto: string }[] = [
  { fuente: 'TODOS', texto: 'Para vos' },
  { fuente: 'SIGUIENDO', texto: 'Siguiendo' },
]

/**
 * Lo último que se reseñó, de todos o de quienes seguís.
 *
 * Arranca en "Para vos", que es todo, y no en "Siguiendo". El día que alguien se
 * registra no sigue a nadie: si la pantalla de entrada fuera la de los seguidos,
 * lo primero que vería sería un vacío, y no tendría de dónde sacar a quién seguir.
 */
export function Feed() {
  const { user } = useAuth()
  const [fuente, setFuente] = useState<Fuente>('TODOS')
  const [items, setItems] = useState<ItemDeFeed[]>([])
  const [siguiente, setSiguiente] = useState<string | null>(null)
  const [cargando, setCargando] = useState(true)
  const [trayendoMas, setTrayendoMas] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [intento, setIntento] = useState(0)

  // Al cambiar de pestaña se vuelve a empezar: el cursor de una no sirve para la otra,
  // porque apunta a una reseña que en la otra lista puede no estar.
  useEffect(() => {
    let vigente = true
    setCargando(true)

    apiClient
      .get<PaginaDeFeed>('/feed', { params: { fuente } })
      .then(({ data }) => {
        if (!vigente) return
        setItems(data.items)
        setSiguiente(data.siguiente)
        setError(null)
      })
      .catch((err) => {
        if (vigente) setError(err)
      })
      .finally(() => {
        if (vigente) setCargando(false)
      })

    return () => {
      vigente = false
    }
  }, [fuente, intento])

  const traerMas = useCallback(() => {
    if (!siguiente || trayendoMas) return

    setTrayendoMas(true)
    apiClient
      .get<PaginaDeFeed>('/feed', { params: { fuente, cursor: siguiente } })
      .then(({ data }) => {
        setItems((previos) => [...previos, ...data.items])
        setSiguiente(data.siguiente)
      })
      .catch(setError)
      .finally(() => setTrayendoMas(false))
  }, [fuente, siguiente, trayendoMas])

  return (
    // Angosto en pantalla grande, y no todo el ancho disponible: la foto se muestra en
    // un cuadrado tan ancho como la columna, así que una columna de 900 px pediría
    // fotos de 900 para verse nítida. A este ancho, el mínimo que se exige al subir
    // alcanza y sobra.
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-[560px] md:p-0 md:pt-6">
      <div className="flex items-center gap-3">
        <Link
          to="/buscar"
          aria-label="Buscar gente"
          className="btn btn-ghost btn-sm btn-square flex-none"
        >
          <IconSearch size={18} />
        </Link>
        <div role="tablist" className="tabs tabs-box flex-1">
          {PESTANIAS.map((p) => (
            <button
              key={p.fuente}
              role="tab"
              type="button"
              onClick={() => setFuente(p.fuente)}
              className={`tab flex-1 ${fuente === p.fuente ? 'tab-active' : ''}`}
            >
              {p.texto}
            </button>
          ))}
        </div>
      </div>

      {error ? (
        <LoadError error={error} onRetry={() => setIntento((n) => n + 1)} />
      ) : (
        <>
          <div className="flex flex-col gap-3">
            {items.map((item) => (
              <TarjetaDeFeed
                key={item.ratingId}
                item={item}
                esMia={item.autorId === user?.userId}
              />
            ))}
          </div>

          {cargando && <p className="text-sm text-base-content/70">Cargando…</p>}

          {!cargando && items.length === 0 && (
            <div className="flex flex-col items-start gap-3 rounded-box bg-base-100 p-4 ring-1 ring-inset ring-base-content/15">
              <p className="text-sm text-base-content/80">
                {fuente === 'SIGUIENDO'
                  ? 'Todavía no seguís a nadie, o quienes seguís no reseñaron nada.'
                  : 'Todavía no hay reseñas en la app.'}
              </p>
              {fuente === 'SIGUIENDO' && (
                <Link to="/buscar" className="btn btn-sm btn-primary">
                  Buscar gente
                </Link>
              )}
            </div>
          )}

          {siguiente && (
            <button
              type="button"
              onClick={traerMas}
              disabled={trayendoMas}
              className="btn btn-outline btn-block"
            >
              {trayendoMas ? 'Trayendo…' : 'Ver más'}
            </button>
          )}
        </>
      )}
    </div>
  )
}
