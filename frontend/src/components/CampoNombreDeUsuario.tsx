import { useId } from 'react'
import type { CampoDeNombre } from '../hooks/useNombreDeUsuario'

/**
 * El campo con el que la persona elige cómo la van a encontrar.
 *
 * La arroba va adentro del campo y no en el texto escrito: así lo que se ve es lo que
 * se guarda, y nadie la escribe de más.
 */
export function CampoNombreDeUsuario({
  campo,
  autoFocus,
}: {
  campo: CampoDeNombre
  autoFocus?: boolean
}) {
  const marcado = campo.problema !== null
  const id = useId()

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-semibold">
        Nombre de usuario
      </label>
      {/* Un div y no otra etiqueta: con dos, el lector de pantalla anunciaba el campo
          como "arroba, Nombre de usuario". */}
      <div
        className={`input input-bordered flex items-center gap-0 ${
          marcado ? 'input-error' : 'focus-within:border-primary'
        }`}
      >
        <span aria-hidden="true" className="text-base-content/70">@</span>
        {/* El nombre de usuario se guarda en minúsculas, así que dejar que el teclado
            del teléfono ponga la primera en mayúscula solo consigue que lo escrito no
            se parezca a lo que va a quedar. */}
        <input
          required
          autoFocus={autoFocus}
          autoComplete="username"
          autoCapitalize="none"
          autoCorrect="off"
          spellCheck={false}
          enterKeyHint="next"
          minLength={3}
          value={campo.valor}
          onChange={(e) => campo.escribir(e.target.value)}
          onBlur={campo.chequear}
          id={id}
          placeholder="tu_nombre"
          className="grow"
          aria-invalid={marcado}
          aria-describedby={`${id}-ayuda`}
        />
      </div>

      {marcado ? (
        <p id={`${id}-ayuda`} role="alert" className="text-sm text-error">
          {campo.problema}
          {campo.sugerencia && (
            <>
              {'. '}
              <button
                type="button"
                onClick={campo.usarSugerencia}
                className="link font-semibold text-primary"
              >
                Usar @{campo.sugerencia}
              </button>
            </>
          )}
        </p>
      ) : (
        <p id={`${id}-ayuda`} className="text-xs text-base-content/70">
          Con esto te encuentran tus amigos. Al menos 3 caracteres: letras, números y guion bajo.
        </p>
      )}
    </div>
  )
}
