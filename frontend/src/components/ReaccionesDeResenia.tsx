import { useRef, useState, type KeyboardEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { DibujoDeReaccion } from './DibujoDeReaccion'
import { IconReaccionar } from './icons'
import type { Reacciones, TipoDeReaccion } from '../types'
import { isSessionExpired } from '../utils/errors'
import { conMiReaccion, reaccion, REACCIONES } from '../utils/reacciones'

/**
 * Las reacciones de una reseña, y las de los demás contadas (#186).
 *
 * Como en los mensajes: se ven solo las que alguien usó, cada una con su número, y
 * tocar una es ponerte esa. Para otra, el botón de la carita abre las cinco. Hay una
 * por persona: elegir otra cambia la tuya, y tocar la tuya la saca. Cada reacción es
 * Burgómetro haciéndola (#365).
 *
 * El número cambia en el momento, antes de que conteste el servidor, y después se
 * acomoda a lo que él diga: si alguien más reaccionó mientras tanto, se cuenta.
 *
 * @param puedeReaccionar falso en la propia y sin sesión: ahí se ven, pero no se tocan.
 */
export function ReaccionesDeResenia({
  ratingId,
  reacciones,
  puedeReaccionar,
}: {
  ratingId: number
  reacciones: Reacciones
  puedeReaccionar: boolean
}) {
  const navigate = useNavigate()
  const [actuales, setActuales] = useState(reacciones)
  // Si quien la muestra vuelve a pedir las reseñas, manda lo nuevo: se toma eso y se
  // deja lo que había acá.
  const [recibidas, setRecibidas] = useState(reacciones)
  if (reacciones !== recibidas) {
    setRecibidas(reacciones)
    setActuales(reacciones)
  }
  const [eligiendo, setEligiendo] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [fallo, setFallo] = useState(false)
  const abrir = useRef<HTMLButtonElement>(null)

  if (!puedeReaccionar && actuales.cuantas.length === 0) return null

  async function elegir(tipo: TipoDeReaccion) {
    setEligiendo(false)
    setFallo(false)
    const antes = actuales
    const sacar = antes.mia === tipo
    setActuales(conMiReaccion(antes, sacar ? null : tipo))
    setGuardando(true)
    try {
      const { data } = sacar
        ? await apiClient.delete<Reacciones>(`/resenias/${ratingId}/reaccion`)
        : await apiClient.put<Reacciones>(`/resenias/${ratingId}/reaccion`, { tipo })
      setActuales(data)
    } catch (err) {
      setActuales(antes)
      if (isSessionExpired(err)) navigate('/login')
      else setFallo(true)
    } finally {
      setGuardando(false)
    }
  }

  function conTeclado(e: KeyboardEvent<HTMLDivElement>) {
    if (e.key !== 'Escape') return
    setEligiendo(false)
    abrir.current?.focus()
  }

  // Como en la maqueta: 64 px de alto para que entre el personaje entero con sus manos, y
  // el borde de 2 px siempre puesto (transparente si no es la tuya) para que elegir una no
  // mueva a las demás.
  const chip = 'inline-flex min-h-16 items-center gap-1.5 rounded-full border-2 pl-1 pr-3.5 text-sm font-semibold'

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-2">
        {actuales.cuantas.map(({ tipo, cuantas }) => {
          const { nombre } = reaccion(tipo)
          const esLaMia = actuales.mia === tipo
          const contenido = (
            <>
              <DibujoDeReaccion tipo={tipo} className="h-[52px] w-14" />
              <span className="contador-de-reaccion">{cuantas}</span>
            </>
          )
          return puedeReaccionar ? (
            <button
              key={tipo}
              type="button"
              onClick={() => elegir(tipo)}
              disabled={guardando}
              aria-pressed={esLaMia}
              aria-label={`${nombre}: ${cuantas}`}
              title={nombre}
              className={`${chip} cursor-pointer transition-colors ${
                esLaMia ? 'border-primary bg-base-200' : 'border-transparent bg-base-200 hover:bg-base-300'
              }`}
            >
              {contenido}
            </button>
          ) : (
            <span key={tipo} role="img" aria-label={`${nombre}: ${cuantas}`} title={nombre} className={`${chip} border-transparent bg-base-200`}>
              {contenido}
            </span>
          )
        })}

        {puedeReaccionar && (
          <button
            ref={abrir}
            type="button"
            onClick={() => setEligiendo((abierto) => !abierto)}
            disabled={guardando}
            aria-expanded={eligiendo}
            aria-label={actuales.mia ? 'Cambiar tu reacción' : 'Reaccionar'}
            className="inline-flex h-11 cursor-pointer items-center gap-1.5 rounded-full px-3 text-sm font-semibold text-base-content/70 hover:bg-base-200 hover:text-base-content"
          >
            <IconReaccionar size={19} />
            {/* Con palabra mientras no haya ninguna: una carita sola, al pie de una
                tarjeta vacía, no dice qué hace. */}
            {actuales.cuantas.length === 0 && <span className="font-semibold">Reaccionar</span>}
          </button>
        )}
      </div>

      {eligiendo && (
        <div
          role="group"
          aria-label="Elegí una reacción"
          onKeyDown={conTeclado}
          className="flex w-fit gap-1.5 rounded-full bg-base-200 p-1.5 shadow-sm"
        >
          {REACCIONES.map(({ tipo, nombre }, i) => (
            <button
              key={tipo}
              type="button"
              onClick={() => elegir(tipo)}
              aria-label={nombre}
              aria-pressed={actuales.mia === tipo}
              title={nombre}
              // Al abrirse, el foco va a la tuya o a la primera: se elige con Tab y Enter
              // sin tener que volver a buscarlas.
              autoFocus={actuales.mia ? actuales.mia === tipo : i === 0}
              className={`btn btn-ghost h-14 min-h-14 w-14 rounded-full p-0 ${actuales.mia === tipo ? 'bg-primary/20 ring-2 ring-primary' : ''}`}
            >
              <DibujoDeReaccion tipo={tipo} className="h-11 w-12" />
            </button>
          ))}
        </div>
      )}

      {fallo && (
        <p role="alert" className="text-xs text-error">
          No pudimos guardar tu reacción. Probá de nuevo.
        </p>
      )}
    </div>
  )
}
