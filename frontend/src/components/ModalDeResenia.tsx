import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
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
  // Con el campo al que se refiere, si es de uno: el aviso va al lado de ese campo y no
  // al pie del formulario, donde quedaba lejos de lo que había que corregir.
  const [error, setError] = useState<{ texto: string; campo?: 'puntaje' | 'foto' } | null>(null)
  const idDelTitulo = useId()
  const zonaDelPuntaje = useRef<HTMLDivElement>(null)
  const zonaDeLaFoto = useRef<HTMLDivElement>(null)

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

  // El aviso de un campo se va en cuanto se corrige ese campo, no recién al volver a
  // apretar Publicar.
  function elegirPuntaje(nuevo: number) {
    setScore(nuevo)
    setError((previo) => (previo?.campo === 'puntaje' ? null : previo))
  }

  function elegirFoto(nueva: File | null) {
    setFoto(nueva)
    if (nueva) setError((previo) => (previo?.campo === 'foto' ? null : previo))
  }

  function intentarCerrar() {
    if (guardando) return
    if (hayAlgoEscrito && !confirm('¿Cerrar sin guardar? Vas a perder lo que escribiste.')) return
    onCerrar()
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()

    if (score === 0) {
      setError({ texto: 'Elegí una nota de 1 a 5 estrellas.', campo: 'puntaje' })
      zonaDelPuntaje.current?.querySelector<HTMLElement>('[role=radio]')?.focus()
      return
    }
    // El servidor lo exige igual; avisarlo acá evita mandar el formulario entero para
    // que vuelva rechazado por algo que ya se sabía antes de salir.
    if (!foto && !miResenia?.photoUrl) {
      setError({ texto: 'Falta la foto: toda reseña lleva una de lo que comiste.', campo: 'foto' })
      zonaDeLaFoto.current?.querySelector<HTMLElement>('input')?.focus()
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
        setError({ texto: err.response?.data?.error ?? 'No pudimos guardar tu reseña. Probá de nuevo.' })
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
      aria-labelledby={idDelTitulo}
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
            <h2 id={idDelTitulo} className="font-display text-lg font-bold">
              {miResenia ? 'Editar tu reseña' : 'Escribir una reseña'}
            </h2>
            <span className="truncate text-sm text-base-content/70">{nombreDelLocal}</span>
          </div>
          <button
            type="button"
            onClick={intentarCerrar}
            aria-label="Cerrar"
            className="btn btn-ghost btn-circle -mr-2 -mt-1 flex-none"
          >
            <span aria-hidden="true">✕</span>
          </button>
        </div>

        {/* Las tres partes de reseñar, cada una con su lugar: la nota, la foto y lo que
            se quiera contar. Separadas se entiende de un vistazo qué falta. */}
        <form onSubmit={guardar} className="flex flex-col gap-4">
          <div ref={zonaDelPuntaje} className="flex flex-col gap-1">
            <span className="text-sm font-semibold">Tu puntaje</span>
            <div className="-ml-2">
              <Stars value={score} onChange={elegirPuntaje} size={30} etiqueta="Tu puntaje" />
            </div>
            {error?.campo === 'puntaje' && (
              <p role="alert" className="text-sm text-error">
                {error.texto}
              </p>
            )}
          </div>

          <div ref={zonaDeLaFoto} className="flex flex-col gap-1.5">
            <span className="text-sm font-semibold">La foto</span>
            <SelectorDeFoto
              elegida={foto}
              yaSubida={miResenia?.photoUrl ?? null}
              onElegir={elegirFoto}
            />
            {error?.campo === 'foto' && (
              <p role="alert" className="text-sm text-error">
                {error.texto}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="texto-de-la-resenia" className="text-sm font-semibold">
              Tu reseña <span className="font-normal text-base-content/70">(opcional)</span>
            </label>
            <textarea
              id="texto-de-la-resenia"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="¿Qué te pareció?"
              maxLength={1000}
              className="textarea textarea-bordered min-h-24 focus:border-primary"
            />
          </div>

          {error && !error.campo && (
            <p role="alert" className="text-sm text-error">
              {error.texto}
            </p>
          )}

          {/* El mismo verbo en el botón y mientras trabaja: "Publicar" se vuelve
              "Publicando…", no "Guardando…". */}
          <button type="submit" disabled={guardando} className="btn btn-primary">
            {miResenia
              ? guardando ? 'Guardando…' : 'Guardar cambios'
              : guardando ? 'Publicando…' : 'Publicar reseña'}
          </button>
        </form>
      </div>

      {/* El clic afuera pasa por el mismo camino que la cruz y que el Esc, así que
          tampoco se lleva el texto sin avisar. */}
      <form method="dialog" className="modal-backdrop">
        <button type="button" tabIndex={-1} aria-hidden="true" onClick={intentarCerrar}>
          Cerrar
        </button>
      </form>
    </dialog>
  )
}
