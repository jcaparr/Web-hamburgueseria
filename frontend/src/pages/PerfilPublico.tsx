import { Link, useNavigate, useParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { BotonBloquear } from '../components/BotonBloquear'
import { BotonSeguir } from '../components/BotonSeguir'
import { CabeceraDePerfil } from '../components/CabeceraDePerfil'
import { JointPhoto } from '../components/JointPhoto'
import { LoadError } from '../components/LoadError'
import { ScoreBadge } from '../components/ScoreBadge'
import { AvisoVacio, Seccion } from '../components/Seccion'
import { usePedido } from '../hooks/usePedido'
import { useTitulo } from '../hooks/useTitulo'
import { isNotFound } from '../utils/errors'
import { nota } from '../utils/numeros'
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

  // Solo lo de esta persona: al pasar de un perfil a otro no se ve el anterior mientras
  // llega el nuevo.
  const pedido = usePedido(`usuario:${username}`, () =>
    apiClient.get<Perfil>(`/usuarios/${username}`).then(({ data }) => data),
  )
  const perfil = pedido.datos
  const noExiste = isNotFound(pedido.error)
  useTitulo(noExiste ? 'Perfil no encontrado' : `@${username}`)

  // El servidor manda los contadores; al seguir o dejar de seguir se corrigen acá, para
  // que el número no quede contradiciendo al botón que se acaba de tocar.
  function cambioDeSeguimiento(loSigo: boolean) {
    pedido.actualizar((previo) => ({
      ...previo,
      loSigo,
      seguidores: previo.seguidores + (loSigo ? 1 : -1),
    }))
  }

  // Que no exista no es una falla: reintentar no lo arregla, y "algo salió mal" lo
  // hacía parecer un problema nuestro (#129).
  if (noExiste) {
    return (
      <div className="flex flex-col items-start gap-3 p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
        <h1 className="titulo-pagina">
          No encontramos a @{username}
        </h1>
        <p className="text-sm text-base-content/70">
          Puede que el enlace esté mal escrito o que esa cuenta ya no exista.
        </p>
        <Link to="/buscar" className="btn btn-primary btn-sm">
          Buscar gente
        </Link>
      </div>
    )
  }

  if (pedido.error) {
    return (
      <div className="p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
        <LoadError error={pedido.error} onRetry={pedido.reintentar} />
      </div>
    )
  }

  if (!perfil) {
    return (
      <p role="status" className="p-4 text-sm text-base-content/70">
        Cargando…
      </p>
    )
  }

  return (
    <div className="flex flex-col gap-6 p-4 md:mx-auto md:max-w-3xl md:p-0 md:pt-6">
      <CabeceraDePerfil
        username={perfil.username}
        hamburguesa={perfil.hamburguesa}
        bajada={
          perfil.siguiendo > 0 ? (
            // A quiénes sigue (#183): dice mucho de qué le gusta, y es de donde sale
            // gente nueva para seguir.
            <Link
              to={`/u/${perfil.username}/siguiendo`}
              className="-my-2 inline-flex min-h-11 w-fit items-center rounded-field text-sm text-base-content/70 hover:text-primary hover:underline"
            >
              {siguiendoEnPalabras(perfil.siguiendo)}
            </Link>
          ) : (
            <span className="text-sm text-base-content/70">{siguiendoEnPalabras(perfil.siguiendo)}</span>
          )
        }
        cifras={[
          { valor: String(perfil.resenias), etiqueta: perfil.resenias === 1 ? 'reseña' : 'reseñas' },
          { valor: perfil.promedio ? nota(perfil.promedio) : '—', etiqueta: 'promedio' },
          {
            valor: String(perfil.seguidores),
            etiqueta: perfil.seguidores === 1 ? 'seguidor' : 'seguidores',
            a: `/u/${perfil.username}/seguidores`,
          },
        ]}
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

      {/* Con lo que escribió y no solo la nota: es lo que dice si vale la pena seguirla.
          Un 4 sin más es igual al de cualquiera. */}
      <Seccion titulo="Últimas reseñas">
        {perfil.ultimasResenias.length > 0 ? (
          <ul className="flex flex-col gap-3">
            {perfil.ultimasResenias.map((r) => (
              <li key={r.id}>
                <Link
                  to={`/burger-joints/${r.burgerJointId}`}
                  className="flex flex-col gap-2 tarjeta p-3 transition-shadow hover:tarjeta-alzada"
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
