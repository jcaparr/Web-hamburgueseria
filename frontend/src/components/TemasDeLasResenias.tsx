import type { ReactNode } from 'react'
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
 * Arriba va una oración que lo resume, como la de Google Maps, y abajo los temas en tres
 * grupos: lo que gusta, lo que no y lo que divide (#392). La oración la arma el backend
 * con los mismos números, así que nunca dice algo que los grupos no muestren.
 *
 * No aparece hasta que hay unas cuantas reseñas escritas, así que hoy casi ningún local
 * lo muestra. Es a propósito: con dos comentarios esto no sería un resumen sino una
 * opinión suelta disfrazada de tendencia, y las reseñas enteras ya están más abajo.
 */
export function TemasDeLasResenias({ temas, frase }: { temas: TemaDeResenias[]; frase: string | null }) {
  if (temas.length === 0) return null

  const gusta = temas.filter((t) => t.tono === 'A_FAVOR')
  const noGusta = temas.filter((t) => t.tono === 'EN_CONTRA')
  const divide = temas.filter((t) => t.tono === 'DIVIDIDO')

  return (
    <section className="flex flex-col gap-3 tarjeta p-3">
      <div className="flex flex-col gap-1">
        <h3 className="text-sm font-semibold">De qué hablan las reseñas</h3>
        {frase && <p className="text-sm">{frase}</p>}
        {/* Una vez acá y no en cada chip: dice de dónde sale la frase y qué cuentan los
            números, sin repetirlo seis veces. */}
        <p className="text-xs text-base-content/70">
          Resumen automático según la nota de quienes nombraron cada tema.
        </p>
      </div>

      {gusta.length > 0 && (
        <Grupo titulo="Lo que gusta" signo="+" colorDelSigno="text-success">
          {gusta.map((t) => (
            <Chip key={t.tema} tema={t.tema} cuenta={`${t.aFavor} de ${t.menciones}`}
              ayuda={`${t.aFavor} de ${t.menciones} con buena nota`} estilo="bg-success/15 text-success" />
          ))}
        </Grupo>
      )}

      {noGusta.length > 0 && (
        <Grupo titulo="Lo que no" signo="−" colorDelSigno="text-error">
          {noGusta.map((t) => (
            <Chip key={t.tema} tema={t.tema} cuenta={`${t.menciones - t.aFavor} de ${t.menciones}`}
              ayuda={`${t.menciones - t.aFavor} de ${t.menciones} con nota baja`} estilo="bg-error/15 text-error" />
          ))}
        </Grupo>
      )}

      {divide.length > 0 && (
        <Grupo titulo="Dividido">
          {divide.map((t) => (
            <Chip key={t.tema} tema={t.tema} cuenta={`${t.aFavor} de ${t.menciones} a favor`}
              ayuda={`${t.aFavor} de ${t.menciones} con buena nota`}
              estilo="bg-base-content/10 text-base-content/80" />
          ))}
        </Grupo>
      )}
    </section>
  )
}

function Grupo({ titulo, signo, colorDelSigno, children }: {
  titulo: string
  signo?: string
  colorDelSigno?: string
  children: ReactNode
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <h4 className="flex items-center gap-1.5 text-xs font-semibold">
        {signo && <span aria-hidden="true" className={`font-bold ${colorDelSigno}`}>{signo}</span>}
        {titulo}
      </h4>
      <ul className="flex flex-wrap gap-1.5">{children}</ul>
    </div>
  )
}

/**
 * Un tema con su cuenta. No se toca, así que no necesita los 44 px de un botón. El
 * número va más chico y sin negrita, no más transparente: con transparencia el verde
 * sobre su fondo bajaba a 3,7 : 1, y así queda en 4,9 : 1 (claro) y 6,3 : 1 (oscuro).
 */
function Chip({ tema, cuenta, ayuda, estilo }: { tema: string; cuenta: string; ayuda: string; estilo: string }) {
  return (
    <li className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-sm font-medium ${estilo}`}>
      {tema}
      <span className="text-xs font-normal tabular-nums" aria-hidden="true">{cuenta}</span>
      <span className="sr-only">{ayuda}</span>
    </li>
  )
}
