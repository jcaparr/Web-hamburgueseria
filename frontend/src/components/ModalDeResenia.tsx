import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { apiClient } from '../api/client'
import { useAuth } from '../context/useAuth'
import { SelectorDeFotos, type FotosElegidas } from './SelectorDeFotos'
import { Stars } from './Stars'
import type { Rating } from '../types'
import { avisarXpGanada } from '../utils/avisosDeXp'
import { isSessionExpired } from '../utils/errors'
import {
  LETRAS_MINIMAS,
  medirComentario,
  nivelDe,
  PALABRAS_MINIMAS,
  XP_CON_COMENTARIO,
  XP_DE_BIENVENIDA,
  xpDeComentario,
  xpSinLaResenia,
} from '../utils/niveles'

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
  const { user, refrescar } = useAuth()
  // Arranca con lo que ya tenía la reseña, si había. Quien la abre le cambia la key a
  // la ventana cada vez que la abre, así que siempre se arma de cero: antes se volvía
  // a llenar desde un efecto al abrirse, un dibujo de más (#109).
  const [score, setScore] = useState(miResenia?.score ?? 0)
  const [comment, setComment] = useState(miResenia?.comment ?? '')
  const [fotos, setFotos] = useState<FotosElegidas>({ quedan: miResenia?.fotos ?? [], nuevas: [] })
  const [guardando, setGuardando] = useState(false)
  const [borrando, setBorrando] = useState(false)
  // Con el campo al que se refiere, si es de uno: el aviso va al lado de ese campo y no
  // al pie del formulario, donde quedaba lejos de lo que había que corregir.
  const [error, setError] = useState<{ texto: string; campo?: 'puntaje' | 'foto' } | null>(null)
  const idDelTitulo = useId()
  const zonaDelPuntaje = useRef<HTMLDivElement>(null)
  const zonaDeLaFoto = useRef<HTMLDivElement>(null)

  // Cuánta XP da lo que está escrito. Sin XP todavía (y sin reseña de este local) es la
  // primera de la cuenta: suma también la bienvenida.
  const xpDeEsta = xpDeComentario(comment)
  const esLaPrimera = !miResenia && (user?.xp ?? 0) === 0
  const xpQueDa = xpDeEsta + (esLaPrimera ? XP_DE_BIENVENIDA : 0)

  // El servidor recalcula la XP al confirmar; se vuelve a pedir quién sos y se avisa lo
  // que cambió, después de cerrar la ventana.
  async function anunciarXp(antes: number) {
    const yo = await refrescar()
    if (!yo) return
    const ganada = yo.xp - antes
    if (ganada <= 0) return
    const nivelAntes = nivelDe(antes)
    const nivelAhora = nivelDe(yo.xp)
    avisarXpGanada({ ganada, nivelNuevo: nivelAhora > nivelAntes ? nivelAhora : null })
  }

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
    fotos.nuevas.length > 0 ||
    fotos.quedan.length !== (miResenia?.fotos.length ?? 0) ||
    score !== (miResenia?.score ?? 0) ||
    comment !== (miResenia?.comment ?? '')

  // El aviso de un campo se va en cuanto se corrige ese campo, no recién al volver a
  // apretar Publicar.
  function elegirPuntaje(nuevo: number) {
    setScore(nuevo)
    setError((previo) => (previo?.campo === 'puntaje' ? null : previo))
  }

  function elegirFotos(elegidas: FotosElegidas) {
    setFotos(elegidas)
    if (elegidas.quedan.length + elegidas.nuevas.length > 0) {
      setError((previo) => (previo?.campo === 'foto' ? null : previo))
    }
  }

  function intentarCerrar() {
    if (guardando || borrando) return
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
    if (fotos.quedan.length + fotos.nuevas.length === 0) {
      setError({ texto: 'Falta la foto: toda reseña lleva al menos una de lo que comiste.', campo: 'foto' })
      zonaDeLaFoto.current?.querySelector<HTMLElement>('input')?.focus()
      return
    }

    setGuardando(true)
    setError(null)
    try {
      // Texto y fotos en el mismo pedido: si una foto no sirve, no se guarda una reseña
      // a medias esperando que alguien vuelva a completarla.
      const cuerpo = new FormData()
      cuerpo.append('score', String(score))
      cuerpo.append('comment', comment)
      fotos.nuevas.forEach((nueva) => cuerpo.append('foto', nueva))
      // Al editar, las que ya tenía y se quedan, en orden. Si no queda ninguna no va el
      // campo, y el servidor entiende que las nuevas reemplazan a todas.
      if (miResenia) fotos.quedan.forEach((url) => cuerpo.append('queda', url))

      if (miResenia) {
        await apiClient.put(`/burger-joints/${localId}/ratings`, cuerpo)
      } else {
        await apiClient.post(`/burger-joints/${localId}/ratings`, cuerpo)
      }

      onGuardada()
      onCerrar()
      void anunciarXp(user?.xp ?? 0)
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

  /**
   * Lo que cuesta borrar en XP, para decirlo antes de confirmar (#312): bajar de nivel o,
   * si no baja, cuántos puntos se van. Con el motor de logros se suman los que se pierden.
   */
  function loQueSePierde(): string {
    if (!user || !miResenia) return ''
    const despues = xpSinLaResenia(user.xp, xpDeComentario(miResenia.comment))
    const nivelDespues = nivelDe(despues)
    if (nivelDespues < nivelDe(user.xp)) return `\n\nVolvés al nivel ${nivelDespues}.`
    return `\n\nPerdés ${user.xp - despues} XP.`
  }

  /**
   * Borrar la reseña propia, con su foto (#180).
   *
   * Pregunta antes porque no hay vuelta atrás: las fotos no se pueden recuperar. Después
   * hace lo mismo que al guardar —quien abrió la ventana vuelve a pedir las reseñas—,
   * y con eso la ficha vuelve a ofrecer escribir una.
   */
  async function borrar() {
    if (!miResenia) return
    if (!confirm(`¿Borrar tu reseña de ${nombreDelLocal}? Se pierden la nota, el comentario y las fotos, y no se puede deshacer.${loQueSePierde()}`)) return

    setBorrando(true)
    setError(null)
    try {
      await apiClient.delete(`/burger-joints/${localId}/ratings`)
      onGuardada()
      onCerrar()
      void refrescar()
    } catch (err: any) {
      if (isSessionExpired(err)) {
        onSesionVencida()
      } else {
        setError({ texto: err.response?.data?.error ?? 'No pudimos borrar tu reseña. Probá de nuevo.' })
      }
    } finally {
      setBorrando(false)
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

        {/* Las tres partes de reseñar, cada una con su lugar: la nota, las fotos y lo que
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
            <span className="text-sm font-semibold">Las fotos</span>
            <SelectorDeFotos yaSubidas={miResenia?.fotos ?? []} onCambio={elegirFotos} />
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

          {/* Cuánta XP da lo que escribiste, mientras lo escribís. */}
          <XpDeLaResenia xp={xpQueDa} comentario={comment} editando={!!miResenia} primera={esLaPrimera} />

          {error && !error.campo && (
            <p role="alert" className="text-sm text-error">
              {error.texto}
            </p>
          )}

          {/* El mismo verbo en el botón y mientras trabaja: "Publicar" se vuelve
              "Publicando…", no "Guardando…". */}
          <button type="submit" disabled={guardando || borrando} className="btn btn-primary">
            {miResenia
              ? guardando ? 'Guardando…' : 'Guardar cambios'
              : guardando ? 'Publicando…' : 'Publicar reseña'}
          </button>

          {/* Debajo y sin relleno: está a mano para quien la busca, pero no compite con
              guardar, que es lo que viene a hacer casi todo el que abre la ventana. */}
          {miResenia && (
            <button
              type="button"
              onClick={borrar}
              disabled={guardando || borrando}
              className="btn btn-ghost text-error"
            >
              {borrando ? 'Borrando…' : 'Borrar reseña'}
            </button>
          )}
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

/**
 * La XP que da la reseña, al lado de la pista de cuánto falta para los 100 (#312).
 *
 * Con foto sola son 70; con un comentario de 80 letras y 10 palabras, 100. La primera
 * reseña de la cuenta suma además la bienvenida. Al editar no se dice "+": la reseña ya
 * estaba, y lo que se muestra es lo que vale ahora.
 */
function XpDeLaResenia({
  xp,
  comentario,
  editando,
  primera,
}: {
  xp: number
  comentario: string
  editando: boolean
  primera: boolean
}) {
  const { letras, palabras } = medirComentario(comentario)
  const faltaLetras = Math.max(0, LETRAS_MINIMAS - letras)
  const faltaPalabras = Math.max(0, PALABRAS_MINIMAS - palabras)
  const completa = xpDeComentario(comentario) === XP_CON_COMENTARIO

  const faltan = [
    faltaLetras > 0 ? `${faltaLetras} ${faltaLetras === 1 ? 'letra' : 'letras'}` : null,
    faltaPalabras > 0 ? `${faltaPalabras} ${faltaPalabras === 1 ? 'palabra' : 'palabras'}` : null,
  ].filter(Boolean)
  const pista = completa
    ? 'Reseña completa: vale 100 XP.'
    : letras === 0
      ? 'Con un comentario de 80 letras y 10 palabras, la reseña vale 100 XP.'
      : `Te faltan ${faltan.join(' y ')} para que la reseña valga 100 XP.`

  return (
    <div className="flex items-center justify-between gap-3 rounded-field bg-base-200 px-3 py-2.5 text-sm">
      <p className="min-w-0 text-base-content/70" aria-live="polite">
        {pista}
        {primera && <span className="block">Incluye {XP_DE_BIENVENIDA} XP de bienvenida.</span>}
      </p>
      <strong className="flex-none whitespace-nowrap rounded-full bg-[#f2b705] px-3 py-1 font-bold tabular-nums text-[#3a2a1e]">
        {editando ? `Vale ${xp} XP` : `+${xp} XP`}
      </strong>
    </div>
  )
}
