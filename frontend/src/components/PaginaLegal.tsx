import { useId, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { CONTACTO, ULTIMA_ACTUALIZACION } from '../utils/legal'

/**
 * El molde de las páginas legales: el título, la fecha y el texto a un ancho de lectura.
 *
 * Son textos para leer de corrido, así que van con la letra un punto más grande que el
 * resto de la app y renglones de no más de unos setenta caracteres.
 */
export function PaginaLegal({
  titulo,
  bajada,
  children,
}: {
  titulo: string
  bajada: string
  children: ReactNode
}) {
  return (
    <article className="flex flex-col gap-6 p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
      <header className="flex flex-col gap-2">
        <h1 className="titulo-pagina">{titulo}</h1>
        <p className="text-base-content/80">{bajada}</p>
        <p className="text-xs text-base-content/70">Última actualización: {ULTIMA_ACTUALIZACION}</p>
      </header>
      <div className="flex flex-col gap-6 leading-relaxed">{children}</div>
      <footer className="border-t border-base-content/10 pt-4 text-sm text-base-content/70">
        ¿Dudas? Escribinos a <Correo />. Ver también los{' '}
        <Link to="/terminos" className="link">
          términos y condiciones
        </Link>{' '}
        y la{' '}
        <Link to="/privacidad" className="link">
          política de privacidad
        </Link>
        .
      </footer>
    </article>
  )
}

/** Una parte del texto, con su título. */
export function Apartado({ titulo, children }: { titulo: string; children: ReactNode }) {
  const id = useId()
  return (
    <section aria-labelledby={id} className="flex flex-col gap-2">
      <h2 id={id} className="font-display text-lg font-bold">
        {titulo}
      </h2>
      {children}
    </section>
  )
}

/** Una lista dentro de un apartado. */
export function Lista({ children }: { children: ReactNode }) {
  return <ul className="flex list-disc flex-col gap-1.5 pl-5 marker:text-base-content/50">{children}</ul>
}

/** La dirección de contacto, como enlace que abre el correo. */
export function Correo() {
  return (
    <a href={`mailto:${CONTACTO}`} className="link font-semibold">
      {CONTACTO}
    </a>
  )
}
