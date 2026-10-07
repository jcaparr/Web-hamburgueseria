import type { ReactNode } from 'react'
import { FONDOS, PANES, QUESOS, recetaDe, type Receta } from '../utils/recetas'

/**
 * La cara de cada uno: una hamburguesa propia.
 *
 * Antes era la inicial sobre un color. Servía para reconocer a alguien en el feed, pero
 * era la cara de cualquier red: una "B" no dice nada de nadie. Una hamburguesa es lo que
 * la persona vino a hacer acá, y armada de a capas da para que cada uno tenga la suya.
 *
 * Cinco cosas cambian de una a otra: el fondo, el pan, el queso, lo verde y cuántas
 * carnes. Salen 360 combinaciones, así que dos personas en la misma pantalla casi nunca
 * se ven iguales. El fondo es lo que más se distingue de lejos, y es lo que sigue dejando
 * reconocer de un vistazo que dos publicaciones son de la misma persona.
 */
export function AvatarDeUsuario({
  username,
  size = 40,
}: {
  username: string
  size?: number
}) {
  const receta = recetaDe(username)
  return (
    <div
      className={`flex-none overflow-hidden rounded-full ${FONDOS[receta.fondo]}`}
      style={{ width: size, height: size }}
      aria-hidden
    >
      <Hamburguesa receta={receta} />
    </div>
  )
}

/** El ancho de la carne y del queso, que son lo más ancho; el resto se acomoda a esto. */
const IZQ = 16
const DER = 84
const ANCHO = DER - IZQ
/** Lo que queda entre capa y capa. Sin esto, a 36 px las capas se funden en una mancha. */
const AIRE = 1.5

/**
 * El dibujo, de arriba hacia abajo: pan, lo verde, el queso, las carnes y el pan de
 * abajo. Mismo trazo que el logo, para que el avatar y la marca se vean de la misma mano.
 *
 * Se arma en dos pasos. Primero se mide cuánto ocupa cada capa y dónde cae; después se
 * pinta de abajo hacia arriba, porque el queso se derrama sobre la carne y la lechuga
 * sobre el tomate, y en SVG lo último que se pinta queda encima.
 */
function Hamburguesa({ receta }: { receta: Receta }) {
  const pan = PANES[receta.pan]
  const queso = QUESOS[receta.queso]
  const ALTO_DEL_DOMO = 22

  // solapa: cuánto se mete la capa de abajo debajo de esta.
  const capas: { alto: number; solapa?: number; pintar: (y: number) => ReactNode }[] = []
  if (receta.verdura === 'lechuga' || receta.verdura === 'completa') {
    capas.push({
      alto: 6,
      pintar: (y) => (
        <path
          key="lechuga"
          d={`M${IZQ} ${y} H${DER} V${y + 3} ${'q-4.25 3.5 -8.5 0 '.repeat(8)}Z`}
          fill="#8fb56a"
        />
      ),
    })
  }
  if (receta.verdura === 'tomate' || receta.verdura === 'completa') {
    capas.push({
      alto: 6,
      pintar: (y) => <rect key="tomate" x={IZQ + 4} y={y} width={ANCHO - 8} height={6} rx={3} fill="#d9483b" />,
    })
  }
  if (queso) {
    // Sin aire debajo: la feta se apoya sobre la carne y la tapa un poco.
    capas.push({
      alto: 5,
      solapa: 2 + AIRE,
      pintar: (y) => (
        <path
          key="queso"
          d={`M${IZQ} ${y} H${DER} L${DER - 3} ${y + 5} H66 C66 ${y + 10} 59 ${y + 10} 59 ${y + 5} H34 L31 ${y + 9} L28 ${y + 5} H${IZQ + 3} Z`}
          fill={queso}
        />
      ),
    })
  }
  for (let i = 0; i < receta.carnes; i++) {
    capas.push({
      alto: 10,
      pintar: (y) => <rect key={`carne-${i}`} x={IZQ} y={y} width={ANCHO} height={10} rx={5} fill="#7a4526" />,
    })
  }
  capas.push({
    alto: 10,
    pintar: (y) => <rect key="pan-de-abajo" x={IZQ + 2} y={y} width={ANCHO - 4} height={10} rx={5} fill={pan.relleno} />,
  })

  let y = ALTO_DEL_DOMO + AIRE
  const ubicadas = capas.map((capa) => {
    const arriba = y
    y += capa.alto + AIRE - (capa.solapa ?? 0)
    return { ...capa, arriba }
  })
  const alto = y - AIRE

  // Se agranda o se achica hasta que las cuatro esquinas toquen casi el borde del
  // círculo (radio 50; 46 deja el contorno adentro). Así una simple llena el avatar
  // igual que una de tres carnes, que es más alta que ancha.
  const escala = 46 / Math.hypot(ANCHO / 2, alto / 2)

  return (
    <svg viewBox="0 0 100 100" width="100%" height="100%" focusable="false" className="block">
      <g
        transform={`translate(50 50) scale(${escala}) translate(-50 ${-alto / 2})`}
        stroke="currentColor"
        strokeWidth={2.5}
        strokeLinejoin="round"
      >
        {ubicadas.reverse().map((capa) => capa.pintar(capa.arriba))}
        <path
          d={`M${IZQ} ${ALTO_DEL_DOMO} A${ANCHO / 2} ${ALTO_DEL_DOMO} 0 0 1 ${DER} ${ALTO_DEL_DOMO} Z`}
          fill={pan.relleno}
        />
        {pan.semillas && (
          <g fill="#f7efd8" strokeWidth={1.2}>
            <ellipse cx={36} cy={10} rx={2} ry={3.6} transform="rotate(-40 36 10)" />
            <ellipse cx={50} cy={6} rx={2} ry={3.6} />
            <ellipse cx={64} cy={10} rx={2} ry={3.6} transform="rotate(40 64 10)" />
            <ellipse cx={43} cy={16} rx={2} ry={3.6} transform="rotate(-15 43 16)" />
            <ellipse cx={57} cy={16} rx={2} ry={3.6} transform="rotate(15 57 16)" />
          </g>
        )}
      </g>
    </svg>
  )
}
