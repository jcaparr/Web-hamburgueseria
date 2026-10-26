import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { CabeceraDePerfil } from '../components/CabeceraDePerfil'
import { IconPencil } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { BarraDePestanias, GrillaDeLocales, VerTodas } from '../components/PestaniasDePerfil'
import { SavedTourCard } from '../components/SavedTourCard'
import { AvisoVacio } from '../components/Seccion'
import { TarjetaChicaDeLocal } from '../components/TarjetaChicaDeLocal'
import { aLaResenia } from '../utils/aLaResenia'
import { useAuth } from '../context/useAuth'
import { useTitulo } from '../hooks/useTitulo'
import type { BurgerJoint, ReseniaDePerfil, ProfileStats, SavedTour } from '../types'
import { isSessionExpired } from '../utils/errors'
import { PANEL_DEL_PERFIL, pestaniaDe, type Pestania } from '../utils/pestaniasDePerfil'
import { cantidad, nota } from '../utils/numeros'
import { fechaYHora } from '../utils/fechaYHora'

/** Cuántas reseñas y guardadas se asoman en el perfil; el resto, en "Ver todas". */
const CUANTAS_EN_LA_GRILLA = 6

export function Profile() {
  const { user } = useAuth()
  useTitulo('Tu perfil')
  const navigate = useNavigate()
  // La pestaña va en la dirección, como en el Ranking: al volver de una ficha se vuelve
  // a la misma, y no a Reseñas.
  const [parametros, setParametros] = useSearchParams()
  const pestania = pestaniaDe(parametros.get('ver'))
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
        setFavorites(wishlistRes.data)
        setTours(toursRes.data)
      })
      .catch((err) => {
        if (isSessionExpired(err)) navigate('/login')
        else setError(err)
      })
      .finally(() => setLoading(false))
  }, [user, navigate, attempt])

  function elegir(nueva: Pestania) {
    // replace: cambiar de pestaña no es ir a otra página, y "atrás" tiene que salir del
    // perfil, no recorrer las pestañas que se tocaron.
    setParametros(nueva === 'resenias' ? {} : { ver: nueva }, { replace: true })
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
    <div className="flex flex-col gap-6 p-4 md:mx-auto md:max-w-3xl md:p-0 md:pt-6">
      <CabeceraDePerfil
        username={user.username}
        hamburguesa={user.hamburguesa}
        xp={user.xp}
        // Debajo del nombre, al lado de la hamburguesa: es donde se la mira y donde se
        // busca cómo cambiarla.
        bajada={
          <Link
            to="/profile/hamburguesa"
            className="-my-2 inline-flex min-h-11 w-fit items-center gap-1.5 rounded-field text-sm font-semibold text-primary hover:underline"
          >
            <IconPencil size={14} />
            Cambiar tu hamburguesa
          </Link>
        }
        cifras={[
          {
            valor: stats ? cantidad(stats.ratingsCount) : '—',
            etiqueta: stats?.ratingsCount === 1 ? 'reseña' : 'reseñas',
            a: '/reviews',
          },
          {
            valor: stats?.averageScore ? nota(stats.averageScore) : '—',
            etiqueta: 'promedio',
          },
          {
            valor: stats ? cantidad(stats.seguidores) : '—',
            etiqueta: stats?.seguidores === 1 ? 'seguidor' : 'seguidores',
            // Quiénes son (#183).
            a: `/u/${user.username}/seguidores`,
          },
          {
            // A quiénes seguís (#232), al lado de quiénes te siguen.
            valor: stats ? cantidad(stats.siguiendo) : '—',
            etiqueta: 'siguiendo',
            a: `/u/${user.username}/siguiendo`,
          },
        ]}
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

      <div className="flex flex-col gap-4">
        <BarraDePestanias actual={pestania} onElegir={elegir} etiqueta="Qué ver de tu perfil" />

        <section
          id={PANEL_DEL_PERFIL}
          role="tabpanel"
          aria-labelledby={`pestania-${pestania}`}
          aria-busy={loading}
          className="flex flex-col gap-4"
        >
          {loading ? (
            <p role="status" className="text-sm text-base-content/70">
              Cargando…
            </p>
          ) : error !== null ? null : pestania === 'resenias' ? (
            ratings.length > 0 ? (
              <>
                <GrillaDeLocales>
                  {ratings.slice(0, CUANTAS_EN_LA_GRILLA).map((r) => (
                    <li key={r.id}>
                      <TarjetaChicaDeLocal
                        id={r.burgerJointId}
                        slug={r.slug}
                        nombre={r.burgerJointName}
                        foto={r.photoUrl}
                        nota={r.score}
                        detalle={fechaYHora(r.createdAt)}
                        a={aLaResenia({ id: r.burgerJointId, slug: r.slug }, r.id)}
                      />
                    </li>
                  ))}
                </GrillaDeLocales>
                {ratings.length > CUANTAS_EN_LA_GRILLA && (
                  <VerTodas a="/reviews">Ver tus {ratings.length} reseñas</VerTodas>
                )}
              </>
            ) : (
              <AvisoVacio accion={{ texto: 'Buscar una para calificar', a: '/' }}>
                Todavía no calificaste ninguna hamburguesería.
              </AvisoVacio>
            )
          ) : pestania === 'guardadas' ? (
            favorites.length > 0 ? (
              <>
                <GrillaDeLocales>
                  {favorites.slice(0, CUANTAS_EN_LA_GRILLA).map((b) => (
                    <li key={b.id}>
                      <TarjetaChicaDeLocal id={b.id} slug={b.slug} nombre={b.name} foto={b.photoUrl} nota={b.averageScore} />
                    </li>
                  ))}
                </GrillaDeLocales>
                {favorites.length > CUANTAS_EN_LA_GRILLA && (
                  <VerTodas a="/wishlist">Ver las {favorites.length} guardadas</VerTodas>
                )}
              </>
            ) : (
              <AvisoVacio accion={{ texto: 'Explorar hamburgueserías', a: '/' }}>
                Todavía no guardaste ninguna. Tocá el corazón de las que quieras probar y
                aparecen acá.
              </AvisoVacio>
            )
          ) : tours.length > 0 ? (
            <>
              <ul className="grid gap-3 md:grid-cols-2">
                {tours.map((tour) => (
                  <li key={tour.id}>
                    <SavedTourCard tour={tour} onBorrar={borrarTour} borrando={borrando === tour.id} />
                  </li>
                ))}
              </ul>
              <VerTodas a="/tour">Armar otro recorrido</VerTodas>
            </>
          ) : (
            <AvisoVacio accion={{ texto: 'Armar un recorrido', a: '/tour' }}>
              Armá un recorrido por varias hamburgueserías y guardalo para hacerlo cuando
              quieras.
            </AvisoVacio>
          )}
        </section>
      </div>
    </div>
  )
}
