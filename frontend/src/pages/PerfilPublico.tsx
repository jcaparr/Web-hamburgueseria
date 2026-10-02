import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { BotonBloquear } from '../components/BotonBloquear'
import { BotonSeguir } from '../components/BotonSeguir'
import { CabeceraDePerfil, CifrasDePerfil } from '../components/CabeceraDePerfil'
import { JointPhoto } from '../components/JointPhoto'
import { LoadError } from '../components/LoadError'
import { ScoreBadge } from '../components/ScoreBadge'
import { AvisoVacio, Seccion } from '../components/Seccion'
import { relativeDate } from '../utils/relativeDate'
import type { PerfilPublico as Perfil } from '../types'

/**
 * El perfil de otra persona.
 *
 * Muestra sus reseñas y sus números, que es lo que hace falta para decidir si vale la
 * pena seguirla. Sus favoritos no: marcar una hamburguesería para ir algún día es una
 * intención, no una opinión publicada.
 */
export function PerfilPublico() {
  const { username = '' } = useParams()
  const navigate = useNavigate()
  const [perfil, setPerfil] = useState<Perfil | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [cargando, setCargando] = useState(true)

  const cargar = useCallback(() => {
    setCargando(true)
    apiClient
      .get<Perfil>(`/usuarios/${username}`)
      .then(({ data }) => {
        setPerfil(data)
        setError(null)
      })
      .catch(setError)
      .finally(() => setCargando(false))
  }, [username])

  useEffect(cargar, [cargar])

  // El servidor manda los contadores; al seguir o dejar de seguir se corrigen acá, para
  // que el número no quede contradiciendo al botón que se acaba de tocar.
  function cambioDeSeguimiento(loSigo: boolean) {
    setPerfil((previo) =>
      previo === null
        ? previo
        : { ...previo, loSigo, seguidores: previo.seguidores + (loSigo ? 1 : -1) },
    )
  }

  if (error) {
    return (
      <div className="p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
        <LoadError error={error} onRetry={cargar} />
      </div>
    )
  }

  if (cargando || !perfil) {
    return <p className="p-4 text-sm text-base-content/70">Cargando…</p>
  }

  return (
    <div className="flex flex-col gap-6 p-4 md:mx-auto md:max-w-3xl md:p-0 md:pt-2">
      <CabeceraDePerfil
        username={perfil.username}
        bajada={
          <span className="text-sm text-base-content/70">
            {siguiendoEnPalabras(perfil.siguiendo)}
          </span>
        }
        acciones={
          perfil.soyYo ? (
            <Link to="/profile" className="btn btn-outline col-span-2 md:px-6">
              Ir a mi perfil
            </Link>
          ) : (
            // Seguir es lo que se viene a hacer acá, así que ocupa la fila entera en el
            // teléfono. Bloquear quedó al final, lejos: no es algo que se toque de paso.
            <div className="col-span-2 grid md:flex md:[&>button]:px-8">
              <BotonSeguir
                username={perfil.username}
                loSigo={perfil.loSigo}
                onCambio={cambioDeSeguimiento}
              />
            </div>
          )
        }
      />

      <CifrasDePerfil
        cifras={[
          { valor: String(perfil.resenias), etiqueta: perfil.resenias === 1 ? 'Reseña' : 'Reseñas' },
          { valor: perfil.promedio ? perfil.promedio.toFixed(1) : '—', etiqueta: 'Promedio' },
          { valor: String(perfil.seguidores), etiqueta: perfil.seguidores === 1 ? 'Seguidor' : 'Seguidores' },
        ]}
      />

      {/* Con lo que escribió y no solo la nota: es lo que dice si vale la pena seguirla.
          Un 4 sin más es igual al de cualquiera. */}
      <Seccion titulo="Últimas reseñas">
        {perfil.ultimasResenias.length > 0 ? (
          <ul className="flex flex-col gap-3">
            {perfil.ultimasResenias.map((r) => (
              <li key={r.id}>
                <Link
                  to={`/burger-joints/${r.burgerJointId}`}
                  className="flex flex-col gap-2 rounded-box bg-base-100 p-3 ring-1 ring-inset ring-base-content/15 transition-shadow hover:shadow-md"
                >
                  <span className="flex items-center gap-3">
                    <JointPhoto
                      src={r.photoUrl}
                      name={r.burgerJointName}
                      className="h-14 w-14 flex-none rounded-lg object-cover"
                    />
                    <span className="flex min-w-0 flex-1 flex-col">
                      <span className="truncate font-semibold">{r.burgerJointName}</span>
                      <span className="text-xs text-base-content/70">{relativeDate(r.createdAt)}</span>
                    </span>
                    <ScoreBadge score={r.score} size="sm" />
                  </span>
                  {r.comment && (
                    <span className="line-clamp-3 text-sm leading-relaxed text-base-content/80">
                      {r.comment}
                    </span>
                  )}
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          <AvisoVacio>
            {perfil.soyYo
              ? 'Todavía no calificaste ninguna hamburguesería.'
              : 'Todavía no calificó ninguna hamburguesería.'}
          </AvisoVacio>
        )}
      </Seccion>

      {!perfil.soyYo && (
        <section className="border-t border-base-content/10 pt-6">
          <BotonBloquear
            username={perfil.username}
            // Bloqueado, este perfil ya no se puede pedir: quedarse acá mostraría un
            // error donde hace un segundo había una persona.
            onBloqueado={() => navigate('/feed')}
          />
        </section>
      )}
    </div>
  )
}

/** "Sigue a 3 personas"; con cero, "Sigue a 0 personas" se leía como un error. */
function siguiendoEnPalabras(cuantos: number) {
  if (cuantos === 0) return 'Todavía no sigue a nadie'
  return cuantos === 1 ? 'Sigue a 1 persona' : `Sigue a ${cuantos} personas`
}
