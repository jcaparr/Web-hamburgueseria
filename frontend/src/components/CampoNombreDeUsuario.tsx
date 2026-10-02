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

  return (
    <div className="flex flex-col gap-1">
      <label
        className={`input input-bordered flex items-center gap-0 ${
          marcado ? 'input-error' : 'focus-within:border-primary'
        }`}
      >
        <span className="text-base-content/70">@</span>
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
          placeholder="nombredeusuario"
          className="grow"
          aria-invalid={marcado}
        />
      </label>

      {marcado ? (
        <p className="text-xs text-error">
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
        <p className="text-xs text-base-content/70">
          Con esto te buscan tus amigos. Letras, números y guión bajo.
        </p>
      )}
    </div>
  )
}
