import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { apiClient } from '../api/client'
import type { Buzon, Notificacion } from '../types'
import { reaccion } from '../utils/reacciones'
import { fechaYHora } from '../utils/fechaYHora'
import { AvatarDeUsuario } from './AvatarDeUsuario'
import { BotonSeguir } from './BotonSeguir'
import { IconBell } from './icons'

/**
 * La campana del buzón de notificaciones, con su panel (#210).
 *
 * El número cuenta lo que llegó desde la última vez que se abrió el panel: quién te
 * siguió y quién reaccionó a tus reseñas. Se pregunta al cargar y en cada cambio de
 * pantalla, que es cuando alguien levanta la vista hacia la barra; no hay un pedido
 * cada tantos segundos gastando batería por algo que puede esperar.
 *
 * Al abrir el panel la campana vuelve a cero en el momento. Lo que acababa de llegar se
 * sigue mostrando como nuevo mientras el panel está abierto, para que se vea qué es lo
 * nuevo; la próxima vez ya está entre lo anterior.
 *
 * Un panel y no otra pantalla: se mira de pasada, sin perder dónde se estaba. Con
 * details, como el selector de orden de Explorar, y lo que details no trae —cerrarse al
 * tocar afuera o con Escape— va a mano.
 */
export function CampanaDeNotificaciones() {
  const desplegable = useRef<HTMLDetailsElement>(null)
  const { pathname } = useLocation()
  const [nuevas, setNuevas] = useState(0)
  const [abierto, setAbierto] = useState(false)
  const [buzon, setBuzon] = useState<Buzon | null>(null)
  const [fallo, setFallo] = useState(false)
  const [intento, setIntento] = useState(0)

  function cerrar() {
    if (desplegable.current) desplegable.current.open = false
  }

  // Cuántas hay sin ver, en cada cambio de pantalla. Un error acá no se muestra: la
  // campana sin número es lo mismo que nada nuevo, y la próxima pantalla vuelve a probar.
  useEffect(() => {
    let vigente = true
    apiClient
      .get<{ nuevas: number }>('/notificaciones/nuevas')
      .then(({ data }) => {
        if (vigente) setNuevas(data.nuevas)
      })
      .catch(() => {})
    return () => {
      vigente = false
    }
  }, [pathname])

  // Tocar un enlace del panel lleva a otra pantalla: el panel se cierra solo.
  useEffect(() => {
    if (desplegable.current) desplegable.current.open = false
  }, [pathname])

  // Al abrirlo, el buzón; y apenas llega, todo queda visto. Se marca después de tenerlo
  // y no antes: si el pedido fallara, lo nuevo no se perdería sin haberse mostrado.
  useEffect(() => {
    if (!abierto) return
    let vigente = true
    apiClient
      .get<Buzon>('/notificaciones')
      .then(({ data }) => {
        if (!vigente) return
        setBuzon(data)
        setFallo(false)
        setNuevas(0)
        if (data.nuevas > 0) apiClient.post('/notificaciones/vistas').catch(() => {})
      })
      .catch(() => {
        if (vigente) setFallo(true)
      })
    return () => {
      vigente = false
    }
  }, [abierto, intento])

  useEffect(() => {
    if (!abierto) return
    function cerrarSiEsAfuera(evento: PointerEvent) {
      if (!desplegable.current?.contains(evento.target as Node)) cerrar()
    }
    function cerrarConEscape(evento: KeyboardEvent) {
      if (evento.key !== 'Escape') return
      cerrar()
      desplegable.current?.querySelector('summary')?.focus()
    }
    document.addEventListener('pointerdown', cerrarSiEsAfuera)
    document.addEventListener('keydown', cerrarConEscape)
    return () => {
      document.removeEventListener('pointerdown', cerrarSiEsAfuera)
      document.removeEventListener('keydown', cerrarConEscape)
    }
  }, [abierto])

  function cambioDeSeguimiento(username: string, loSigo: boolean) {
    setBuzon((previo) =>
      previo && {
        ...previo,
        notificaciones: previo.notificaciones.map((n) =>
          n.tipo === 'SEGUIMIENTO' && n.username === username ? { ...n, loSigo } : n,
        ),
      },
    )
  }

  const avisos = buzon?.notificaciones ?? []
  const lasNuevas = avisos.filter((n) => n.nueva)
  const lasAnteriores = avisos.filter((n) => !n.nueva)

  return (
    <details
      ref={desplegable}
      className="dropdown dropdown-end"
      onToggle={(evento) => setAbierto(evento.currentTarget.open)}
    >
      <summary
        aria-label={nuevas > 0 ? `Notificaciones, ${nuevas} sin ver` : 'Notificaciones'}
        className="btn btn-ghost btn-square relative list-none [&::-webkit-details-marker]:hidden"
      >
        <IconBell size={21} />
        {nuevas > 0 && (
          <span
            aria-hidden="true"
            className="badge badge-primary badge-xs absolute right-1 top-1 h-4 min-w-4 px-1 text-[0.65rem] font-bold tabular-nums"
          >
            {nuevas > 9 ? '9+' : nuevas}
          </span>
        )}
      </summary>

      {/* En el teléfono, de borde a borde debajo de la barra: anclado a la campana se
          salía por la izquierda. En la compu, colgado de la campana. */}
      <div className="dropdown-content z-30 mt-2 flex max-h-[70vh] flex-col overflow-hidden rounded-box bg-base-100 shadow-[var(--sombra-alzada)] max-md:fixed max-md:inset-x-4 max-md:top-16 md:w-96">
        <h2 className="px-4 pb-2 pt-3 font-display text-lg font-bold">Notificaciones</h2>
        <div className="overflow-y-auto px-2 pb-2">
          {fallo ? (
            <div className="flex flex-col items-start gap-2 px-2 pb-2">
              <p className="text-sm text-base-content/70">No pudimos traer tus notificaciones.</p>
              <button type="button" onClick={() => setIntento((n) => n + 1)} className="btn btn-outline btn-sm">
                Reintentar
              </button>
            </div>
          ) : !buzon ? (
            <p role="status" className="px-2 pb-2 text-sm text-base-content/70">
              Cargando…
            </p>
          ) : avisos.length === 0 ? (
            <p className="px-2 pb-2 text-sm text-base-content/70">
              Todavía no tenés notificaciones. Cuando alguien te siga o reaccione a una de tus
              reseñas, te va a aparecer acá.
            </p>
          ) : (
            <>
              {lasNuevas.length > 0 && (
                <Grupo titulo="Nuevas" avisos={lasNuevas} onCambioDeSeguimiento={cambioDeSeguimiento} />
              )}
              {lasAnteriores.length > 0 && (
                <Grupo
                  titulo={lasNuevas.length > 0 ? 'Anteriores' : null}
                  avisos={lasAnteriores}
                  onCambioDeSeguimiento={cambioDeSeguimiento}
                />
              )}
            </>
          )}
        </div>
      </div>
    </details>
  )
}

function Grupo({ titulo, avisos, onCambioDeSeguimiento }: {
  titulo: string | null
  avisos: Notificacion[]
  onCambioDeSeguimiento: (username: string, loSigo: boolean) => void
}) {
  return (
    <section aria-label={titulo ?? 'Notificaciones'}>
      {titulo && <h3 className="px-2 pb-1 pt-2 text-xs font-semibold text-base-content/70">{titulo}</h3>}
      <ul className="flex flex-col">
        {avisos.map((aviso) => (
          <li
            key={`${aviso.tipo}-${aviso.userId}-${aviso.localId ?? ''}-${aviso.cuando}`}
            className={`flex items-center gap-3 rounded-field px-2 py-2 ${aviso.nueva ? 'bg-primary/8' : ''}`}
          >
            <Link to={`/u/${aviso.username}`} tabIndex={-1} aria-hidden="true" className="flex-none">
              <AvatarDeUsuario username={aviso.username} hamburguesa={aviso.hamburguesa} size={36} />
            </Link>
            <p className="min-w-0 flex-1 text-sm leading-snug">
              <Link to={`/u/${aviso.username}`} className="font-semibold hover:underline">
                @{aviso.username}
              </Link>{' '}
              {aviso.tipo === 'SEGUIMIENTO' ? (
                'empezó a seguirte'
              ) : (
                <ReaccionA aviso={aviso} />
              )}
              <span className="block text-xs text-base-content/70">{fechaYHora(aviso.cuando)}</span>
            </p>
            {aviso.tipo === 'SEGUIMIENTO' && (
              <BotonSeguir
                username={aviso.username}
                loSigo={aviso.loSigo}
                onCambio={(loSigo) => onCambioDeSeguimiento(aviso.username, loSigo)}
                chico
              />
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}

/** "reaccionó 🔥 a tu reseña de Bmoodie", con el local enlazado a su ficha. */
function ReaccionA({ aviso }: { aviso: Notificacion }) {
  const cual = aviso.reaccion ? reaccion(aviso.reaccion) : null
  return (
    <>
      reaccionó{' '}
      {cual && (
        <>
          <span aria-hidden="true">{cual.emoji}</span>
          <span className="sr-only">«{cual.nombre}»</span>
        </>
      )}{' '}
      a tu reseña de{' '}
      <Link to={`/burger-joints/${aviso.localId}`} className="font-semibold hover:underline">
        {aviso.localNombre}
      </Link>
    </>
  )
}
