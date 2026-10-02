import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { CabeceraDePerfil, CifrasDePerfil } from '../components/CabeceraDePerfil'
import { CuentasBloqueadas } from '../components/CuentasBloqueadas'
import { IconMedal, IconSearch } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { SavedTourCard } from '../components/SavedTourCard'
import { AvisoVacio, Seccion } from '../components/Seccion'
import { FilaDeTarjetas, LugarEnLaFila, TarjetaChicaDeLocal } from '../components/TarjetaChicaDeLocal'
import { useAuth } from '../context/AuthContext'
import type { BurgerJoint, ReseniaDePerfil, ProfileStats, SavedTour } from '../types'
import { isSessionExpired } from '../utils/errors'
import { relativeDate } from '../utils/relativeDate'

/** Cuántas reseñas y guardadas se asoman en el perfil; el resto, en "Ver todas". */
const CUANTAS_EN_LA_FILA = 6

export function Profile() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [stats, setStats] = useState<ProfileStats | null>(null)
  const [ratings, setRatings] = useState<ReseniaDePerfil[]>([])
  const [favorites, setFavorites] = useState<BurgerJoint[]>([])
  const [tours, setTours] = useState<SavedTour[]>([])
  const [borrando, setBorrando] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    // RequireAuth guarantees there is a session by the time this renders.
    Promise.all([
      apiClient.get<ProfileStats>('/profile/stats'),
      apiClient.get<ReseniaDePerfil[]>('/profile/ratings'),
      apiClient.get<BurgerJoint[]>('/wishlist'),
      apiClient.get<SavedTour[]>('/tours/mios'),
    ])
      .then(([statsRes, ratingsRes, wishlistRes, toursRes]) => {
        setStats(statsRes.data)
        setRatings(ratingsRes.data)
        setFavorites(wishlistRes.data.slice(0, CUANTAS_EN_LA_FILA))
        setTours(toursRes.data)
      })
      .catch((err) => {
        if (isSessionExpired(err)) navigate('/login')
        else setError(err)
      })
      .finally(() => setLoading(false))
  }, [user, navigate, attempt])

  const recentRatings = ratings.slice(0, CUANTAS_EN_LA_FILA)

  // Lo mismo que "Salir" arriba, y en el mismo orden: primero se va al inicio, porque
  // borrar la sesión con una página privada abierta manda al login.
  async function cerrarSesion() {
    navigate('/')
    await logout()
  }

  function borrarTour(id: number) {
    setBorrando(id)
    apiClient
      .delete(`/tours/${id}`)
      // Se saca de la lista acá en vez de volver a pedirla: es una fila menos, y pedir
      // todo el perfil de nuevo haría parpadear lo que no cambió.
      .then(() => setTours((previos) => previos.filter((tour) => tour.id !== id)))
      .catch((err) => {
        if (isSessionExpired(err)) navigate('/login')
        else setError(err)
      })
      .finally(() => setBorrando(null))
  }

  // RequireAuth already guarantees this, but the compiler cannot see through it and
  // the name is read below.
  if (!user) return null

  return (
    <div className="flex flex-col gap-6 p-4 md:mx-auto md:max-w-3xl md:p-0 md:pt-2">
      <CabeceraDePerfil
        username={user.username}
        bajada={
          <span className="inline-flex w-fit items-center gap-1.5 rounded-full bg-secondary/30 px-3 py-1 text-xs font-semibold text-neutral">
            <IconMedal size={14} />
            Nivel hamburguesero: próximamente
          </span>
        }
        acciones={
          <>
            <Link to="/buscar" className="btn btn-outline gap-2 whitespace-nowrap md:px-6">
              <IconSearch size={16} />
              Buscar gente
            </Link>
            {/* El perfil que ven los demás no muestra tus guardadas ni tus recorridos:
                acá se puede ver qué queda a la vista. */}
            <Link to={`/u/${user.username}`} className="btn btn-outline whitespace-nowrap md:px-6">
              Cómo te ven
            </Link>
          </>
        }
      />

      {error !== null && (
        <LoadError
          error={error}
          onRetry={() => {
            setError(null)
            setLoading(true)
            setAttempt((n) => n + 1)
          }}
        />
      )}

      <CifrasDePerfil
        cifras={[
          {
            valor: stats ? String(stats.ratingsCount) : '—',
            etiqueta: stats?.ratingsCount === 1 ? 'Reseña' : 'Reseñas',
            a: '/reviews',
          },
          {
            valor: stats?.averageScore ? stats.averageScore.toFixed(1) : '—',
            etiqueta: 'Promedio',
          },
          {
            valor: stats ? String(stats.seguidores) : '—',
            etiqueta: stats?.seguidores === 1 ? 'Seguidor' : 'Seguidores',
          },
        ]}
      />

      {loading && (
        <p role="status" className="text-sm text-base-content/70">
          Cargando…
        </p>
      )}

      {/* Las tres filas que siguen se deslizan de costado, como "Más hamburgueserías
          en…" en la ficha: apiladas, con cuatro de cada una el perfil era una tira
          interminable y lo de abajo no lo veía nadie. */}
      <Seccion titulo="Mis reseñas" verTodas={ratings.length > 0 ? '/reviews' : undefined}>
        {recentRatings.length > 0 ? (
          <FilaDeTarjetas>
            {recentRatings.map((r) => (
              <LugarEnLaFila key={r.id}>
                <TarjetaChicaDeLocal
                  id={r.burgerJointId}
                  nombre={r.burgerJointName}
                  foto={r.photoUrl}
                  nota={r.score}
                  detalle={relativeDate(r.createdAt)}
                />
              </LugarEnLaFila>
            ))}
          </FilaDeTarjetas>
        ) : (
          !loading &&
          !error && (
            <AvisoVacio accion={{ texto: 'Buscar una para calificar', a: '/' }}>
              Todavía no calificaste ninguna hamburguesería.
            </AvisoVacio>
          )
        )}
      </Seccion>

      <Seccion titulo="Guardadas" verTodas={favorites.length > 0 ? '/wishlist' : undefined}>
        {favorites.length > 0 ? (
          <FilaDeTarjetas>
            {favorites.map((b) => (
              <LugarEnLaFila key={b.id}>
                <TarjetaChicaDeLocal id={b.id} nombre={b.name} foto={b.photoUrl} nota={b.averageScore} />
              </LugarEnLaFila>
            ))}
          </FilaDeTarjetas>
        ) : (
          !loading &&
          !error && (
            <AvisoVacio accion={{ texto: 'Explorar hamburgueserías', a: '/' }}>
              Todavía no guardaste ninguna. Tocá el corazón de las que quieras probar y
              aparecen acá.
            </AvisoVacio>
          )
        )}
      </Seccion>

      <Seccion
        titulo="Mis recorridos"
        verTodas={tours.length > 0 ? '/tour' : undefined}
        textoDeVerTodas="Armar otro"
      >
        {tours.length > 0 ? (
          <ul className="-mx-4 flex snap-x gap-3 overflow-x-auto px-4 pb-2 md:mx-0 md:px-0">
            {tours.map((tour) => (
              <li key={tour.id} className="w-72 flex-none snap-start">
                <SavedTourCard tour={tour} onBorrar={borrarTour} borrando={borrando === tour.id} />
              </li>
            ))}
          </ul>
        ) : (
          !loading &&
          !error && (
            <AvisoVacio accion={{ texto: 'Armar un tour', a: '/tour' }}>
              Armá un recorrido por varias hamburgueserías y guardalo para hacerlo cuando
              quieras.
            </AvisoVacio>
          )
        )}
      </Seccion>

      <Seccion titulo="Logros">
        <AvisoVacio>
          Muy pronto vas a poder desbloquear logros a medida que calificás y descubrís
          hamburgueserías.
        </AvisoVacio>
      </Seccion>

      {/* Lo de la cuenta al final y aparte: es lo que menos se toca, y cerrar sesión no
          tiene que quedar al lado de nada que se toque seguido. */}
      <section className="flex flex-col gap-4 border-t border-base-content/10 pt-6">
        <CuentasBloqueadas />
        <button type="button" onClick={cerrarSesion} className="btn btn-outline w-full md:w-fit md:px-6">
          Cerrar sesión
        </button>
      </section>
    </div>
  )
}
