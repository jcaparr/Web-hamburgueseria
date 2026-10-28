import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { apiClient } from '../api/client'
import { useAuth } from '../context/useAuth'
import { Personaje } from './Personaje'
import { BuscadorDeLocal } from './BuscadorDeLocal'
import { DialogoDeConfirmacion } from './DialogoDeConfirmacion'
import { SelectorDeFotos, type FotosElegidas } from './SelectorDeFotos'
import { Stars } from './Stars'
import type { BurgerJoint, DesgloseDeNotas, Rating, ResumenDeResenias } from '../types'
import { avisarXpGanada } from '../utils/avisosDeXp'
import { isSessionExpired } from '../utils/errors'
import { notasConDatos } from '../utils/notasOpcionales'
import {
  nivelDe,
  XP_CON_COMENTARIO,
  XP_DE_BIENVENIDA,
  xpDeComentario,
  xpSinLaResenia,
} from '../utils/niveles'

type Campo = 'local' | 'puntaje' | 'foto' | 'fecha'

/** El local sobre el que se reseña: lo que hace falta para mostrarlo y para mandar. */
interface LocalElegido {
  id: number | string
  nombre: string
  barrio: string | null
}

/** El día de hoy en Buenos Aires, "2026-10-27": es el que cuenta el servidor. */
function hoyEnBuenosAires(): string {
  return new Date().toLocaleDateString('en-CA', { timeZone: 'America/Argentina/Buenos_Aires' })
}

/** Hace un año, para el límite de la fecha de visita. */
function haceUnAnio(hoy: string): string {
  const [anio, mes, dia] = hoy.split('-')
  // El 29 de febrero no existe el año anterior.
  return `${Number(anio) - 1}-${mes}-${mes === '02' && dia === '29' ? '28' : dia}`
}

/**
 * Con qué arranca el formulario: lo que ya tenía la reseña, o nada.
 *
 * Una reseña de antes de la fecha de visita no la tiene: se arranca en el día en que se
 * cargó, que es lo que la tarjeta muestra, así que guardarla sin tocar nada no cambia lo
 * que se ve.
 */
function valoresDe(r: Rating | null) {
  const dia = r ? (r.visitDate ?? new Date(r.createdAt).toLocaleDateString('en-CA', { timeZone: 'America/Argentina/Buenos_Aires' })) : ''
  return {
    score: r?.score ?? 0,
    comment: r?.comment ?? '',
    notaLocal: r?.scoreLocal ?? 0,
    notaAtencion: r?.scoreAtencion ?? 0,
    notaPrecio: r?.scorePrecio ?? 0,
    cuando: r ? ('otro' as const) : ('hoy' as const),
    dia,
  }
}

/**
 * Reseñar un local, en una ventana sobre la pantalla.
 *
 * Es la misma pantalla para el lápiz de la barra de abajo y para el botón de la ficha
 * (#337). Desde la ficha llega con el local ya elegido (`localId`); desde el lápiz llega
 * sin él y arranca con el buscador. Y es la misma para crear y para editar: si el local
 * ya tiene una reseña tuya, el formulario arranca con lo que ya había y manda un PUT (una
 * reseña por local).
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
  /** El local ya elegido, si llega de su ficha. Sin él se muestra el buscador. */
  localId?: number | string
  nombreDelLocal?: string
  /** La reseña que ya dejaste en este local, si dejaste una. */
  miResenia: Rating | null
  onCerrar: () => void
  /**
   * Se avisa con el local reseñado, para que quien abrió la ventana vuelva a pedir lo suyo.
   * Con el id de la reseña cuando es nueva: es a la que lleva "Ver mi reseña".
   */
  onGuardada: (localId: number | string, ratingId?: number) => void
  onSesionVencida: () => void
}) {
  const dialogo = useRef<HTMLDialogElement>(null)
  const { user, refrescar } = useAuth()
  const esFijo = localId !== undefined
  const inicial = valoresDe(miResenia)

  const [local, setLocal] = useState<LocalElegido | null>(
    esFijo ? { id: localId, nombre: nombreDelLocal ?? '', barrio: null } : null,
  )
  // La reseña que se edita. Desde la ficha llega de afuera; con el buscador se pide al
  // elegir el local.
  const [mia, setMia] = useState<Rating | null>(miResenia)
  // La pregunta de "¿cerrar sin guardar?" (#351), en una ventana propia sobre esta.
  const [confirmandoCierre, setConfirmandoCierre] = useState(false)
  const [desglose, setDesglose] = useState<DesgloseDeNotas | null>(null)
  // Arranca con lo que ya tenía la reseña, si había. Quien la abre le cambia la key a
  // la ventana cada vez que la abre, así que siempre se arma de cero: antes se volvía
  // a llenar desde un efecto al abrirse, un dibujo de más (#109).
  const [score, setScore] = useState(inicial.score)
  const [comment, setComment] = useState(inicial.comment)
  const [notaLocal, setNotaLocal] = useState(inicial.notaLocal)
  const [notaAtencion, setNotaAtencion] = useState(inicial.notaAtencion)
  const [notaPrecio, setNotaPrecio] = useState(inicial.notaPrecio)
  const [cuando, setCuando] = useState<'hoy' | 'otro'>(inicial.cuando)
  const [dia, setDia] = useState(inicial.dia)
  const [fotos, setFotos] = useState<FotosElegidas>({ quedan: miResenia?.fotos ?? [], nuevas: [] })
  const [guardando, setGuardando] = useState(false)
  // La reseña nueva ya salió: se muestra el "¡Listo, gracias!" en lugar del formulario.
  const [publicada, setPublicada] = useState<{ ratingId: number } | null>(null)
  const [borrando, setBorrando] = useState(false)
  // Con el campo al que se refiere, si es de uno: el aviso va al lado de ese campo y no
  // al pie del formulario, donde quedaba lejos de lo que había que corregir.
  const [error, setError] = useState<{ texto: string; campo?: Campo } | null>(null)
  const idDelTitulo = useId()
  // Ids propios: en la ficha conviven dos pantallas de reseña, y con ids fijos las
  // etiquetas apuntarían a los campos de la otra.
  const idCuando = useId()
  const idComentario = useId()
  // Cuenta los pedidos de "¿ya la reseñaste?": una respuesta que llega después de haber
  // cambiado de local es de otro local y no se aplica.
  const pedidoDeMia = useRef(0)
  const zonaDelLocal = useRef<HTMLDivElement>(null)
  const [revisando, setRevisando] = useState(false)
  const [fallaRevisar, setFallaRevisar] = useState(false)
  const zonaDelPuntaje = useRef<HTMLDivElement>(null)
  const zonaDeLaFoto = useRef<HTMLDivElement>(null)
  const zonaDeLaFecha = useRef<HTMLDivElement>(null)

  const hoy = hoyEnBuenosAires()
  const base = valoresDe(mia)

  // Cuánta XP da lo que está escrito. Sin XP todavía (y sin reseña de este local) es la
  // primera de la cuenta: suma también la bienvenida.
  const xpDeEsta = xpDeComentario(comment)
  const esLaPrimera = !mia && (user?.xp ?? 0) === 0
  const xpQueDa = xpDeEsta + (esLaPrimera ? XP_DE_BIENVENIDA : 0)

  // Cómo calificaron los demás este local. Es solo información: si no llega, el bloque
  // no se muestra y se reseña igual.
  const idDelLocal = local?.id
  useEffect(() => {
    if (idDelLocal === undefined) return
    let vigente = true
    apiClient
      .get<ResumenDeResenias>(`/burger-joints/${idDelLocal}/ratings/resumen`)
      .then(({ data }) => {
        if (vigente) setDesglose(data.desglose)
      })
      .catch(() => {
        if (vigente) setDesglose(null)
      })
    return () => {
      vigente = false
    }
  }, [idDelLocal])

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
    if (abierto && !ventana.open) {
      ventana.showModal()
      // Sin local elegido lo primero es buscarlo: el cursor ya queda en el buscador.
      ventana.querySelector<HTMLInputElement>('input[type=search]')?.focus()
    }
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
    fotos.quedan.length !== (mia?.fotos.length ?? 0) ||
    score !== base.score ||
    comment !== base.comment ||
    notaLocal !== base.notaLocal ||
    notaAtencion !== base.notaAtencion ||
    notaPrecio !== base.notaPrecio ||
    cuando !== base.cuando ||
    (cuando === 'otro' && dia !== base.dia)

  // El aviso de un campo se va en cuanto se corrige ese campo, no recién al volver a
  // apretar Publicar.
  function sacarAviso(campo: Campo) {
    setError((previo) => (previo?.campo === campo ? null : previo))
  }

  function elegirPuntaje(nuevo: number) {
    setScore(nuevo)
    sacarAviso('puntaje')
  }

  function elegirFotos(elegidas: FotosElegidas) {
    setFotos(elegidas)
    if (elegidas.quedan.length + elegidas.nuevas.length > 0) sacarAviso('foto')
  }

  /** Cada nota opcional se saca volviendo a tocar la misma estrella. */
  function alternar(poner: (n: number) => void, actual: number) {
    return (nueva: number) => poner(nueva === actual ? 0 : nueva)
  }

  /** Vuelca una reseña que ya existía en el formulario, para editarla. */
  function cargar(r: Rating) {
    const v = valoresDe(r)
    setMia(r)
    setScore(v.score)
    setComment(v.comment)
    setNotaLocal(v.notaLocal)
    setNotaAtencion(v.notaAtencion)
    setNotaPrecio(v.notaPrecio)
    setCuando(v.cuando)
    setDia(v.dia)
    setFotos({ quedan: r.fotos, nuevas: [] })
  }

  /** Vuelve al formulario en blanco, para cuando se cambia de local. */
  function vaciar() {
    const v = valoresDe(null)
    setMia(null)
    setScore(v.score)
    setComment(v.comment)
    setNotaLocal(0)
    setNotaAtencion(0)
    setNotaPrecio(0)
    setCuando(v.cuando)
    setDia(v.dia)
    setFotos({ quedan: [], nuevas: [] })
    setError(null)
  }

  /**
   * Si ya reseñaste este local, abre la reseña para editarla: es una reseña por local.
   *
   * Mientras no se sabe no se puede publicar (un POST sobre una reseña que ya existe
   * es un 409), y si no se pudo saber se dice y se deja reintentar.
   */
  async function revisarSiYaReseniaste(id: number | string) {
    const este = ++pedidoDeMia.current
    setRevisando(true)
    setFallaRevisar(false)
    try {
      const { data, status } = await apiClient.get<Rating>(`/burger-joints/${id}/ratings/mine`)
      if (pedidoDeMia.current !== este) return
      if (status === 200 && data) cargar(data)
    } catch (err) {
      if (pedidoDeMia.current !== este) return
      if (isSessionExpired(err)) onSesionVencida()
      else setFallaRevisar(true)
    } finally {
      if (pedidoDeMia.current === este) setRevisando(false)
    }
  }

  function elegirLocal(elegido: BurgerJoint) {
    // El formulario ya está a la vista y puede tener cosas puestas: elegir el local no las
    // borra. Si resulta que ya lo reseñaste, `cargar` las reemplaza por la reseña que había.
    sacarAviso('local')
    setDesglose(null)
    setLocal({ id: elegido.id, nombre: elegido.name, barrio: elegido.area })
    void revisarSiYaReseniaste(elegido.id)
    // El botón que se tocó desaparece: el foco pasa a lo siguiente que hay que hacer.
    requestAnimationFrame(() => zonaDelPuntaje.current?.querySelector<HTMLElement>('[role=radio]')?.focus())
  }

  function cambiarDeLocal() {
    pedidoDeMia.current++
    setRevisando(false)
    setFallaRevisar(false)
    // Si se estaba editando la reseña de ese local, lo cargado era de ella y se va con ella.
    if (mia) vaciar()
    setDesglose(null)
    setLocal(null)
    requestAnimationFrame(() => dialogo.current?.querySelector<HTMLInputElement>('input[type=search]')?.focus())
  }

  /**
   * Cierra el "¡Listo, gracias!". Recién acá se le avisa a quien abrió la ventana: en la
   * ficha, avisarle antes cambia la key de la ventana (llega la reseña propia) y se
   * rearmaría de cero, llevándose la pantalla de gracias.
   */
  function terminar() {
    if (!local) return
    onGuardada(local.id, publicada?.ratingId)
    onCerrar()
  }

  function intentarCerrar() {
    if (guardando || borrando) return
    if (publicada) {
      terminar()
      return
    }
    if (hayAlgoEscrito) {
      setConfirmandoCierre(true)
      return
    }
    onCerrar()
  }

  function cerrarSinGuardar() {
    setConfirmandoCierre(false)
    onCerrar()
  }

  function avisar(texto: string, campo: Campo, zona: { current: HTMLDivElement | null }) {
    setError({ texto, campo })
    zona.current?.querySelector<HTMLElement>('[role=radio], input, button')?.focus()
  }

  async function guardar(e?: FormEvent) {
    e?.preventDefault()
    if (!local) {
      avisar('Elegí el local que querés reseñar.', 'local', zonaDelLocal)
      return
    }
    if (revisando || fallaRevisar) return
    if (score === 0) {
      avisar('Elegí una nota de 1 a 5 estrellas.', 'puntaje', zonaDelPuntaje)
      return
    }
    // El servidor lo exige igual; avisarlo acá evita mandar el formulario entero para
    // que vuelva rechazado por algo que ya se sabía antes de salir.
    if (fotos.quedan.length + fotos.nuevas.length === 0) {
      avisar('Falta la foto: toda reseña lleva al menos una de lo que comiste.', 'foto', zonaDeLaFoto)
      return
    }
    if (cuando === 'otro') {
      if (!dia) {
        avisar('Elegí qué día fuiste.', 'fecha', zonaDeLaFecha)
        return
      }
      if (dia > hoy) {
        avisar('El día no puede ser futuro.', 'fecha', zonaDeLaFecha)
        return
      }
      if (dia < haceUnAnio(hoy) && dia !== base.dia) {
        avisar('Solo se pueden reseñar visitas de hasta un año atrás.', 'fecha', zonaDeLaFecha)
        return
      }
    }

    setGuardando(true)
    setError(null)
    try {
      // Texto y fotos en el mismo pedido: si una foto no sirve, no se guarda una reseña
      // a medias esperando que alguien vuelva a completarla.
      const cuerpo = new FormData()
      cuerpo.append('score', String(score))
      cuerpo.append('comment', comment)
      // "Hoy" no manda fecha: queda la de carga, que es la de hoy. Y las tres notas
      // opcionales van solo si se pusieron.
      // Al editar, "Hoy" se manda: sin el campo el servidor lo borraría y se volvería a
      // mostrar el día en que se cargó la reseña.
      if (cuando === 'otro') cuerpo.append('visitDate', dia)
      else if (mia) cuerpo.append('visitDate', hoy)
      if (notaLocal > 0) cuerpo.append('scoreLocal', String(notaLocal))
      if (notaAtencion > 0) cuerpo.append('scoreAtencion', String(notaAtencion))
      if (notaPrecio > 0) cuerpo.append('scorePrecio', String(notaPrecio))
      fotos.nuevas.forEach((nueva) => cuerpo.append('foto', nueva))
      // Al editar, las que ya tenía y se quedan, en orden. Si no queda ninguna no va el
      // campo, y el servidor entiende que las nuevas reemplazan a todas.
      if (mia) fotos.quedan.forEach((url) => cuerpo.append('queda', url))

      // Editar vuelve a donde estaba; publicar una nueva se agradece (#365). La XP se
      // anuncia igual: el aviso espera a que esta ventana se cierre.
      if (mia) {
        await apiClient.put(`/burger-joints/${local.id}/ratings`, cuerpo)
        onGuardada(local.id)
        onCerrar()
      } else {
        const { data } = await apiClient.post<Rating>(`/burger-joints/${local.id}/ratings`, cuerpo)
        setPublicada({ ratingId: data.id })
      }
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
    if (!user || !mia) return ''
    const despues = xpSinLaResenia(user.xp, xpDeComentario(mia.comment))
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
    if (!mia || !local) return
    if (!confirm(`¿Borrar tu reseña de ${local.nombre}? Se pierden la nota, el comentario y las fotos, y no se puede deshacer.${loQueSePierde()}`)) return

    setBorrando(true)
    setError(null)
    try {
      await apiClient.delete(`/burger-joints/${local.id}/ratings`)
      onGuardada(local.id)
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
      // Un segundo Esc cierra el dialog aunque el primero se haya frenado: se avisa para
      // que `abierto` no quede en true con la ventana cerrada.
      onClose={(e) => {
        // El de la confirmación de adentro (#351) sube hasta acá: no es el de esta ventana.
        if (e.target !== e.currentTarget) return
        if (abierto) onCerrar()
      }}
      // El Esc lo cierra el navegador solo, y eso se llevaría el texto sin preguntar.
      onCancel={(e) => {
        // El Esc de la ventana de "Sugerí un local", que está adentro de esta, sube hasta acá
        // en el árbol de React: no es el de esta ventana y no la tiene que cerrar.
        if (e.target !== e.currentTarget) return
        e.preventDefault()
        intentarCerrar()
      }}
    >
      <div className="modal-box flex flex-col gap-4">
        {publicada && local ? (
          <div role="status" className="flex flex-col items-center gap-3 py-2 text-center">
            <Personaje pose="celu" className="h-40 w-auto" />
            <h2 id={idDelTitulo} className="font-display text-xl font-bold">
              ¡Listo, gracias!
            </h2>
            <p className="text-sm text-base-content/70">
              Tu reseña de {local.nombre} ya está publicada.
            </p>
            <button type="button" onClick={terminar} autoFocus className="btn btn-primary mt-1 w-full">
              Ver mi reseña
            </button>
          </div>
        ) : (
          <>
            <div className="flex items-start justify-between gap-3">
              <h2 id={idDelTitulo} className="font-display text-lg font-bold">
                {mia ? 'Editar tu reseña' : 'Escribir una reseña'}
              </h2>
              <button
                type="button"
                onClick={intentarCerrar}
                aria-label="Cerrar"
                className="btn btn-ghost btn-circle -mr-2 -mt-1 flex-none"
              >
                <span aria-hidden="true">✕</span>
              </button>
            </div>

            {/* Un div y no un <form>: "Sugerí un local" abre su propio formulario, y uno adentro
                del otro mandaría los dos al enviar. Todo el formulario está a la vista desde
                que se abre: el local se elige arriba y se puede completar el resto antes o después. */}
            <div className="flex flex-col gap-4">
              <div ref={zonaDelLocal} className="flex flex-col gap-3">
                {local ? (
                  <>
                    <div className="flex items-center justify-between gap-3 rounded-field bg-base-200 px-3 py-2">
                      <div className="flex min-w-0 flex-col">
                        <span className="truncate font-semibold">{local.nombre}</span>
                        {local.barrio && <span className="truncate text-sm text-base-content/70">{local.barrio}</span>}
                        {mia && !esFijo && (
                          <span role="status" className="text-xs text-base-content/70">
                            Ya la reseñaste: estás editando.
                          </span>
                        )}
                      </div>
                      {!esFijo && (
                        <button type="button" onClick={cambiarDeLocal} className="btn btn-ghost h-11 min-h-11 flex-none px-3">
                          Cambiar
                        </button>
                      )}
                    </div>
                    {fallaRevisar && (
                      <div role="alert" className="flex items-center justify-between gap-2 text-sm text-error">
                        <span>No pudimos revisar si ya reseñaste este local.</span>
                        <button type="button" onClick={() => void revisarSiYaReseniaste(local.id)} className="btn btn-ghost h-11 min-h-11 px-3">
                          Reintentar
                        </button>
                      </div>
                    )}
                    <AsiLoCalificaron desglose={desglose} />
                  </>
                ) : (
                  <BuscadorDeLocal onElegir={elegirLocal} />
                )}
                {error?.campo === 'local' && (
                  <p role="alert" className="text-sm text-error">
                    {error.texto}
                  </p>
                )}
              </div>

              <>
                  <div ref={zonaDelPuntaje} className="flex flex-col gap-1">
                    <span className="text-base font-semibold">¿Cómo estuvo?</span>
                    <div className="-ml-2">
                      <Stars value={score} onChange={elegirPuntaje} size={36} etiqueta="¿Cómo estuvo?" />
                    </div>
                    {error?.campo === 'puntaje' && (
                      <p role="alert" className="text-sm text-error">
                        {error.texto}
                      </p>
                    )}
                  </div>

                  {/* Las tres notas son opcionales y no entran en el promedio: van plegadas
                      para que publicar sea nota y foto, y el detalle lo ponga quien quiere. */}
                  <details
                    // Sin estado propio: lo abre y lo cierra el navegador. Arranca abierto solo
                    // si la reseña que se edita ya tenía notas, y la key lo rearma al cambiar de reseña.
                    key={mia?.id ?? 'nueva'}
                    open={base.notaLocal + base.notaAtencion + base.notaPrecio > 0}
                    className="rounded-field border border-base-300 px-3"
                  >
                    <summary className="flex min-h-12 cursor-pointer items-center justify-between font-semibold">
                      Sumá más detalle
                      <span aria-hidden="true" className="text-base-content/70 transition-transform in-[details[open]]:rotate-180 motion-reduce:transition-none">▾</span>
                    </summary>
                    <div className="flex flex-col gap-1 pb-3">
                      <p className="text-xs text-base-content/70">Tocá otra vez la misma estrella para sacarla.</p>
                      <FilaDeNota rotulo="Local" valor={notaLocal} onCambio={alternar(setNotaLocal, notaLocal)} />
                      <FilaDeNota rotulo="Atención" valor={notaAtencion} onCambio={alternar(setNotaAtencion, notaAtencion)} />
                      <FilaDeNota rotulo="Precio" valor={notaPrecio} onCambio={alternar(setNotaPrecio, notaPrecio)} />
                    </div>
                  </details>

                  <div ref={zonaDeLaFoto} className="flex flex-col gap-1.5">
                    <span className="text-sm font-semibold">Las fotos <span className="font-normal text-base-content/70">(al menos una)</span></span>
                    <SelectorDeFotos key={mia?.id ?? 'nueva'} yaSubidas={mia?.fotos ?? []} onCambio={elegirFotos} />
                    {error?.campo === 'foto' && (
                      <p role="alert" className="text-sm text-error">
                        {error.texto}
                      </p>
                    )}
                  </div>

                  <div ref={zonaDeLaFecha} className="flex flex-col gap-1.5">
                    <span id={idCuando} className="text-sm font-semibold">
                      ¿Cuándo fuiste?
                    </span>
                    <div role="group" aria-labelledby={idCuando} className="flex flex-wrap gap-2">
                      {(
                        [
                          ['hoy', 'Hoy'],
                          ['otro', 'Elegir día'],
                        ] as const
                      ).map(([valor, texto]) => (
                        <button
                          key={valor}
                          type="button"
                          aria-pressed={cuando === valor}
                          onClick={() => {
                            setCuando(valor)
                            if (valor === 'otro' && !dia) setDia(hoy)
                            sacarAviso('fecha')
                          }}
                          className={`btn h-11 min-h-11 rounded-full px-5 ${cuando === valor ? 'btn-secondary' : 'btn-outline'}`}
                        >
                          {texto}
                        </button>
                      ))}
                    </div>
                    {cuando === 'otro' && (
                      <input
                        type="date"
                        value={dia}
                        min={haceUnAnio(hoy)}
                        max={hoy}
                        onChange={(e) => {
                          setDia(e.target.value)
                          sacarAviso('fecha')
                        }}
                        aria-label="Día de la visita"
                        className="input campo h-12 w-full"
                      />
                    )}
                    {error?.campo === 'fecha' && (
                      <p role="alert" className="text-sm text-error">
                        {error.texto}
                      </p>
                    )}
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label htmlFor={idComentario} className="text-sm font-semibold">
                      Tu reseña <span className="font-normal text-base-content/70">(opcional)</span>
                    </label>
                    <textarea
                      id={idComentario}
                      value={comment}
                      onChange={(e) => setComment(e.target.value)}
                      placeholder="¿Qué te pareció?"
                      maxLength={1000}
                      className="textarea campo min-h-24"
                    />
                  </div>

                  {/* Cuánta XP da lo que escribiste, mientras lo escribís. */}
                  <XpDeLaResenia xp={xpQueDa} comentario={comment} editando={!!mia} primera={esLaPrimera} />
              </>

              {error && !error.campo && (
                <p role="alert" className="text-sm text-error">
                  {error.texto}
                </p>
              )}

              {/* El mismo verbo en el botón y mientras trabaja: "Publicar" se vuelve
                  "Publicando…", no "Guardando…". */}
              <button
                type="button"
                onClick={() => void guardar()}
                disabled={guardando || borrando || revisando || fallaRevisar}
                className="btn btn-primary"
              >
                {revisando
                  ? 'Revisando…'
                  : mia
                    ? guardando ? 'Guardando…' : 'Guardar cambios'
                    : guardando ? 'Publicando…' : 'Publicar reseña'}
              </button>

              {/* Debajo y sin relleno: está a mano para quien la busca, pero no compite con
                  guardar, que es lo que viene a hacer casi todo el que abre la ventana. */}
              {mia && (
                <button
                  type="button"
                  onClick={borrar}
                  disabled={guardando || borrando}
                  className="btn btn-ghost text-error"
                >
                  {borrando ? 'Borrando…' : 'Borrar reseña'}
                </button>
              )}
            </div>
          </>
        )}
      </div>

      {/* El clic afuera pasa por el mismo camino que la cruz y que el Esc, así que
          tampoco se lleva el texto sin avisar. */}
      <form method="dialog" className="modal-backdrop">
        <button type="button" tabIndex={-1} aria-hidden="true" onClick={intentarCerrar}>
          Cerrar
        </button>
      </form>

      <DialogoDeConfirmacion
        abierto={confirmandoCierre}
        titulo="¿Cerrar sin guardar?"
        texto="Vas a perder lo que escribiste."
        textoDeSeguir="Seguir escribiendo"
        textoDeConfirmar="Cerrar sin guardar"
        onSeguir={() => setConfirmandoCierre(false)}
        onConfirmar={cerrarSinGuardar}
      />
    </dialog>
  )
}

/** Una nota opcional: el rótulo a la izquierda y las estrellas a la derecha. */
function FilaDeNota({ rotulo, valor, onCambio }: { rotulo: string; valor: number; onCambio: (n: number) => void }) {
  return (
    <div className="flex items-center justify-between gap-2">
      <span className="text-sm font-semibold">{rotulo}</span>
      <Stars value={valor} onChange={onCambio} size={24} etiqueta={rotulo} />
    </div>
  )
}

/** "4,2": un decimal y coma, como se escribe acá. */
function coma(n: number): string {
  return n.toFixed(1).replace('.', ',')
}

/**
 * Lo que dicen los demás de este local en las tres notas opcionales (#337).
 *
 * Solo las que tienen datos; si ninguna, no se muestra nada: un bloque vacío o con
 * guiones sería ruido justo donde hay que decidir cuántas estrellas poner.
 */
function AsiLoCalificaron({ desglose }: { desglose: DesgloseDeNotas | null }) {
  const notas = notasConDatos(desglose)
  if (notas.length === 0) return null

  return (
    <div className="flex flex-col gap-1.5 rounded-field border border-base-300 px-3 py-2">
      <span className="text-sm text-base-content/70">Así lo calificaron</span>
      <ul className="flex">
        {notas.map(({ rotulo, promedio }) => (
          <li key={rotulo} className="flex flex-1 flex-col items-center">
            <span className="font-display text-xl font-bold tabular-nums">{coma(promedio)}</span>
            <span className="text-sm text-base-content/70">{rotulo}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

/**
 * La XP que da la reseña, al lado de la pista de cuánto falta para los 100 (#312).
 *
 * Con foto sola son 70; con un comentario de 5 palabras, 100 (#344). La primera
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
  const completa = xpDeComentario(comentario) === XP_CON_COMENTARIO
  // Sin contar letras ni palabras: se dice qué se gana, no cuánto hay que escribir.
  const pista = completa
    ? 'Reseña completa: vale 100 XP.'
    : comentario.trim() === ''
      ? 'Sumá un comentario y la reseña vale 100 XP en vez de 70.'
      : 'Seguí escribiendo y la reseña vale 100 XP en vez de 70.'

  return (
    <div className="flex items-center justify-between gap-3 rounded-field bg-base-200 px-3 py-2.5 text-sm">
      <p className="min-w-0 text-base-content/70">
        {pista}
        {primera && <span className="block">Incluye {XP_DE_BIENVENIDA} XP de bienvenida.</span>}
      </p>
      <strong className="flex-none whitespace-nowrap rounded-full bg-[#f2b705] px-3 py-1 font-bold tabular-nums text-[#3a2a1e]">
        {editando ? `Vale ${xp} XP` : `+${xp} XP`}
      </strong>
    </div>
  )
}
