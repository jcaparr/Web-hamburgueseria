import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { useAuth } from '../context/useAuth'
import { useNombreDeUsuario } from '../hooks/useNombreDeUsuario'
import { isSessionExpired } from '../utils/errors'
import { CampoNombreDeUsuario } from './CampoNombreDeUsuario'
import { DialogoDeConfirmacion } from './DialogoDeConfirmacion'
import { IconPencil } from './icons'

/** Lo que contesta GET /profile/username: desde cuándo puede cambiarlo, o null si ya puede. */
interface EstadoDelNombre {
  puedeCambiarDesde: string | null
  texto: string | null
}

/** Cada cuánto se puede cambiar. Lo decide el servidor; acá es solo para el aviso. */
const DIAS_ENTRE_CAMBIOS = 30

/**
 * El lápiz junto al nombre de usuario, y lo que abre (#380).
 *
 * En dos pasos, como eligió Juan: primero se escribe el nombre nuevo, y después una
 * confirmación que dice que por 30 días no se va a poder volver atrás. Es un cambio que
 * casi nunca se hace y que traba un mes: un toque más vale la pena.
 *
 * El estado se pide al tocar el lápiz y la ventana se abre recién con la respuesta: así
 * no salta de alto mientras carga, y si todavía no puede cambiarlo abre directo el aviso
 * con la fecha. El plazo lo decide el servidor; acá solo se avisa.
 *
 * En el teléfono la ventana sube desde abajo, como la de reseñar: centrada, el teclado
 * la empujaba.
 */
export function BotonCambiarNombreDeUsuario() {
  const { user, cambiarNombreDeUsuario } = useAuth()
  const navigate = useNavigate()
  const dialogo = useRef<HTMLDialogElement>(null)
  const formulario = useRef<HTMLFormElement>(null)
  const idDelTitulo = useId()
  const campo = useNombreDeUsuario('', user?.username)
  const [abierto, setAbierto] = useState(false)
  const [preguntando, setPreguntando] = useState(false)
  const [estado, setEstado] = useState<EstadoDelNombre | null>(null)
  const [falloElEstado, setFalloElEstado] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const [guardando, setGuardando] = useState(false)
  // Si el plazo llegó recién al guardar: el nombre escrito no se guardó y hay que decirlo.
  const [noSeGuardo, setNoSeGuardo] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [listo, setListo] = useState<string | null>(null)

  // El estado del <dialog> lo maneja el navegador, así que hay que empujarlo.
  useEffect(() => {
    const ventana = dialogo.current
    if (!ventana) return
    if (abierto && !ventana.open) ventana.showModal()
    if (!abierto && ventana.open) ventana.close()
  }, [abierto])

  // El aviso de "listo" se va solo.
  useEffect(() => {
    if (!listo) return
    const espera = window.setTimeout(() => setListo(null), 5000)
    return () => window.clearTimeout(espera)
  }, [listo])

  if (!user) return null

  /** @return false si la sesión venció y ya se mandó a entrar de nuevo. */
  async function preguntarEstado(): Promise<boolean> {
    setPreguntando(true)
    setFalloElEstado(false)
    try {
      const { data } = await apiClient.get<EstadoDelNombre>('/profile/username')
      setEstado(data)
    } catch (err) {
      if (isSessionExpired(err)) {
        navigate('/login')
        return false
      }
      setFalloElEstado(true)
    } finally {
      setPreguntando(false)
    }
    return true
  }

  async function abrir() {
    if (preguntando) return
    campo.escribir('')
    setEstado(null)
    setNoSeGuardo(false)
    setError(null)
    setListo(null)
    if (await preguntarEstado()) setAbierto(true)
  }

  function cerrar() {
    // Mientras se guarda no se cierra: la respuesta tiene que tener dónde mostrarse.
    if (!guardando) setAbierto(false)
  }

  /** Primer paso: el nombre está bien escrito, falta confirmarlo. */
  function seguir(e: FormEvent) {
    e.preventDefault()
    // Si ya se sabe que está tomado, o se está preguntando, confirmar sería pedir un 409.
    if (campo.valor.length < 3 || campo.problema || campo.chequeando) return
    if (campo.valor === user!.username) {
      setError('Ese ya es tu nombre. Escribí uno distinto.')
      return
    }
    setError(null)
    setConfirmando(true)
  }

  /** Segundo paso: se guarda. */
  async function guardar() {
    setConfirmando(false)
    setGuardando(true)
    setError(null)
    try {
      await cambiarNombreDeUsuario(campo.valor)
      // Se cierra ya y no en el efecto: con la ventana abierta, lo de afuera está inerte y
      // el lector de pantalla podía no anunciar el aviso de "listo".
      dialogo.current?.close()
      setAbierto(false)
      setListo(campo.valor)
    } catch (err: any) {
      // El "Seguir" de donde vino el foco está deshabilitado mientras guarda: sin esto, el foco
      // caía al body y quien usa teclado quedaba perdido.
      window.setTimeout(() => formulario.current?.querySelector('input')?.focus())
      const data = err?.response?.data
      if (campo.rechazar(err)) {
        // Queda marcado en el campo, que es donde se arregla.
      } else if (data?.code === 'USERNAME_COOLDOWN') {
        setEstado({
          puedeCambiarDesde: data.puedeCambiarDesde ?? null,
          texto: null,
        })
        setNoSeGuardo(true)
      } else if (isSessionExpired(err)) {
        navigate('/login')
      } else {
        setError('No pudimos guardar el cambio. Probá de nuevo.')
      }
    } finally {
      setGuardando(false)
    }
  }

  // Sin estado no se sabe si puede: con el error a la vista, se ofrece reintentar.
  const puede = estado !== null && estado.puedeCambiarDesde === null

  return (
    <>
      <button
        type="button"
        onClick={abrir}
        aria-label="Cambiar nombre de usuario"
        aria-busy={preguntando}
        disabled={preguntando || guardando}
        className="btn btn-ghost btn-square min-h-11 min-w-11"
      >
        {preguntando ? (
          <span className="loading loading-spinner loading-sm" aria-hidden="true" />
        ) : (
          <IconPencil size={18} />
        )}
      </button>

      {/* Fuera de la cabecera: el lápiz va en una caja corrida con transform, y adentro de ella
          la ventana y el aviso se ubicaban contra esa caja y no contra la pantalla, y además
          heredaban el texto centrado del perfil. */}
      {createPortal(
        <>
          <dialog
            ref={dialogo}
            aria-labelledby={idDelTitulo}
            className="modal modal-bottom sm:modal-middle text-left"
            // Si el navegador la cierra por su cuenta, `abierto` no puede quedar en true con la
            // ventana cerrada: la próxima vez no se volvería a abrir.
            onClose={(e) => {
              e.stopPropagation()
              // Chrome deja frenar el Esc una sola vez: con un segundo Esc mientras se guarda, se
              // vuelve a abrir para que la respuesta tenga dónde mostrarse.
              if (guardando) {
                dialogo.current?.showModal()
                return
              }
              setAbierto(false)
            }}
            onCancel={(e) => {
              if (guardando) e.preventDefault()
            }}
          >
            {puede ? (
              <form ref={formulario} onSubmit={seguir} className="modal-box flex flex-col gap-5 sm:max-w-sm">
                <div className="flex flex-col gap-1">
                  <h2 id={idDelTitulo} className="font-display text-lg font-bold">
                    Cambiar nombre de usuario
                  </h2>
                  <p className="text-sm text-base-content/70">Hoy sos @{user.username}</p>
                </div>

                <CampoNombreDeUsuario
                  campo={campo}
                  autoFocus
                  cambiando
                  etiqueta="Nuevo nombre"
                  ayuda="De 3 a 20 caracteres: letras, números y guion bajo."
                />

                {error && (
                  <p role="alert" className="text-sm text-error">
                    {error}
                  </p>
                )}

                <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                  <button type="button" onClick={cerrar} className="btn btn-ghost min-h-11">
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    disabled={guardando || campo.valor.length < 3 || campo.problema !== null}
                    className="btn btn-primary min-h-11"
                  >
                    {guardando ? 'Guardando…' : 'Seguir'}
                  </button>
                </div>
              </form>
            ) : (
              <div className="modal-box flex flex-col gap-5 sm:max-w-sm">
                <h2 id={idDelTitulo} className="font-display text-lg font-bold">
                  Cambiar nombre de usuario
                </h2>
                {estado ? (
                  <p role={noSeGuardo ? 'alert' : undefined} className="text-base-content/80">
                    {noSeGuardo && 'No se guardó. '}
                    Ya lo cambiaste hace poco. Se puede una vez cada {DIAS_ENTRE_CAMBIOS} días: {cuandoPuede(estado)}.
                  </p>
                ) : (
                  <p role="alert" className="text-error">
                    {falloElEstado ? 'No pudimos ver si podés cambiarlo.' : 'Algo salió mal.'}
                  </p>
                )}
                <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                  {!estado && (
                    <button type="button" onClick={cerrar} className="btn btn-ghost min-h-11">
                      Cerrar
                    </button>
                  )}
                  {estado ? (
                    <button type="button" onClick={cerrar} className="btn btn-primary min-h-11" autoFocus>
                      Entendido
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={preguntarEstado}
                      disabled={preguntando}
                      className="btn btn-primary min-h-11"
                    >
                      {preguntando ? 'Probando…' : 'Reintentar'}
                    </button>
                  )}
                </div>
              </div>
            )}
            {/* El clic afuera cierra, salvo mientras se guarda. */}
            <form method="dialog" className="modal-backdrop">
              <button type="button" tabIndex={-1} aria-hidden="true" onClick={cerrar}>
                Cerrar
              </button>
            </form>

            {/* Adentro de la ventana para que quede arriba de ella, en la misma capa. */}
            <DialogoDeConfirmacion
              abierto={confirmando}
              titulo={`¿Pasar a @${campo.valor}?`}
              texto={`Vas a tener que esperar ${DIAS_ENTRE_CAMBIOS} días, hasta el ${dentroDe(DIAS_ENTRE_CAMBIOS)}, para volver a cambiarlo.`}
              textoDeSeguir="Volver"
              textoDeConfirmar="Sí, cambiarlo"
              onSeguir={() => setConfirmando(false)}
              onConfirmar={guardar}
              confirmarAlFinal
            />
          </dialog>

          {/* Siempre montado, para que el lector de pantalla anuncie lo que aparece adentro. */}
          {/* Por encima de la barra de abajo del teléfono, contando la zona segura del iPhone. */}
          <div
            role="status"
            className="toast toast-center toast-bottom z-50 mb-[calc(4.5rem+env(safe-area-inset-bottom))] md:mb-4"
          >
            {listo && <div className="alert alert-success text-sm font-semibold">Listo, ahora sos @{listo}.</div>}
          </div>
        </>,
        document.body,
      )}
    </>
  )
}

/** "vas a poder de nuevo el 2 de diciembre", o sin fecha si el servidor no la mandó. */
function cuandoPuede(estado: EstadoDelNombre): string {
  if (estado.texto) return `vas a poder de nuevo el ${estado.texto}`
  if (estado.puedeCambiarDesde) {
    const [anio, mes, dia] = estado.puedeCambiarDesde.split('-').map(Number)
    return `vas a poder de nuevo el ${enPalabras(new Date(anio, mes - 1, dia))}`
  }
  return 'vas a poder de nuevo dentro de unos días'
}

/** Hoy más `dias`, en Buenos Aires: es como cuenta el servidor. */
function dentroDe(dias: number): string {
  // Sin pasar por un texto en inglés: cómo se vuelve a leer eso depende del navegador.
  return new Date(Date.now() + dias * 86_400_000).toLocaleDateString('es-AR', {
    timeZone: 'America/Argentina/Buenos_Aires',
    day: 'numeric',
    month: 'long',
  })
}

function enPalabras(dia: Date): string {
  return dia.toLocaleDateString('es-AR', { day: 'numeric', month: 'long' })
}
