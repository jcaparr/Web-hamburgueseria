import type { TemaDeResenias } from '../types'

/**
 * De qué habla la gente que escribió sobre este local.
 *
 * Es lo que el promedio y la distribución no dicen: por qué. Eso está en los
 * comentarios, y leer veinte para enterarse de que casi todos elogian la carne y se
 * quejan de la espera es justamente lo que nadie hace.
 *
 * Sale de nuestras propias reseñas, no de Google. Lo que se cuenta es de qué se habla;
 * si se habló bien o mal lo decide la nota que puso esa misma persona, que es un dato
 * que ya está y que no se equivoca con la ironía.
 *
 * No aparece hasta que hay unas cuantas reseñas escritas, así que hoy casi ningún local
 * lo muestra. Es a propósito: con dos comentarios esto no sería un resumen sino una
 * opinión suelta disfrazada de tendencia, y las reseñas enteras ya están más abajo.
 */
export function TemasDeLasResenias({ temas }: { temas: TemaDeResenias[] }) {
  if (temas.length === 0) return null

  return (
    <section className="flex flex-col gap-2 rounded-box bg-base-100 p-3 ring-1 ring-inset ring-base-content/15">
      <div className="flex flex-col">
        <h3 className="text-xs font-semibold uppercase tracking-wide text-base-content/50">
          De qué hablan las reseñas
        </h3>
        {/* La aclaración va una vez acá y no en cada fila: repetir "hablan bien" cuatro
            veces hace más ruido que el dato. */}
        <p className="text-xs text-base-content/50">
          Cuántos de los que lo nombraron pusieron buena nota.
        </p>
      </div>

      <ul className="flex flex-col gap-1.5">
        {temas.map(({ tema, menciones, aFavor }) => (
          <li key={tema} className="flex items-center justify-between gap-3 text-sm">
            <span className="font-medium">{tema}</span>
            <span className="flex flex-none items-center gap-1.5 text-xs tabular-nums text-base-content/60">
              <span className={`h-1.5 w-1.5 rounded-full ${colorDe(aFavor, menciones)}`} />
              {aFavor} de {menciones}
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}

/**
 * El punto de color, que es lo que se lee antes que el número.
 *
 * Tres estados y no un degradé: lo que importa es si el tema juega a favor del local,
 * en contra, o está dividido. Los cortes son holgados —dos tercios para cada lado— para
 * que el punto no cambie de color por una sola reseña nueva.
 */
function colorDe(aFavor: number, menciones: number) {
  const proporcion = aFavor / menciones
  if (proporcion >= 2 / 3) return 'bg-success'
  if (proporcion <= 1 / 3) return 'bg-error'
  return 'bg-base-content/30'
}
