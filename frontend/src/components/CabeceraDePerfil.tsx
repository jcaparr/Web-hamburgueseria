import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { AvatarDeUsuario } from './AvatarDeUsuario'

/**
 * La parte de arriba de un perfil: una franja de color con el avatar encima, el nombre
 * y lo que se puede hacer.
 *
 * Es la misma idea que la portada de la ficha de un local —una banda arriba con la
 * tira a cuadros, y lo importante superpuesto—, para que el perfil se sienta de la
 * misma app. El avatar es el de las reseñas, con la inicial y el color de cada uno: el
 * ícono gris de persona que había era igual para todo el mundo.
 *
 * @param bajada  la línea de debajo del nombre: seguidores, una insignia
 * @param acciones los botones, en una fila que reparte el ancho en el teléfono
 */
export function CabeceraDePerfil({
  username,
  bajada,
  acciones,
}: {
  username: string
  bajada?: ReactNode
  acciones?: ReactNode
}) {
  return (
    <div className="flex flex-col">
      <div aria-hidden="true" className="-mx-4 -mt-4 md:mx-0 md:mt-0">
        <div className="h-24 bg-neutral md:h-28 md:rounded-t-box" />
        <div className="checker-strip" />
      </div>

      <div className="-mt-12 flex flex-col gap-3 px-1">
        <div className="w-fit rounded-full ring-4 ring-base-100 md:ml-6">
          <AvatarDeUsuario username={username} size={88} />
        </div>
        <div className="flex min-w-0 flex-col gap-1.5">
          <h1 className="truncate font-display text-2xl font-bold">@{username}</h1>
          {bajada}
        </div>
        {acciones && <div className="grid grid-cols-2 gap-3 md:flex">{acciones}</div>}
      </div>
    </div>
  )
}

/**
 * Los números de un perfil en una tarjeta, separados por líneas finas.
 *
 * Los que llevan a algún lado son enlaces: "12 reseñas" en tu perfil abre tus reseñas.
 */
export function CifrasDePerfil({
  cifras,
}: {
  cifras: { valor: string; etiqueta: string; a?: string }[]
}) {
  return (
    <ul
      className="grid divide-x divide-base-content/10 rounded-box bg-base-100 py-3 ring-1 ring-inset ring-base-content/15"
      style={{ gridTemplateColumns: `repeat(${cifras.length}, minmax(0, 1fr))` }}
    >
      {cifras.map(({ valor, etiqueta, a }) => {
        const contenido = (
          <>
            <span className="font-display text-2xl font-bold tabular-nums">{valor}</span>
            <span className="text-xs text-base-content/70">{etiqueta}</span>
          </>
        )
        const estilo = 'flex flex-col items-center gap-0.5 px-2 text-center'
        return (
          <li key={etiqueta}>
            {a ? (
              <Link to={a} className={`${estilo} hover:text-primary`}>
                {contenido}
              </Link>
            ) : (
              <div className={estilo}>{contenido}</div>
            )}
          </li>
        )
      })}
    </ul>
  )
}
