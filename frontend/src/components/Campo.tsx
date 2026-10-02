import { useId, useState, type InputHTMLAttributes, type ReactNode } from 'react'

/**
 * Un campo de formulario con su nombre arriba, a la vista.
 *
 * Los formularios tenían el nombre solo adentro, como texto de ejemplo: al empezar a
 * escribir desaparecía, y con el gestor de contraseñas completando los dos campos no
 * quedaba a la vista cuál era cuál.
 *
 * @param ayuda lo que conviene saber antes de escribir, como el mínimo de una
 *              contraseña. Va debajo y queda asociado al campo para los lectores de
 *              pantalla.
 */
export function Campo({
  etiqueta,
  ayuda,
  children,
}: {
  etiqueta: string
  ayuda?: ReactNode
  children: (props: { id: string; 'aria-describedby'?: string }) => ReactNode
}) {
  const id = useId()
  const idDeAyuda = `${id}-ayuda`
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-semibold">
        {etiqueta}
      </label>
      {children({ id, 'aria-describedby': ayuda ? idDeAyuda : undefined })}
      {ayuda && (
        <p id={idDeAyuda} className="text-xs text-base-content/70">
          {ayuda}
        </p>
      )}
    </div>
  )
}

/**
 * Una contraseña que se puede mostrar.
 *
 * En el teléfono es fácil errarle a una tecla sin darse cuenta, y con los puntos no
 * hay forma de ver qué se escribió: el error recién aparece al intentar entrar.
 */
export function CampoDeContrasenia({
  etiqueta,
  ayuda,
  ...props
}: { etiqueta: string; ayuda?: ReactNode } & Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>) {
  const [visible, setVisible] = useState(false)
  return (
    <Campo etiqueta={etiqueta} ayuda={ayuda}>
      {(campo) => (
        <div className="input input-bordered flex items-center gap-2 pr-1 focus-within:border-primary">
          <input
            {...props}
            {...campo}
            type={visible ? 'text' : 'password'}
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            className="grow"
          />
          <button
            type="button"
            onClick={() => setVisible((v) => !v)}
            aria-pressed={visible}
            aria-controls={campo.id}
            className="btn btn-ghost btn-xs h-8 px-2 font-semibold"
          >
            {visible ? 'Ocultar' : 'Mostrar'}
          </button>
        </div>
      )}
    </Campo>
  )
}
