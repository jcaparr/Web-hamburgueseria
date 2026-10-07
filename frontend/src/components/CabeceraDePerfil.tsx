import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { AvatarDeUsuario } from './AvatarDeUsuario'

/**
 * La parte de arriba de un perfil: la hamburguesa de la persona, su nombre, sus números
 * y lo que se puede hacer.
 *
 * Tres cosas con tres pesos distintos, porque se leen distinto. El nombre es lo que dice
 * de quién es la página: va angosto y grueso, como los títulos. Los números son datos que
 * se comparan entre sí: van en su propia franja, cada uno en su casilla, con la letra de
 * ancho normal. Antes iban los tres en la misma fila que el nombre y con la misma letra,
 * y se leían como una sola cosa amontonada.
 *
 * Todo dentro de una tarjeta, para que la cabecera sea un bloque y las pestañas de abajo
 * otro. Sigue siendo compacta: en el teléfono son dos filas, no los ocho bloques que
 * tenía la versión de antes de #144.
 *
 * @param bajada   la línea de debajo del nombre, como a cuántos sigue
 * @param cifras   los números; los que llevan a algún lado son enlaces
 * @param acciones lo que se puede hacer, debajo de la tarjeta
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
    <header className="flex flex-col gap-3">
      <div className="flex flex-col gap-4 tarjeta p-4 md:flex-row md:items-center md:gap-6 md:p-5">
        <div className="flex min-w-0 flex-1 items-center gap-4">
          <AvatarDeUsuario username={username} size={88} />
          <div className="flex min-w-0 flex-col gap-1">
            {/* El arroba más apagado: es de todos los nombres y no dice nada de este. */}
            <h1 className="truncate titulo-pagina">
              <span className="text-base-content/60">@</span>
              {username}
            </h1>
            {bajada}
          </div>
        </div>
        <CifrasDePerfil cifras={cifras} />
      </div>
      {acciones}
    </header>
  )
}

/**
 * Los números en una franja hundida, un poco más oscura que la tarjeta, partida en
 * casillas iguales. Hundida y no en tarjetas propias: son parte de la cabecera, y tres
 * tarjetas chicas adentro de otra se ven como una grilla de botones.
 */
function CifrasDePerfil({ cifras }: { cifras: { valor: string; etiqueta: string; a?: string }[] }) {
  return (
    <ul className="grid auto-cols-fr grid-flow-col divide-x divide-base-content/10 rounded-field bg-base-200 md:w-80 md:flex-none">
      {cifras.map(({ valor, etiqueta, a }) => {
        const contenido = (
          <>
            <span className="text-lg font-bold leading-none tabular-nums">{valor}</span>
            <span className="text-xs text-base-content/70">{etiqueta}</span>
          </>
        )
        // La casilla entera es lo que se toca: con el relleno pasa los 44 px que pide el dedo.
        const estilo = 'flex flex-1 flex-col items-center justify-center gap-1 px-1 py-3'
        return (
          <li key={etiqueta} className="flex">
            {a ? (
              <Link to={a} className={`${estilo} rounded-field hover:text-primary`}>
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
