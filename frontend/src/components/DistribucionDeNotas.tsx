import type { NotaYCuantas } from '../types'

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
 */
export function DistribucionDeNotas({ distribucion }: { distribucion: NotaYCuantas[] }) {
  const masVotada = Math.max(...distribucion.map((d) => d.cuantas))

  if (masVotada === 0) return null

  return (
    <div className="flex flex-col gap-1.5">
      {[...distribucion].reverse().map(({ nota, cuantas }) => (
        <div key={nota} className="flex items-center gap-2">
          <span className="w-8 flex-none text-right text-xs tabular-nums text-base-content/60">
            {nota} ★
          </span>
          {/* El riel gris de fondo deja ver el largo que la barra no ocupa, que es lo
              que hace comparables las cinco filas de un vistazo. */}
          <div className="h-2 flex-1 overflow-hidden rounded-full bg-base-content/10">
            <div
              className="h-full rounded-full bg-primary"
              style={{ width: `${(cuantas / masVotada) * 100}%` }}
            />
          </div>
          <span className="w-6 flex-none text-xs tabular-nums text-base-content/60">
            {cuantas}
          </span>
        </div>
      ))}
    </div>
  )
}
