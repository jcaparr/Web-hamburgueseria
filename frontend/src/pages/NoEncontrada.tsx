import { Link } from 'react-router-dom'
import { Personaje } from '../components/Personaje'
import { useNoIndexar } from '../hooks/useMetadatos'
import { useTitulo } from '../hooks/useTitulo'

/**
 * Lo que se ve en una dirección que no existe.
 *
 * Sin esto, una dirección mal escrita —"/perfil" en vez de "/profile", un enlace viejo—
 * dejaba la página en blanco entre las dos barras, sin decir qué pasó ni adónde ir.
 * Burgómetro lo acompaña con el mapa: se perdió, pero no hay que sentirse mal (#365).
 */
export function NoEncontrada() {
  useTitulo('Página no encontrada')
  useNoIndexar()
  return (
    <div className="flex flex-col items-center gap-3 p-4 text-center md:p-0">
      <Personaje pose="mapa" className="h-40 w-auto" />
      <h1 className="titulo-pagina">Esta página no existe</h1>
      <p className="text-sm text-base-content/70">
        Puede que el enlace esté mal escrito o que la página ya no esté.
      </p>
      <Link to="/" className="btn btn-primary min-h-11">
        Ir a Explorar
      </Link>
    </div>
  )
}
