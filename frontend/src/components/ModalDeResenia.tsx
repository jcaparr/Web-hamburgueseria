import { useEffect, useRef, useState, type FormEvent } from 'react'
import { apiClient } from '../api/client'
import { SelectorDeFoto } from './SelectorDeFoto'
import { Stars } from './Stars'
import type { Rating } from '../types'
import { isSessionExpired } from '../utils/errors'

/**
 * Reseñar un local, en una ventana sobre la pantalla.
 *
 * Antes era una sección fija en la ficha del local. Ocupaba más alto que la info del
 * local para una acción que se hace una vez por lugar, y solo si fuiste: casi todo el
 * que entra está decidiendo si ir, no escribiendo.
 *
 * Es el mismo modal para crear y para editar. Lo que cambia es que llegue una reseña en
 * `miResenia`: si llega, el formulario arranca con lo que ya había y manda un PUT.
 *
 * Es un `<dialog>` y no un div flotante: el navegador se encarga del foco atrapado
 * adentro, del Esc y de dejar inerte lo de atrás, que es todo lo que habría que escribir
 * a mano para que se pueda usar con el teclado.
 */
export function ModalDeResenia({
  abierto,
  localId,
  nombreDelLocal,
  miResenia,
  onCerrar,
  onGuardada,
  onSesionVencida,
}: {
  abierto: boolean
  localId: number | string
  nombreDelLocal: string
  /** La reseña que ya dejaste en este local, si dejaste una. */
  miResenia: Rating | null
  onCerrar: () => void
  onGuardada: () => void
  onSesionVencida: () => void
}) {
  const dialogo = useRef<HTMLDialogElement>(null)
  const [score, setScore] = useState(0)
  const [comment, setComment] = useState('')
  const [foto, setFoto] = useState<File | null>(null)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Al abrir se arranca de cero, o de lo que ya había escrito si es una edición. Va acá
  // y no al cerrar para que un borrador a medias no quede esperando la próxima vez.
  useEffect(() => {
    if (!abierto) return
    setScore(miResenia?.score ?? 0)
    setComment(miResenia?.comment ?? '')
    setFoto(null)
    setError(null)
  }, [abierto, miResenia?.id])

  // El estado de la ventana lo maneja el navegador, así que hay que empujarlo: showModal
  // es lo que da el foco atrapado y el fondo inerte, y no hay forma de pedirlo con JSX.
  useEffect(() => {
    const ventana = dialogo.current
    if (!ventana) return
    if (abierto && !ventana.open) ventana.showModal()
    if (!abierto && ventana.open) ventana.close()
  }, [abierto])

  /**
   * Si hay algo escrito que se perdería al cerrar.
   *
   * Es la diferencia con el formulario de antes: ahí podías scrollear a leer otras
   * reseñas y volver, y tu texto seguía estando. Acá un clic al costado lo borraría, así
   * que mientras haya algo puesto se pregunta.
   */
  const hayAlgoEscrito =
    foto !== null ||
    score !== (miResenia?.score ?? 0) ||
    comment !== (miResenia?.comment ?? '')

  function intentarCerrar() {
    if (guardando) return
    if (hayAlgoEscrito && !confirm('Vas a perder lo que escribiste. ¿Cerramos igual?')) return
    onCerrar()
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()

    if (score === 0) {
      setError('Elegí una calificación de 1 a 5 estrellas')
      return
    }
    // El servidor lo exige igual; avisarlo acá evita mandar el formulario entero para
    // que vuelva rechazado por algo que ya se sabía antes de salir.
    if (!foto && !miResenia?.photoUrl) {
      setError('Toda reseña lleva una foto de lo que comiste')
      return
    }

    setGuardando(true)
    setError(null)
    try {
      // Texto y foto en el mismo pedido: si la foto no sirve, no se guarda una reseña a
      // medias esperando que alguien vuelva a completarla.
      const cuerpo = new FormData()
      cuerpo.append('score', String(score))
      cuerpo.append('comment', comment)
      if (foto) cuerpo.append('foto', foto)

      if (miResenia) {
        await apiClient.put(`/burger-joints/${localId}/ratings`, cuerpo)
      } else {
        await apiClient.post(`/burger-joints/${localId}/ratings`, cuerpo)
      }

      onGuardada()
      onCerrar()
    } catch (err: any) {
      if (isSessionExpired(err)) {
        onSesionVencida()
      } else {
        // El servidor sabe por qué no sirvió esa foto —que no es una imagen, que está
        // dañada, que es enorme— y decirlo es lo único que le permite arreglarlo.
        setError(err.response?.data?.error ?? 'No pudimos guardar tu reseña')
      }
    } finally {
      setGuardando(false)
    }
  }

  return (
    // Pegado abajo en el celular y centrado en pantalla grande: con el teclado abierto,
    // una ventana centrada salta cuando el teclado le come el espacio.
    <dialog
      ref={dialogo}
      className="modal modal-bottom sm:modal-middle"
      // El Esc lo cierra el navegador solo, y eso se llevaría el texto sin preguntar.
      onCancel={(e) => {
        e.preventDefault()
        intentarCerrar()
      }}
    >
      <div className="modal-box flex flex-col gap-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex min-w-0 flex-col">
            <h3 className="font-display text-lg font-bold">
              {miResenia ? 'Editá tu opinión' : 'Dejá tu opinión'}
            </h3>
            <span className="truncate text-sm text-base-content/60">{nombreDelLocal}</span>
          </div>
          <button
            type="button"
            onClick={intentarCerrar}
            aria-label="Cerrar"
            className="btn btn-ghost btn-sm btn-circle flex-none"
          >
            ✕
          </button>
        </div>

        {/* Las tres partes de reseñar, cada una con su lugar: la nota, la foto y lo que
            se quiera contar. Separadas se entiende de un vistazo qué falta. */}
        <form onSubmit={guardar} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <span className="text-xs font-semibold uppercase tracking-wide text-base-content/50">
              Tu puntaje
            </span>
            <Stars value={score} onChange={setScore} size={30} />
          </div>

          <div className="flex flex-col gap-1.5">
            <span className="text-xs font-semibold uppercase tracking-wide text-base-content/50">
              La foto
            </span>
            <SelectorDeFoto
              elegida={foto}
              yaSubida={miResenia?.photoUrl ?? null}
              onElegir={setFoto}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <span className="text-xs font-semibold uppercase tracking-wide text-base-content/50">
              Tu reseña
            </span>
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="¿Qué te pareció?"
              maxLength={1000}
              className="textarea textarea-bordered min-h-24 focus:border-primary"
            />
          </div>

          {error && <p className="text-xs text-error">{error}</p>}

          <button type="submit" disabled={guardando} className="btn btn-primary">
            {guardando ? 'Guardando...' : miResenia ? 'Actualizar reseña' : 'Publicar reseña'}
          </button>
        </form>
      </div>

      {/* El clic afuera pasa por el mismo camino que la cruz y que el Esc, así que
          tampoco se lleva el texto sin avisar. */}
      <form method="dialog" className="modal-backdrop">
        <button type="button" onClick={intentarCerrar}>
          cerrar
        </button>
      </form>
    </dialog>
  )
}
