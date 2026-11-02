import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { apiClient } from '../api/client'
import { useAuth } from '../context/useAuth'
import { useNombreDeUsuario } from '../hooks/useNombreDeUsuario'
import { CampoNombreDeUsuario } from './CampoNombreDeUsuario'
import { IconPencil } from './icons'

/** Lo que contesta GET /profile/username: desde cuándo puede cambiarlo, o null si ya puede. */
interface EstadoDelNombre {
  puedeCambiarDesde: string | null
  texto: string | null
}

/**
 * El lápiz junto al nombre de usuario, y la ventana que abre (#380).
 *
 * El nombre se puede cambiar una vez cada 30 días. La ventana muestra el nombre de ahora
 * y, debajo, el campo del nuevo. Si todavía no corresponde, en vez del campo dice desde
 * cuándo: el plazo lo decide el servidor, acá solo se avisa. Es un `<dialog>` con
 * showModal, como DialogoDeConfirmacion: el navegador atrapa el foco y se encarga del Esc.
 */
export function BotonCambiarNombreDeUsuario() {
  const { user, cambiarNombreDeUsuario } = useAuth()
  const dialogo = useRef<HTMLDialogElement>(null)
  const idDelTitulo = useId()
  const campo = useNombreDeUsuario('', user?.username)
  const [abierto, setAbierto] = useState(false)
  const [estado, setEstado] = useState<EstadoDelNombre | null>(null)
  const [falloElEstado, setFalloElEstado] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // El estado del <dialog> lo maneja el navegador, así que hay que empujarlo.
  useEffect(() => {
    const ventana = dialogo.current
    if (!ventana) return
    if (abierto && !ventana.open) ventana.showModal()
    if (!abierto && ventana.open) ventana.close()
  }, [abierto])

  // Cada vez que se abre se pregunta de nuevo: el plazo puede haber cambiado desde la vez anterior.
  useEffect(() => {
    if (!abierto) return
    let vigente = true
    apiClient
      .get<EstadoDelNombre>('/profile/username')
      .then(({ data }) => vigente && setEstado(data))
      .catch(() => vigente && setFalloElEstado(true))
    return () => {
      vigente = false
    }
  }, [abierto])

  if (!user) return null

  function abrir() {
    campo.escribir('')
    setEstado(null)
    setFalloElEstado(false)
    setError(null)
    setAbierto(true)
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    if (guardando || campo.valor.length < 3 || campo.valor === user!.username) return
    setGuardando(true)
    setError(null)
    try {
      await cambiarNombreDeUsuario(campo.valor)
      setAbierto(false)
    } catch (err: any) {
      const data = err?.response?.data
      if (campo.rechazar(err)) {
        // Queda marcado en el campo, que es donde se arregla.
      } else if (data?.code === 'USERNAME_COOLDOWN') {
        setEstado({ puedeCambiarDesde: data.puedeCambiarDesde, texto: null })
      } else {
        setError('No pudimos guardar el cambio. Probá de nuevo.')
      }
    } finally {
      setGuardando(false)
    }
  }

  const puede = estado !== null && estado.puedeCambiarDesde === null

  return (
    <>
      <button
        type="button"
        onClick={abrir}
        aria-label="Cambiar nombre de usuario"
        className="btn btn-ghost btn-square min-h-11 min-w-11 shrink-0"
      >
        <IconPencil size={18} />
      </button>

      <dialog
        ref={dialogo}
        aria-labelledby={idDelTitulo}
        className="modal"
        // Si el navegador la cierra por su cuenta (Esc), `abierto` no puede quedar en true
        // con la ventana cerrada: la próxima vez no se volvería a abrir.
        onClose={(e) => {
          e.stopPropagation()
          setAbierto(false)
        }}
      >
        <form onSubmit={guardar} className="modal-box flex max-w-sm flex-col gap-4">
          <h2 id={idDelTitulo} className="font-display text-lg font-bold">
            Cambiar nombre de usuario
          </h2>

          <div className="flex flex-col gap-1">
            <span className="text-sm font-semibold">Nombre actual</span>
            <span className="min-h-11 content-center break-all text-base-content/80">@{user.username}</span>
          </div>

          {puede ? (
            <CampoNombreDeUsuario
              campo={campo}
              autoFocus
              cambiando
              etiqueta="Nuevo nombre"
              ayuda="Después no vas a poder cambiarlo hasta dentro de 30 días."
            />
          ) : falloElEstado ? (
            <p role="alert" className="text-sm text-error">
              No pudimos ver si podés cambiarlo. Cerrá la ventana y probá de nuevo.
            </p>
          ) : estado ? (
            <p role="status" className="text-base-content/80">
              Podés volver a cambiarlo el {estado.texto ?? fechaEnPalabras(estado.puedeCambiarDesde)}.
            </p>
          ) : (
            <p role="status" className="min-h-11 content-center text-sm text-base-content/70">
              Cargando…
            </p>
          )}

          {error && (
            <p role="alert" className="text-sm text-error">
              {error}
            </p>
          )}

          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <button type="button" onClick={() => setAbierto(false)} className="btn btn-ghost min-h-11">
              {puede ? 'Cancelar' : 'Cerrar'}
            </button>
            {puede && (
              <button
                type="submit"
                disabled={guardando || campo.valor.length < 3 || campo.valor === user.username || campo.chequeando}
                className="btn btn-primary min-h-11"
              >
                {guardando ? 'Guardando…' : 'Guardar'}
              </button>
            )}
          </div>
        </form>
        {/* El clic afuera cierra: no se pierde nada, el nombre no cambió. */}
        <form method="dialog" className="modal-backdrop">
          <button type="button" tabIndex={-1} aria-hidden="true" onClick={() => setAbierto(false)}>
            Cerrar
          </button>
        </form>
      </dialog>
    </>
  )
}

/** Solo si el servidor mandó la fecha y no el texto: "2026-12-01" pasa a "1 de diciembre". */
function fechaEnPalabras(iso: string | null): string {
  if (!iso) return 'más adelante'
  const [anio, mes, dia] = iso.split('-').map(Number)
  return new Date(anio, mes - 1, dia).toLocaleDateString('es-AR', { day: 'numeric', month: 'long' })
}
