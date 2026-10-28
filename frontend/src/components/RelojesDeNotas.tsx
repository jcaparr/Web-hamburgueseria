import { nota } from '../utils/numeros'
import { veredicto, type NotaConDatos } from '../utils/notasOpcionales'

/**
 * Los tres relojitos de la ficha: local, atención y precio (#356).
 *
 * Son las notas opcionales de la reseña (#336). Van debajo del tablero grande porque
 * explican el promedio —"rica pero cara"— sin competir con él, y no cuentan para el
 * ranking: la nota general sigue siendo la única que cuenta.
 *
 * Solo los que tienen datos. Con uno o dos queda ese uno o dos, del mismo ancho que
 * cuando son tres, centrados: un reloj estirado a todo el ancho se leería como el
 * principal. Sin ninguno, nada: un recuadro con guiones sería ruido.
 */
export function RelojesDeNotas({ notas }: { notas: NotaConDatos[] }) {
  if (notas.length === 0) return null

  return (
    <>
      <hr className="border-0 border-t border-dashed border-base-300" />
      <ul aria-label="Notas por aspecto" className="flex justify-center gap-2">
        {notas.map(({ rotulo, promedio, votos }) => (
          <li
            key={rotulo}
            className="flex min-w-0 max-w-[calc((100%-1rem)/3)] flex-1 flex-col items-center gap-0.5 rounded-field bg-base-200 px-1 py-2.5 text-center"
          >
            {/* El reloj es para mirar; el lector de pantalla recibe todo junto. */}
            <span className="sr-only">
              {rotulo}: {nota(promedio)} de 5, {veredicto(promedio)}, {votos === 1 ? '1 voto' : `${votos} votos`}
            </span>
            <MiniTablero valor={promedio} />
            <span
              aria-hidden="true"
              className="font-logo mt-0.5 text-[22px] leading-none tabular-nums"
            >
              {nota(promedio)}
            </span>
            <span aria-hidden="true" className="text-[13px] text-base-content/70">
              {rotulo}
            </span>
            <span
              aria-hidden="true"
              className="mt-1 rounded-full bg-base-content px-2 py-0.5 text-[11px] font-bold tracking-wide text-base-100"
            >
              {veredicto(promedio)}
            </span>
            <span aria-hidden="true" className="text-xs text-base-content/70">
              {votos === 1 ? '1 voto' : `${votos} votos`}
            </span>
          </li>
        ))}
      </ul>
    </>
  )
}

/**
 * El tablero del burgómetro en miniatura, en mostaza: sin las semillas de la escala, que
 * a este tamaño serían puntos sueltos. El contorno marrón va por debajo y se saca en
 * oscuro (ver --mostaza-contorno). La aguja queda quieta: son tres relojes chicos y el
 * grande ya hace la entrada.
 */
function MiniTablero({ valor }: { valor: number }) {
  const lleno = Math.min(Math.max(valor / 5, 0), 1) * 100
  const arco = 'M22 100 A78 78 0 0 1 178 100'
  return (
    <svg viewBox="0 0 200 110" aria-hidden="true" className="w-full max-w-[92px]">
      <path d={arco} fill="none" strokeWidth={18} strokeLinecap="round" className="stroke-(--pista-filo)" />
      <path d={arco} fill="none" strokeWidth={16} strokeLinecap="round" className="stroke-(--pista)" />
      <path
        d={arco}
        pathLength={100}
        fill="none"
        strokeWidth={21}
        strokeLinecap="round"
        className="stroke-(--mostaza-contorno)"
        style={{ strokeDasharray: `${lleno} 100` }}
      />
      <path
        d={arco}
        pathLength={100}
        fill="none"
        strokeWidth={16}
        strokeLinecap="round"
        className="stroke-secondary"
        style={{ strokeDasharray: `${lleno} 100` }}
      />
      <g style={{ transform: `rotate(${-90 + lleno * 1.8}deg)`, transformOrigin: '100px 100px' }}>
        <polygon points="100,42 105,100 95,100" className="fill-base-content" />
      </g>
      <circle cx="100" cy="100" r="9" className="fill-base-content" />
      <circle cx="100" cy="100" r="3.5" className="fill-base-200" />
    </svg>
  )
}
