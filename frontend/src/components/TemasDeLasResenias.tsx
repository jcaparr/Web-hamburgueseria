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
 * Los temas van en dos grupos, lo que gusta y lo que no (#392). Se probó con una oración
 * resumen arriba y con un grupo "Dividido", y Juan los sacó: con los chips alcanza.
 *
 * No aparece hasta que hay unas cuantas reseñas escritas, así que hoy casi ningún local
 * lo muestra. Es a propósito: con dos comentarios esto no sería un resumen sino una
 * opinión suelta disfrazada de tendencia, y las reseñas enteras ya están más abajo.
 */
export function TemasDeLasResenias({ temas }: { temas: TemaDeResenias[] }) {
  // Lo dividido no se muestra (pedido de Juan en #392): un tema que no juega para
  // ningún lado no ayuda a decidir.
  const gusta = temas.filter((t) => t.tono === 'A_FAVOR')
  const noGusta = temas.filter((t) => t.tono === 'EN_CONTRA')
  if (gusta.length === 0 && noGusta.length === 0) return null

  return (
    <section className="flex flex-col gap-3 tarjeta p-3">
      <div className="flex flex-col gap-1">
        <h3 className="text-sm font-semibold">De qué hablan las reseñas</h3>
        {/* Una vez acá y no en cada chip: dice cómo se reparten los temas, sin repetirlo
            seis veces. */}
        <p className="text-xs text-base-content/70">
          Según la nota de quienes nombraron cada tema.
        </p>
      </div>

      {gusta.length > 0 && (
        <Grupo titulo="Lo que gusta" signo="+" colorDelSigno="text-success">
          {gusta.map((t) => <Chip key={t.tema} tema={t.tema} estilo="bg-success/15 text-success" />)}
        </Grupo>
      )}

      {noGusta.length > 0 && (
        <Grupo titulo="Lo que no" signo="−" colorDelSigno="text-error">
          {noGusta.map((t) => <Chip key={t.tema} tema={t.tema} estilo="bg-error/15 text-error" />)}
        </Grupo>
      )}
    </section>
  )
}

function Grupo({ titulo, signo, colorDelSigno, children }: {
  titulo: string
  signo: string
  colorDelSigno: string
  children: ReactNode
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <h4 className="flex items-center gap-1.5 text-xs font-semibold">
        <span aria-hidden="true" className={`font-bold ${colorDelSigno}`}>{signo}</span>
        {titulo}
      </h4>
      <ul className="flex flex-wrap gap-1.5">{children}</ul>
    </div>
  )
}

/**
 * Un tema, sin la cuenta: "2 de 2" con números tan chicos se leía como una nota y no
 * sumaba (#392). No se toca, así que no necesita los 44 px de un botón. El verde sobre
 * su fondo da 4,9 : 1 en claro y 6,3 : 1 en oscuro.
 */
function Chip({ tema, estilo }: { tema: string; estilo: string }) {
  return (
    <li className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium ${estilo}`}>
      {tema}
    </li>
  )
}
