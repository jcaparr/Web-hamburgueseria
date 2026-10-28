import type { NotaYCuantas } from '../types'
import { cantidad } from '../utils/numeros'

/**
 * Cuántas reseñas tiene cada nota, en cinco barras.
 *
 * Es lo que el promedio no dice: un 4.0 de puros 4 es un local parejo, y un 4.0 de
 * mitad 5 y mitad 3 es un local que divide opiniones. Los dos muestran "4.0".
 *
 * Las barras van del 5 al 1, de arriba hacia abajo, que es el orden en que se leen estas
 * listas en todas partes. Se dibujan siempre las cinco, incluso en cero: una barra vacía
 * al lado de una llena es justamente el dato, y esconderla movería la escala de local en
 * local.
 *
 * El largo es relativo a la nota más votada y no al total. Con 8 reseñas repartidas
 * 5/2/1, medirlas contra el total daría barras de 62%, 25% y 12%, todas cortas y
 * parecidas; contra el máximo se ve de una cuál ganó.
 *
 * Las cinco filas son las capas de una hamburguesa (#356): pan con semillas arriba (5), las
 * del medio y el pan de abajo (1). Es la misma idea del burgómetro de al lado, sin sumar
 * información. La más votada va en rojo y con su cantidad grande; el resto, en mostaza.
 * Se estira a la altura del tablero que tiene al lado, repartiendo las filas parejo.
 */
export function DistribucionDeNotas({ distribucion }: { distribucion: NotaYCuantas[] }) {
  const masVotada = Math.max(...distribucion.map((d) => d.cuantas))

  if (masVotada === 0) return null

  return (
    // Las barras son para mirar; para el lector de pantalla cada fila se dice entera
    // ("5 estrellas: 3 reseñas"), porque "5 ★ 3" suelto no se entiende.
    <ul
      aria-label="Reseñas por nota"
      className="flex h-full min-w-0 flex-col justify-between gap-2.5 py-1.5"
    >
      {[...distribucion].reverse().map(({ nota, cuantas }) => {
        const esLaMasVotada = cuantas === masVotada
        // Con al menos un voto, nunca menos de 6 %: si no, un solo voto entre 27 no se ve.
        const ancho = cuantas === 0 ? 0 : Math.max((cuantas / masVotada) * 100, 6)
        return (
          <li key={nota} className="grid grid-cols-[2rem_1fr_minmax(1.75rem,auto)] items-center gap-2">
            <span className="sr-only">
              {nota === 1 ? '1 estrella' : `${nota} estrellas`}:{' '}
              {cuantas === 1 ? '1 reseña' : `${cuantas} reseñas`}
            </span>
            <span
              aria-hidden="true"
              className="whitespace-nowrap text-[13px] font-bold tabular-nums"
            >
              <span className="mr-0.5 text-(--mostaza-texto)">★</span>
              {nota}
            </span>
            {/* La pista deja ver el largo que la barra no ocupa, que es lo que hace
                comparables las cinco filas de un vistazo. */}
            <div
              aria-hidden="true"
              className={`relative h-[18px] bg-(--pista) shadow-[inset_0_0_0_1px_var(--pista-filo)] ${FORMA_DE_LA_CAPA[nota] ?? 'rounded'}`}
            >
              {ancho > 0 && (
                <div
                  className={`absolute inset-y-0 left-0 overflow-hidden ${FORMA_DE_LA_CAPA[nota] ?? 'rounded'} ${
                    esLaMasVotada
                      ? 'bg-primary'
                      : 'bg-secondary shadow-[inset_0_0_0_2px_var(--mostaza-contorno)]'
                  }`}
                  style={{ width: `${ancho}%` }}
                >
                  {/* En una barra corta las semillas quedarían como astillas pegadas al borde. */}
                  {nota === 5 && ancho >= 35 && <SemillasDelPan />}
                </div>
              )}
            </div>
            <span
              aria-hidden="true"
              className={`text-right tabular-nums ${
                esLaMasVotada
                  ? 'font-logo text-[18px] leading-none text-primary'
                  : 'text-[13px] text-base-content/70'
              }`}
            >
              {cantidad(cuantas)}
            </span>
          </li>
        )
      })}
    </ul>
  )
}

/** Cada nota con la forma de su capa: pan de arriba, el medio (la 4 con un guiño) y pan de abajo. */
const FORMA_DE_LA_CAPA: Record<number, string> = {
  5: 'rounded-[18px_18px_5px_5px]',
  4: 'rounded-[5px_12px_5px_12px]',
  3: 'rounded-[5px]',
  2: 'rounded-[5px]',
  1: 'rounded-[5px_5px_18px_18px]',
}

const SEMILLAS: { izquierda: number; giro: number; arriba: number }[] = [
  { izquierda: 14, giro: -24, arriba: 3 },
  { izquierda: 34, giro: 18, arriba: 4 },
  { izquierda: 54, giro: -14, arriba: 3 },
  { izquierda: 74, giro: 26, arriba: 4 },
]

/**
 * Las semillas de sésamo del pan de arriba. Van en crema fijo, igual que las del logo: el
 * rojo y el mostaza que las llevan debajo son los mismos en los dos temas. Si la barra es
 * corta quedan recortadas por ella, que es lo que pasa con un pan chico.
 */
function SemillasDelPan() {
  return (
    <>
      {SEMILLAS.map(({ izquierda, giro, arriba }) => (
        <i
          key={izquierda}
          className="absolute h-[11px] w-[6px] rounded-full bg-[#f7efd8] opacity-90"
          style={{ left: `${izquierda}%`, top: arriba, transform: `rotate(${giro}deg)` }}
        />
      ))}
    </>
  )
}
