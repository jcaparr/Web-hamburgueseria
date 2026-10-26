import type { ReactNode } from 'react'
import { avanceDe, nivelDe } from '../utils/niveles'
import { FONDOS, PANES, QUESOS, recetaPara, type Receta } from '../utils/recetas'

/**
 * La cara de cada uno: una hamburguesa propia.
 *
 * Antes era la inicial sobre un color. Servía para reconocer a alguien en el feed, pero
 * era la cara de cualquier red: una "B" no dice nada de nadie. Una hamburguesa es lo que
 * la persona vino a hacer acá, y armada de a capas da para que cada uno tenga la suya.
 *
 * Cinco cosas cambian de una a otra: el fondo, el pan, el queso, lo verde y cuántas
 * carnes. Cada uno arma la suya desde su perfil; mientras no lo haga, ve la que sale de
 * su nombre, una de 360, así que dos personas en la misma pantalla casi nunca se ven
 * iguales. El fondo es lo que más se distingue de lejos, y es lo que sigue dejando
 * reconocer de un vistazo que dos publicaciones son de la misma persona.
 */
export function AvatarDeUsuario({
  username,
  hamburguesa,
  xp,
  sinCirculo = false,
  size = 40,
  className,
}: {
  username: string
  /** La que eligió, en cinco cifras; sin ella, la que sale de su nombre. */
  hamburguesa?: string | null
  /**
   * Su XP (#311). Con ella se dibuja el anillo de avance y el circulito del nivel; sin
   * ella, la hamburguesa sola, así cada pantalla puede adoptarlo de a una. Pide `size`:
   * la geometría del anillo sale del tamaño en píxeles.
   */
  xp?: number
  /** Solo el anillo, sin el número: para la pestaña Perfil, donde no hay lugar para él. */
  sinCirculo?: boolean
  size?: number
  /**
   * Clases de tamaño en vez de `size`, para que cambie con la pantalla: la cabecera del
   * perfil la quiere más grande en la compu que en el teléfono. Con esto, `size` no se usa.
   */
  className?: string
}) {
  const receta = recetaPara(username, hamburguesa)
  const disco = (
    <div
      className={`flex-none overflow-hidden rounded-full ${FONDOS[receta.fondo]} ${className ?? ''}`}
      style={className ? undefined : { width: size, height: size }}
      aria-hidden
    >
      <Hamburguesa receta={receta} />
    </div>
  )
  if (xp === undefined || className) return disco
  return (
    <AvatarConNivel size={size} xp={xp} anillo={anilloDe(size)} sinCirculo={sinCirculo}>
      {disco}
    </AvatarConNivel>
  )
}

/** El hueco entre el avatar y el anillo, y lo grueso del anillo, según el tamaño. */
function anilloDe(size: number) {
  if (size <= 48) return { hueco: 1.5, grosor: 2.5 }
  if (size <= 96) return { hueco: 3, grosor: 5 }
  return { hueco: 4, grosor: 6 }
}

/** El diámetro del circulito del nivel y su letra. Con dos cifras, el chico se ensancha. */
function circuloDe(size: number, dosCifras: boolean) {
  if (size <= 48) return { diametro: dosCifras ? 20 : 18, letra: 12 }
  if (size <= 84) return { diametro: 26, letra: 14 }
  if (size <= 96) return { diametro: 28, letra: 15 }
  return { diametro: 32, letra: 17 }
}

/**
 * El avatar con su nivel: un anillo que se llena hacia el siguiente y, colgando abajo
 * al centro, el circulito amarillo con el número.
 *
 * El borde de arriba del circulito queda 3 px por encima del borde de abajo de la
 * hamburguesa, así cuelga sin tapar el pan. En los chicos (el feed y las listas) el
 * circulito sobresale hacia abajo pero el bloque no crece: el renglón mide lo mismo que
 * sin nivel. Los amarillos y marrones son fijos, como los fondos de la hamburguesa: el
 * contraste del número (marrón sobre amarillo, 8,5 : 1) tiene que darse en los dos temas.
 */
function AvatarConNivel({
  size,
  xp,
  anillo,
  sinCirculo,
  children,
}: {
  size: number
  xp: number
  anillo: { hueco: number; grosor: number }
  sinCirculo: boolean
  children: ReactNode
}) {
  const nivel = nivelDe(xp)
  const { hueco, grosor } = anillo
  const margen = hueco + grosor
  const caja = size + 2 * margen
  const centro = caja / 2
  const radio = centro - grosor / 2
  const circunferencia = 2 * Math.PI * radio
  const avance = avanceDe(xp)

  const { diametro, letra } = circuloDe(size, nivel > 9)
  const baja = margen + size
  const chico = size <= 48
  const centroY = chico
    ? Math.min(baja + diametro / 2 - 2, caja + 12 - diametro / 2)
    : baja + diametro / 2 - 3
  const alto = chico || sinCirculo ? caja : centroY + diametro / 2

  return (
    <span className="relative block flex-none" style={{ width: caja, height: alto }}>
      <svg
        width={caja}
        height={caja}
        viewBox={`0 0 ${caja} ${caja}`}
        className="absolute left-0 top-0"
        aria-hidden
        focusable="false"
      >
        <circle
          cx={centro}
          cy={centro}
          r={radio}
          fill="none"
          strokeWidth={grosor}
          style={{ stroke: 'color-mix(in oklab, var(--color-base-content) 22%, transparent)' }}
        />
        {avance > 0 && (
          <circle
            cx={centro}
            cy={centro}
            r={radio}
            fill="none"
            strokeWidth={grosor}
            strokeLinecap="round"
            strokeDasharray={`${avance * circunferencia} ${circunferencia}`}
            transform={`rotate(-90 ${centro} ${centro})`}
            className="stroke-primary"
          />
        )}
      </svg>
      <span className="absolute" style={{ left: margen, top: margen }}>
        {children}
      </span>
      {!sinCirculo && (
      <span
        aria-hidden
        className="absolute flex -translate-x-1/2 items-center justify-center rounded-full border-2 border-base-100 bg-[#f2b705] font-display font-bold leading-none text-[#3a2a1e] tabular-nums"
        style={{ left: '50%', top: centroY - diametro / 2, width: diametro, height: diametro, fontSize: letra }}
      >
        {nivel}
      </span>
      )}
    </span>
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
