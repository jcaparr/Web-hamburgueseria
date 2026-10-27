import { useEffect, useId, useRef } from 'react'

/**
 * La pregunta antes de hacer algo que no se puede deshacer (#351), en lugar del cuadro
 * nativo del navegador ("burgometro.com.ar dice…"), que es gris, no sigue el tema y en el
 * celular se ve como un cuerpo extraño.
 *
 * Es un `<dialog>` con showModal por lo mismo que la ventana de reseñar: el navegador
 * atrapa el foco, deja inerte lo de atrás y se encarga del Esc. El foco arranca en
 * "seguir", la opción que no pierde nada, para que un Enter apurado no borre lo escrito.
 * Se abre encima de otra ventana: va a la capa superior y no necesita z-index.
 */
export function DialogoDeConfirmacion({
  abierto,
  titulo,
  texto,
  textoDeSeguir,
  textoDeConfirmar,
  onSeguir,
  onConfirmar,
}: {
  abierto: boolean
  titulo: string
  texto: string
  /** El botón que deja todo como estaba. */
  textoDeSeguir: string
  /** El botón que sigue adelante: nombra lo que hace. */
  textoDeConfirmar: string
  onSeguir: () => void
  onConfirmar: () => void
}) {
  const dialogo = useRef<HTMLDialogElement>(null)
  const idDelTitulo = useId()
  const idDelTexto = useId()

  // El estado de la ventana lo maneja el navegador, así que hay que empujarlo.
  useEffect(() => {
    const ventana = dialogo.current
    if (!ventana) return
    if (abierto && !ventana.open) ventana.showModal()
    if (!abierto && ventana.open) ventana.close()
  }, [abierto])

  return (
    <dialog
      ref={dialogo}
      role="alertdialog"
      aria-labelledby={idDelTitulo}
      aria-describedby={idDelTexto}
      className="modal"
      // El Esc es "seguir". Se frena para que no cierre el dialog por su cuenta y no suba
      // a la ventana de abajo, que lo tomaría como su propio Esc.
      onCancel={(e) => {
        e.preventDefault()
        e.stopPropagation()
        onSeguir()
      }}
    >
      <div className="modal-box flex max-w-sm flex-col gap-4">
        <div className="flex flex-col gap-2">
          <h2 id={idDelTitulo} className="font-display text-lg font-bold">
            {titulo}
          </h2>
          <p id={idDelTexto} className="text-base-content/80">
            {texto}
          </p>
        </div>
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <button type="button" onClick={onConfirmar} className="btn btn-ghost min-h-11 text-error">
            {textoDeConfirmar}
          </button>
          <button type="button" autoFocus onClick={onSeguir} className="btn btn-primary min-h-11">
            {textoDeSeguir}
          </button>
        </div>
      </div>
      {/* El clic afuera es "seguir": nadie pierde lo escrito por tocar al costado. */}
      <form method="dialog" className="modal-backdrop">
        <button type="button" tabIndex={-1} aria-hidden="true" onClick={onSeguir}>
          Seguir
        </button>
      </form>
    </dialog>
  )
}
