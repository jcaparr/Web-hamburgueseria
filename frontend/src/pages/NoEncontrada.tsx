import { Link } from 'react-router-dom'
import { useTitulo } from '../hooks/useTitulo'

/**
 * Lo que se ve en una dirección que no existe.
 *
 * Sin esto, una dirección mal escrita —"/perfil" en vez de "/profile", un enlace viejo—
 * dejaba la página en blanco entre las dos barras, sin decir qué pasó ni adónde ir.
 */
export function NoEncontrada() {
  useTitulo('Página no encontrada')
  return (
    <div className="flex flex-col items-start gap-3 p-4 md:p-0">
      <h1 className="font-display text-3xl font-extrabold leading-tight md:text-4xl">Esta página no existe</h1>
      <p className="text-sm text-base-content/70">
        Puede que el enlace esté mal escrito o que la página ya no esté.
      </p>
      <Link to="/" className="btn btn-primary btn-sm">
        Ir a Explorar
      </Link>
    </div>
  )
}
