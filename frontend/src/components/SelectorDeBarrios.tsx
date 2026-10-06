import { useEffect, useId, useMemo, useRef, useState } from 'react'
import { IconChevronRight, IconSearch } from './icons'

/**
 * Elegir uno o varios barrios para filtrar Explorar.
 *
 * Era un `<select>` de un solo barrio. Dejó de alcanzar por dos motivos a la vez: ahora
 * se puede querer mirar dos barrios vecinos juntos —Palermo y Villa Crespo son el mismo
 * paseo— y la lista pasó de 48 opciones a más de doscientas cuando la búsqueda se abrió
 * a 75 km. Un desplegable nativo con doscientas opciones, en el celular, es una tira
 * infinita sin forma de buscar.
 *
 * Así que es un botón que abre una ventana con buscador y casillas. El buscador es lo
 * que vuelve manejable la lista larga: tipeás "quil" y quedan los de Quilmes.
 *
 * Los elegidos van arriba de todo y no en su lugar alfabético. Con la lista entera
 * desplegada, si quedaran en su lugar habría que ir a buscarlos para destildarlos, y
 * desde abajo ni se ven.
 */
export function SelectorDeBarrios({
  barrios,
  elegidos,
  onAlternar,
  onLimpiar,
  textoSinElegir = 'Todos los barrios',
  claseDelBoton = 'btn-sm',
}: {
  /** Todos los barrios que tienen al menos un local, como los da el servidor. */
  barrios: string[]
  elegidos: string[]
  /**
   * Marca o desmarca uno. Se avisa cuál se tocó y no la lista nueva a propósito: la
   * lista nueva habría que calcularla con los elegidos que llegaron al dibujar, y entre
   * dos clics seguidos eso es una lectura vieja que hace que el segundo pise al primero.
   */
  onAlternar: (barrio: string) => void
  onLimpiar: () => void
  /** Lo que dice el botón sin ninguno elegido: en Tour, por ejemplo, es "Cerca mío". */
  textoSinElegir?: string
  claseDelBoton?: string
}) {
  const dialogo = useRef<HTMLDialogElement>(null)
  const idDelTitulo = useId()
  const [abierto, setAbierto] = useState(false)
  const [buscado, setBuscado] = useState('')

  // Igual que en el modal de reseña: showModal es lo que da el foco atrapado, el Esc y
  // el fondo inerte, y no hay forma de pedirlo desde el JSX.
  useEffect(() => {
    const ventana = dialogo.current
    if (!ventana) return
    if (abierto && !ventana.open) ventana.showModal()
    if (!abierto && ventana.open) ventana.close()
  }, [abierto])

  // Al abrir se limpia lo buscado: lo que se tipeó la vez pasada no tiene nada que ver
  // con lo que se viene a buscar ahora, y dejarlo esconde media lista sin avisar. Va en
  // el clic que abre y no en un efecto que mire si se abrió: es la misma cosa, sin un
  // dibujo de más.
  function abrir() {
    setBuscado('')
    setAbierto(true)
  }

  const visibles = useMemo(() => {
    const busca = sinAcentos(buscado)
    const coinciden = busca
      ? barrios.filter((b) => sinAcentos(b).includes(busca))
      : barrios

    // Los elegidos primero, y entre ellos y el resto el orden que ya traían.
    const marcados = coinciden.filter((b) => elegidos.includes(b))
    const resto = coinciden.filter((b) => !elegidos.includes(b))
    return [...marcados, ...resto]
  }, [barrios, elegidos, buscado])

  return (
    <>
      <button
        type="button"
        onClick={abrir}
        aria-haspopup="dialog"
        className={`btn rounded-full ${claseDelBoton} ${elegidos.length > 0 ? 'btn-neutral' : 'border-0 bg-base-100 shadow-[var(--sombra-tarjeta)]'}`}
      >
        <span className="truncate">{comoSeLee(elegidos, textoSinElegir)}</span>
        {/* La flecha dice que el botón abre una lista. */}
        <IconChevronRight size={14} className="ml-auto flex-none rotate-90" />
      </button>

      <dialog
        ref={dialogo}
        aria-labelledby={idDelTitulo}
        className="modal modal-bottom sm:modal-middle"
        onCancel={(e) => {
          e.preventDefault()
          setAbierto(false)
        }}
      >
        <div className="modal-box flex max-h-[80dvh] flex-col gap-3">
          <div className="flex items-center justify-between gap-3">
            <h2 id={idDelTitulo} className="font-display text-lg font-bold">
              Barrios
            </h2>
            {/* Limpiar está arriba y no al final de doscientas filas: deshacer la
                selección entera es lo que más se busca y no se puede pedir que para eso
                haya que scrollear hasta el fondo. */}
            {elegidos.length > 0 && (
              <button type="button" onClick={onLimpiar} className="btn btn-ghost btn-sm">
                Desmarcar todos
              </button>
            )}
          </div>

          <label className="flex items-center gap-2 rounded-xl border border-base-content/20 px-3 py-2.5 focus-within:border-primary">
            <IconSearch size={15} className="text-base-content/70" />
            <input
              type="search"
              value={buscado}
              onChange={(e) => setBuscado(e.target.value)}
              placeholder="Buscar barrio…"
              aria-label="Buscar barrio"
              autoComplete="off"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              className="w-full bg-transparent text-base outline-none placeholder:text-base-content/70 md:text-sm"
            />
          </label>

          <ul className="flex flex-col overflow-y-auto">
            {visibles.map((barrio) => (
              <li key={barrio}>
                <label className="flex min-h-11 cursor-pointer items-center gap-3 py-1 text-sm">
                  <input
                    type="checkbox"
                    className="checkbox checkbox-sm checkbox-primary"
                    checked={elegidos.includes(barrio)}
                    onChange={() => onAlternar(barrio)}
                  />
                  <span>{barrio}</span>
                </label>
              </li>
            ))}
            {visibles.length === 0 && (
              <li className="py-3 text-sm text-base-content/70">
                Ningún barrio se llama así.
              </li>
            )}
          </ul>

          <button type="button" onClick={() => setAbierto(false)} className="btn btn-primary">
            Listo
          </button>
        </div>

        {/* El clic afuera cierra. Fuera del orden del Tab: es un botón invisible, y con
            el teclado ya están "Listo" y Esc. */}
        <form method="dialog" className="modal-backdrop">
          <button type="button" tabIndex={-1} aria-hidden="true" onClick={() => setAbierto(false)}>
            Cerrar
          </button>
        </form>
      </dialog>
    </>
  )
}

/**
 * Qué dice el botón.
 *
 * Con uno o dos se nombran, que es lo que deja ver qué está filtrado sin abrir nada. De
 * tres en adelante no entran, así que se cuentan: "4 barrios" ocupa lo mismo siempre y
 * no empuja al resto de la fila.
 */
function comoSeLee(elegidos: string[], sinElegir: string) {
  if (elegidos.length === 0) return sinElegir
  if (elegidos.length === 1) return elegidos[0]
  if (elegidos.length === 2) return `${elegidos[0]} y ${elegidos[1]}`
  return `${elegidos.length} barrios`
}

/** Las mismas reglas que usa el servidor para buscar: nadie escribe "Núñez" con acento. */
function sinAcentos(valor: string) {
  return valor.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim()
}
