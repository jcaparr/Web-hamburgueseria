import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { BotonBloquear } from '../components/BotonBloquear'
import { BotonSeguir } from '../components/BotonSeguir'
import { CabeceraDePerfil } from '../components/CabeceraDePerfil'
import { GuardadasDeOtraPersona, RecorridosDeOtraPersona } from '../components/ListasDeOtraPersona'
import { ListaDeReseniasDePerfil } from '../components/ListaDeReseniasDePerfil'
import { LoadError } from '../components/LoadError'
import { BarraDePestanias, VerTodas } from '../components/PestaniasDePerfil'
import { AvisoVacio, Seccion } from '../components/Seccion'
import { usePedido } from '../hooks/usePedido'
import { useTitulo } from '../hooks/useTitulo'
import { isNotFound } from '../utils/errors'
import { PANEL_DEL_PERFIL, pestaniaDe, type Pestania } from '../utils/pestaniasDePerfil'
import { cantidad, nota } from '../utils/numeros'
import type { PerfilPublico as Perfil } from '../types'

/**
 * El perfil de otra persona.
 *
 * Muestra sus reseñas y sus números, que es lo que hace falta para decidir si vale la
 * pena seguirla. Sus guardadas y sus recorridos, solo si eligió mostrarlos (#264):
 * marcar una hamburguesería para ir algún día es una intención, no una opinión publicada.
 */
export function PerfilPublico() {
  const { username = '' } = useParams()
  const navigate = useNavigate()
  // La pestaña en la dirección, como en el perfil propio: al volver de una ficha se
  // vuelve a la misma.
  const [parametros, setParametros] = useSearchParams()
  const pestania = pestaniaDe(parametros.get('ver'))
  function elegir(nueva: Pestania) {
    setParametros(nueva === 'resenias' ? {} : { ver: nueva }, { replace: true })
  }

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
        <Link to="/feed" className="btn btn-primary btn-sm">
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

  const hayMasResenias = perfil.resenias > perfil.ultimasResenias.length
  const resenias =
    perfil.ultimasResenias.length > 0 ? (
      <ListaDeReseniasDePerfil resenias={perfil.ultimasResenias} />
    ) : (
      <AvisoVacio>
        {perfil.soyYo
          ? 'Todavía no calificaste ninguna hamburguesería.'
          : 'Todavía no calificó ninguna hamburguesería.'}
      </AvisoVacio>
    )

  return (
    <div className="flex flex-col gap-6 p-4 md:mx-auto md:max-w-3xl md:p-0 md:pt-6">
      <CabeceraDePerfil
        username={perfil.username}
        hamburguesa={perfil.hamburguesa}
        cifras={[
          {
            // Lleva a todas sus reseñas (#247), como seguidores lleva a quiénes son.
            valor: cantidad(perfil.resenias),
            etiqueta: perfil.resenias === 1 ? 'reseña' : 'reseñas',
            a: `/u/${perfil.username}/resenias`,
          },
          { valor: perfil.promedio ? nota(perfil.promedio) : '—', etiqueta: 'promedio' },
          {
            valor: cantidad(perfil.seguidores),
            etiqueta: perfil.seguidores === 1 ? 'seguidor' : 'seguidores',
            a: `/u/${perfil.username}/seguidores`,
          },
          {
            // A quiénes sigue (#183): dice mucho de qué le gusta, y es de donde sale gente
            // nueva para seguir. Una cifra más y no una línea aparte debajo del nombre
            // (#232): junto a los seguidores se lee de un vistazo.
            valor: cantidad(perfil.siguiendo),
            etiqueta: 'siguiendo',
            a: `/u/${perfil.username}/siguiendo`,
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

      {/* Con sus guardadas y recorridos a la vista (#264), las mismas pestañas que el
          perfil propio. Si no los muestra, sin pestañas: una sola no es una elección. */}
      {perfil.seVenSusListas ? (
        <div className="flex flex-col gap-4">
          <BarraDePestanias actual={pestania} onElegir={elegir} etiqueta={`Qué ver de @${perfil.username}`} />
          <section
            id={PANEL_DEL_PERFIL}
            role="tabpanel"
            aria-labelledby={`pestania-${pestania}`}
            className="flex flex-col gap-4"
          >
            {pestania === 'guardadas' ? (
              <GuardadasDeOtraPersona username={perfil.username} />
            ) : pestania === 'recorridos' ? (
              <RecorridosDeOtraPersona username={perfil.username} soyYo={perfil.soyYo} />
            ) : (
              <>
                {resenias}
                {hayMasResenias && <VerTodas a={`/u/${perfil.username}/resenias`}>Ver todas</VerTodas>}
              </>
            )}
          </section>
        </div>
      ) : (
        <Seccion titulo="Últimas reseñas" verTodas={hayMasResenias ? `/u/${perfil.username}/resenias` : undefined}>
          {resenias}
        </Seccion>
      )}

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
