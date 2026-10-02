import type { Rating, ResumenDeResenias } from '../types'
import { DistribucionDeNotas } from './DistribucionDeNotas'
import { Stars } from './Stars'
import { TemasDeLasResenias } from './TemasDeLasResenias'

/**
 * Qué tan buena es, de un vistazo: el promedio grande, cuántas lo dicen y cómo se
 * reparten las notas.
 *
 * El promedio va grande al lado de las barras porque son la misma respuesta a dos
 * escalas: el número dice cuánto, las barras dicen si todos coinciden. Antes las
 * barras estaban solas y el promedio, chico, arriba al lado del nombre.
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
      <section className="flex flex-col gap-2 rounded-box bg-base-200 p-5">
        <h2 className="font-display text-lg font-bold">Todavía nadie la calificó</h2>
        <p className="text-sm text-base-content/70">
          Si fuiste, contá qué tal estuvo. La primera reseña es la que más ayuda a los
          que todavía no saben si ir.
        </p>
      </section>
    )
  }

  return (
    <section className="flex flex-col gap-4" aria-labelledby="calificaciones">
      <h2 id="calificaciones" className="font-display text-lg font-bold">
        Calificaciones
      </h2>

      <div className="flex items-center gap-5 rounded-box bg-base-100 p-4 ring-1 ring-inset ring-base-content/15">
        <div className="flex flex-none flex-col items-center gap-0.5 pr-1">
          <span className="font-display text-5xl font-extrabold leading-none tabular-nums">
            {promedio.toFixed(1)}
          </span>
          <span className="text-xs text-base-content/70">
            {cuantas === 1 ? '1 reseña' : `${cuantas} reseñas`}
          </span>
        </div>
        <div className="min-w-0 flex-1">
          {resumen && <DistribucionDeNotas distribucion={resumen.distribucion} />}
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
