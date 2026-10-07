import { Link } from 'react-router-dom'

/**
 * La línea que dice que al crear una cuenta se aceptan los términos y la política de
 * privacidad, con los enlaces a las dos.
 *
 * Va donde se crea una cuenta: en Crear cuenta, y en Iniciar sesión, porque entrar con
 * Google por primera vez también la crea.
 */
export function AvisoDeTerminos({ accion }: { accion: string }) {
  return (
    <p className="text-xs text-base-content/70">
      {accion} aceptás los{' '}
      <Link to="/terminos" className="link">
        términos y condiciones
      </Link>{' '}
      y la{' '}
      <Link to="/privacidad" className="link">
        política de privacidad
      </Link>
      .
    </p>
  )
}
