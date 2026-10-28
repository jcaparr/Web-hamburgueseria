import type { Rating, ResumenDeResenias } from '../types'
import { notasConDatos } from '../utils/notasOpcionales'
import { DistribucionDeNotas } from './DistribucionDeNotas'
import { MedidorGrande } from './MedidorGrande'
import { Personaje } from './Personaje'
import { RelojesDeNotas } from './RelojesDeNotas'
import { Stars } from './Stars'
import { TemasDeLasResenias } from './TemasDeLasResenias'

/**
 * Qué tan buena es, de un vistazo: el promedio grande, cuántas lo dicen y cómo se
 * reparten las notas.
 *
 * El promedio va grande al lado de las barras porque son la misma respuesta a dos
 * escalas: el número dice cuánto, las barras dicen si todos coinciden. Antes las
 * barras estaban solas y el promedio, chico, arriba al lado del nombre. Va en el
 * burgómetro grande, el tablero del logo, que es lo que le da sentido al nombre.
 *
 * Debajo, las notas de local, atención y precio (#356), solo las que alguien puso: explican
 * el promedio sin cambiarlo, que sigue siendo la única nota que cuenta para el ranking.
 *
 * Sin calificaciones no hay nada que resumir, y en vez de un bloque vacío se invita a
 * ser quien empieza: es el momento en que una reseña más vale más. Sin botón propio:
 * "Escribir reseña" está justo arriba, y dos botones rojos para lo mismo competirían.
 */
export function ResumenDeCalificaciones({
  promedio,
  cuantas,
  resumen,
  miResenia,
}: {
  promedio: number | null
  cuantas: number
  resumen: ResumenDeResenias | null
  miResenia: Rating | null
}) {
  if (!promedio || cuantas === 0) {
    return (
      <section className="tarjeta flex items-center gap-4 p-5">
        <Personaje pose="lupa" className="h-24 w-auto shrink-0" />
        <div className="flex flex-col gap-2">
          <h2 className="font-display text-lg font-bold">Todavía nadie la calificó</h2>
          <p className="text-sm text-base-content/70">
            Si fuiste, contá qué tal estuvo. La primera reseña es la que más ayuda a los
            que todavía no saben si ir.
          </p>
        </div>
      </section>
    )
  }

  return (
    <section className="flex flex-col gap-4" aria-labelledby="calificaciones">
      <h2 id="calificaciones" className="font-logo text-lg">
        Calificaciones
      </h2>

      {/* Con tope de ancho y centrado: en escritorio la ficha es ancha y, estiradas, las
          barras se volvían tiras de 400 px y los relojes quedaban perdidos en su recuadro. */}
      <div className="tarjeta p-4 min-[360px]:p-5 md:p-6">
        <div className="mx-auto flex w-full max-w-lg flex-col gap-5 md:gap-6">
          {/* El contador se estira a la altura del tablero (items-stretch) y reparte sus filas
              parejo: así el borde de arriba y el de abajo de las dos mitades coinciden. */}
          <div className="flex items-stretch gap-4 md:gap-8">
            <MedidorGrande valor={promedio} conLetraDelLogo>
              <span className="mt-2 text-xs text-base-content/70">
                {cuantas === 1 ? '1 reseña' : `${cuantas} reseñas`}
              </span>
            </MedidorGrande>
            <div className="min-w-0 flex-1">
              {resumen && <DistribucionDeNotas distribucion={resumen.distribucion} />}
            </div>
          </div>
          <RelojesDeNotas notas={notasConDatos(resumen?.desglose ?? null)} />
        </div>
      </div>

      {resumen && <TemasDeLasResenias temas={resumen.temas} />}

      {/* Lo que pusiste vos, al lado de lo que pusieron todos: sin esto, un local que ya
          calificaste se ve igual que uno donde nunca fuiste. */}
      {miResenia && (
        <p className="flex items-center gap-2 text-sm">
          <span className="font-semibold">Tu puntaje</span>
          <Stars value={miResenia.score} size={16} />
        </p>
      )}
    </section>
  )
}
