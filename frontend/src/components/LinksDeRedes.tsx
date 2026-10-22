import { IconInstagram, IconTikTok } from './icons'
import { INSTAGRAM, TIKTOK } from '../utils/redes'

/**
 * Los dos botones a las redes (#297), en la tuerca de Ajustes y en el pie.
 *
 * Cada uno lleva su nombre a la vista y no solo el ícono: un ícono suelto no dice a
 * dónde va. Abren en otra pestaña, y el aria-label lo avisa. 44 px de alto y 8 de aire
 * entre uno y otro; el ícono a opacidad plena (13:1) y la palabra en gris, que pasa 4,5:1.
 */
export function LinksDeRedes({ className = '' }: { className?: string }) {
  const clase =
    'inline-flex min-h-11 min-w-11 items-center gap-2 rounded-field px-3 text-sm font-medium text-base-content/70 hover:text-primary'
  return (
    <nav aria-label="Redes sociales" className={`flex flex-wrap gap-2 ${className}`}>
      <a
        href={INSTAGRAM.url}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={`Instagram de Burgómetro, @${INSTAGRAM.usuario} (se abre en una pestaña nueva)`}
        className={clase}
      >
        <IconInstagram className="text-base-content" />
        Instagram
      </a>
      <a
        href={TIKTOK.url}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={`TikTok de Burgómetro, @${TIKTOK.usuario} (se abre en una pestaña nueva)`}
        className={clase}
      >
        <IconTikTok className="text-base-content" />
        TikTok
      </a>
    </nav>
  )
}
