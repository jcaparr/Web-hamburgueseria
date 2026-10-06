import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { AvatarDeUsuario } from './AvatarDeUsuario'

/**
 * La parte de arriba de un perfil: el avatar, el nombre, los números y lo que se puede
 * hacer.
 *
 * Compacta, como en cualquier red: el avatar a la izquierda y el nombre con los números
 * al lado. La versión anterior apilaba una banda marrón, el avatar grande, el nombre,
 * una insignia, dos botones y una tarjeta con los números: en el teléfono eran ocho
 * bloques antes de llegar a la primera reseña.
 *
 * El avatar es el de las reseñas, con la inicial y el color de cada uno, para que la
 * persona se reconozca igual acá que en el feed.
 *
 * @param bajada   la línea de debajo del nombre, como a cuántos sigue
 * @param cifras   los números; los que llevan a algún lado son enlaces
 * @param acciones lo que se puede hacer, debajo de todo
 */
export function CabeceraDePerfil({
  username,
  bajada,
  cifras,
  acciones,
}: {
  username: string
  bajada?: ReactNode
  cifras: { valor: string; etiqueta: string; a?: string }[]
  acciones?: ReactNode
}) {
  return (
    <header className="flex flex-col gap-4">
      <div className="flex items-center gap-4">
        <AvatarDeUsuario username={username} size={72} />
        <div className="flex min-w-0 flex-1 flex-col gap-2">
          <div className="flex min-w-0 flex-col gap-0.5">
            <h1 className="truncate font-display text-2xl font-bold leading-tight">@{username}</h1>
            {bajada}
          </div>
          <CifrasDePerfil cifras={cifras} />
        </div>
      </div>
      {acciones}
    </header>
  )
}

/**
 * Los números en una fila, cada uno con su nombre abajo y sin tarjeta alrededor: son
 * parte de la cabecera, no un bloque aparte.
 */
function CifrasDePerfil({ cifras }: { cifras: { valor: string; etiqueta: string; a?: string }[] }) {
  return (
    <ul className="flex gap-5">
      {cifras.map(({ valor, etiqueta, a }) => {
        const contenido = (
          <>
            <span className="font-display text-xl font-bold leading-none tabular-nums">{valor}</span>
            <span className="text-xs text-base-content/70">{etiqueta}</span>
          </>
        )
        const estilo = 'flex flex-col gap-1'
        return (
          <li key={etiqueta}>
            {a ? (
              // El relleno agranda lo que se toca hasta pasar los 44 px que pide el dedo,
              // y el margen negativo lo compensa para que la fila no crezca.
              <Link to={a} className={`${estilo} -my-1.5 justify-center rounded-field py-1.5 hover:text-primary`}>
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
