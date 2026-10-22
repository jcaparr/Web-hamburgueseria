import { Link } from 'react-router-dom'
import { LinksDeRedes } from './LinksDeRedes'

/**
 * El pie de todas las pantallas (#297): las redes y los textos legales, nada más.
 *
 * En el teléfono lleva el relleno que deja pasar la barra de abajo, que flota encima
 * (antes lo ponía el contenido de la página).
 */
export function PieDeLaPagina() {
  const enlace = 'inline-flex min-h-11 items-center rounded-field px-2 text-base-content/70 hover:text-base-content hover:underline md:px-3'
  return (
    <footer className="mx-auto flex w-full max-w-[480px] flex-col items-center border-t border-base-content/10 px-4 pb-[calc(4.5rem_+_env(safe-area-inset-bottom))] pt-3 md:max-w-[75rem] md:flex-row md:justify-between md:px-6 md:pb-4">
      <LinksDeRedes />
      <nav aria-label="Legales" className="flex flex-wrap justify-center text-sm">
        <Link to="/terminos" className={enlace}>
          Términos y condiciones
        </Link>
        <Link to="/privacidad" className={enlace}>
          Política de privacidad
        </Link>
      </nav>
    </footer>
  )
}
