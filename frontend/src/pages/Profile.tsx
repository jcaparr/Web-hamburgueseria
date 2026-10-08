import { useEffect, useState, type ReactNode } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { CabeceraDePerfil } from '../components/CabeceraDePerfil'
import { CuentasBloqueadas } from '../components/CuentasBloqueadas'
import { IconChevronRight, IconPencil, IconSearch, IconSettings, IconUser } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { SavedTourCard } from '../components/SavedTourCard'
import { SelectorDeTema } from '../components/SelectorDeTema'
import { AvisoVacio } from '../components/Seccion'
import { TarjetaChicaDeLocal } from '../components/TarjetaChicaDeLocal'
import { useAuth } from '../context/useAuth'
import { useTitulo } from '../hooks/useTitulo'
import type { BurgerJoint, ReseniaDePerfil, ProfileStats, SavedTour } from '../types'
import { isSessionExpired } from '../utils/errors'
import { nota } from '../utils/numeros'
import { relativeDate } from '../utils/relativeDate'

/** Cuántas reseñas y guardadas se asoman en el perfil; el resto, en "Ver todas". */
const CUANTAS_EN_LA_GRILLA = 6

type Pestania = 'resenias' | 'guardadas' | 'recorridos'

const PESTANIAS: { id: Pestania; texto: string }[] = [
  { id: 'resenias', texto: 'Reseñas' },
  { id: 'guardadas', texto: 'Guardadas' },
  { id: 'recorridos', texto: 'Recorridos' },
]

function esPestania(valor: string | null): valor is Pestania {
  return PESTANIAS.some((p) => p.id === valor)
}

export function Profile() {
  const { user, logout } = useAuth()
  useTitulo('Tu perfil')
  const navigate = useNavigate()
  // La pestaña va en la dirección, como en el Ranking: al volver de una ficha se vuelve
  // a la misma, y no a Reseñas.
  const [parametros, setParametros] = useSearchParams()
  const pedida = parametros.get('ver')
  const pestania: Pestania = esPestania(pedida) ? pedida : 'resenias'
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
    <div className="flex flex-col gap-6 p-4 md:mx-auto md:max-w-3xl md:p-0 md:pt-6">
      <CabeceraDePerfil
        username={user.username}
        hamburguesa={user.hamburguesa}
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
            valor: stats ? String(stats.ratingsCount) : '—',
            etiqueta: stats?.ratingsCount === 1 ? 'reseña' : 'reseñas',
            a: '/reviews',
          },
          {
            valor: stats?.averageScore ? nota(stats.averageScore) : '—',
            etiqueta: 'promedio',
          },
          {
            valor: stats ? String(stats.seguidores) : '—',
            etiqueta: stats?.seguidores === 1 ? 'seguidor' : 'seguidores',
            // Quiénes son, y desde ahí a quiénes seguís (#183).
            a: `/u/${user.username}/seguidores`,
          },
        ]}
        acciones={
          // Accesos y no botones grandes: se usan de vez en cuando, y dos botones de
          // ancho completo pesaban más que el perfil mismo. Buscar gente está también en
          // el Feed, que es donde más se busca.
          <div className="-my-2 flex flex-wrap gap-x-5">
            <Link to="/buscar" className="inline-flex min-h-11 items-center gap-1.5 rounded-field text-sm font-semibold text-primary hover:underline">
              <IconSearch size={16} />
              Buscar gente
            </Link>
            {/* El perfil que ven los demás no muestra tus guardadas ni tus recorridos:
                acá se puede ver qué queda a la vista. */}
            <Link
              to={`/u/${user.username}`}
              className="inline-flex min-h-11 items-center gap-1.5 rounded-field text-sm font-semibold text-primary hover:underline"
            >
              <IconUser size={16} />
              Cómo te ven los demás
            </Link>
          </div>
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

      {/* Pestañas en vez de tres secciones apiladas: con las tres a la vista, el perfil
          era una tira larga de tarjetas y cajas vacías, y lo de abajo no lo veía nadie. */}
      <div className="flex flex-col gap-4">
        <div role="tablist" aria-label="Qué ver de tu perfil" className="flex border-b border-base-content/15">
          {PESTANIAS.map((p) => (
            <button
              key={p.id}
              type="button"
              role="tab"
              id={`pestania-${p.id}`}
              aria-selected={pestania === p.id}
              aria-controls="contenido-del-perfil"
              onClick={() => elegir(p.id)}
              className={`-mb-px min-h-11 flex-1 cursor-pointer border-b-2 px-2 text-sm font-semibold transition-colors ${
                pestania === p.id
                  ? 'border-primary text-base-content'
                  : 'border-transparent text-base-content/70 hover:text-base-content'
              }`}
            >
              {p.texto}
            </button>
          ))}
        </div>

        <section
          id="contenido-del-perfil"
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
                        nombre={r.burgerJointName}
                        foto={r.photoUrl}
                        nota={r.score}
                        detalle={relativeDate(r.createdAt)}
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
                      <TarjetaChicaDeLocal id={b.id} nombre={b.name} foto={b.photoUrl} nota={b.averageScore} />
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

      {/* Los ajustes al final, aparte y plegados: es lo que menos se toca, y cerrar
          sesión ya está arriba, en la barra. */}
      <details className="group border-t border-base-content/10 pt-2">
        <summary className="flex min-h-11 cursor-pointer list-none items-center gap-2 rounded-field font-display text-base font-bold [&::-webkit-details-marker]:hidden">
          <IconSettings size={18} />
          Ajustes
          <IconChevronRight size={16} className="ml-auto transition-transform group-open:rotate-90" />
        </summary>
        <div className="flex flex-col gap-5 pt-2 pb-2">
          <SelectorDeTema />
          <CuentasBloqueadas />
          <button type="button" onClick={cerrarSesion} className="btn btn-outline w-full md:w-fit md:px-6">
            Cerrar sesión
          </button>
          <nav aria-label="Legales" className="-my-2 flex flex-wrap gap-x-5 text-sm">
            <Link to="/terminos" className="inline-flex min-h-11 items-center rounded-field text-base-content/70 hover:text-base-content hover:underline">
              Términos y condiciones
            </Link>
            <Link to="/privacidad" className="inline-flex min-h-11 items-center rounded-field text-base-content/70 hover:text-base-content hover:underline">
              Política de privacidad
            </Link>
          </nav>
        </div>
      </details>
    </div>
  )
}

/** Las tarjetas de una pestaña: de a dos en el teléfono y de a tres en la compu. */
function GrillaDeLocales({ children }: { children: ReactNode }) {
  return <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3">{children}</ul>
}

/** El enlace al final de una pestaña, hacia la lista entera. */
function VerTodas({ a, children }: { a: string; children: ReactNode }) {
  return (
    <Link
      to={a}
      className="-my-3 flex w-fit items-center gap-1 self-end rounded-field py-3 text-sm font-semibold text-primary hover:underline"
    >
      {children}
      <IconChevronRight size={14} />
    </Link>
  )
}
