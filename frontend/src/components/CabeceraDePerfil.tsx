import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { AvatarDeUsuario } from './AvatarDeUsuario'

/**
 * La parte de arriba de un perfil: la hamburguesa de la persona, su nombre, sus números
 * y lo que se puede hacer.
 *
 * Tres cosas con tres pesos distintos, porque se leen distinto. El nombre es lo que dice
 * de quién es la página: va angosto y grueso, como los títulos. Los números van armados
 * como una hamburguesa, una cifra por capa ({@link CifrasDePerfil}). Antes iban en la
 * misma fila que el nombre y con la misma letra, y se leían como una sola cosa amontonada.
 *
 * Todo dentro de una tarjeta, para que la cabecera sea un bloque y las pestañas de abajo
 * otro.
 *
 * @param bajada   la línea de debajo del nombre, como el enlace para cambiar la hamburguesa
 * @param cifras   los números, de arriba abajo; los que llevan a algún lado son enlaces
 * @param acciones lo que se puede hacer, debajo de la tarjeta
 */
export function CabeceraDePerfil({
  username,
  hamburguesa,
  bajada,
  cifras,
  acciones,
}: {
  username: string
  hamburguesa: string | null
  bajada?: ReactNode
  cifras: { valor: string; etiqueta: string; a?: string }[]
  acciones?: ReactNode
}) {
  return (
    <header className="flex flex-col gap-3">
      <div className="flex flex-col gap-4 tarjeta p-4 md:flex-row md:items-center md:gap-6 md:p-5">
        <div className="flex min-w-0 flex-1 items-center gap-4">
          <AvatarDeUsuario username={username} hamburguesa={hamburguesa} size={88} />
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
 * Las capas, de arriba abajo: pan, cheddar, carne y pan.
 *
 * Son los colores de la hamburguesa del avatar (utils/recetas.ts y AvatarDeUsuario), así
 * que las cifras hacen juego con ella. Cada capa trae su propio color de letra y no el del
 * tema: los fondos son los mismos en claro y en oscuro, y el contraste tiene que darse
 * contra la capa. Medidos con la fórmula de WCAG, los cuatro pasan con aire los 4,5 : 1
 * que pide un texto chico: pan con letra oscura 6,3; cheddar con letra oscura 10,3; carne
 * con letra crema 6,8.
 *
 * La etiqueta va del mismo color que el número, sin transparencia. Apagarla con opacidad,
 * como en el resto de la app, es lo que la dejaba por debajo del mínimo sobre el pan.
 */
const CAPAS = [
  'bg-[#cf8636] text-[#17110d] rounded-t-[1.75rem] rounded-b-md',
  'bg-[#f2b705] text-[#17110d] rounded-md',
  'bg-[#7a4526] text-[#f7efd8] rounded-md',
  'bg-[#cf8636] text-[#17110d] rounded-t-md rounded-b-[1.25rem]',
]

/**
 * Los números armados como una hamburguesa: una capa por cifra, con el nombre a la
 * izquierda y el número a la derecha (#232).
 *
 * Reemplaza a la franja de casillas iguales, que con cuatro cifras se sentía cargada: las
 * capas se leen de arriba abajo como una lista, de a una, y el número queda alineado a la
 * derecha para compararlos de un vistazo.
 *
 * Cada capa mide 44 px de alto, lo que pide el dedo, porque las que llevan a una lista se
 * tocan enteras. Con el foco del teclado, el contorno queda por fuera, sobre la tarjeta.
 */
function CifrasDePerfil({ cifras }: { cifras: { valor: string; etiqueta: string; a?: string }[] }) {
  return (
    <ul className="flex flex-col gap-[3px] md:w-72 md:flex-none">
      {cifras.map(({ valor, etiqueta, a }, i) => {
        const capa = CAPAS[i] ?? CAPAS[1]
        const contenido = (
          <>
            <span className="text-sm font-semibold first-letter:uppercase">{etiqueta}</span>
            <span className="text-base font-bold tabular-nums">{valor}</span>
          </>
        )
        const estilo = `flex min-h-11 items-center justify-between gap-3 px-4 ${capa}`
        return (
          <li key={etiqueta}>
            {a ? (
              <Link to={a} className={`${estilo} transition-[filter] hover:brightness-95 focus-visible:outline-offset-2`}>
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
