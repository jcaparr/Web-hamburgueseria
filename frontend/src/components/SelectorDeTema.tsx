import { useTema, type Tema } from '../hooks/useTema'

const OPCIONES: { id: Tema; texto: string }[] = [
  { id: 'auto', texto: 'Automático' },
  { id: 'claro', texto: 'Claro' },
  { id: 'oscuro', texto: 'Oscuro' },
]

/**
 * Claro, oscuro o lo que diga el sistema, como tres botones pegados: uno arriba del otro
 * en el teléfono, donde el panel es angosto, y uno al lado del otro en la compu.
 *
 * Por dentro son radios y no botones: es una sola elección entre tres, y así el lector de
 * pantalla dice "1 de 3, seleccionado" y las flechas pasan de una a otra.
 */
export function SelectorDeTema() {
  const [tema, elegir] = useTema()

  return (
    <fieldset className="flex flex-col">
      <legend className="px-2 pb-1 pt-2 text-xs font-semibold text-base-content/70">Apariencia</legend>
      <div className="flex flex-col rounded-box bg-base-200 p-1 md:flex-row md:rounded-full">
        {OPCIONES.map((opcion) => (
          <label
            key={opcion.id}
            className={`relative flex min-h-11 flex-1 cursor-pointer items-center rounded-field px-4 text-sm md:justify-center md:rounded-full font-semibold transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-primary ${
              tema === opcion.id ? 'bg-base-100 shadow-[var(--sombra-tarjeta)]' : 'text-base-content/70 hover:text-base-content'
            }`}
          >
            <input
              type="radio"
              name="tema"
              value={opcion.id}
              checked={tema === opcion.id}
              onChange={() => elegir(opcion.id)}
              className="sr-only"
            />
            {opcion.texto}
          </label>
        ))}
      </div>
      <p className="sr-only">Automático sigue lo que tengas elegido en el teléfono o la compu.</p>
    </fieldset>
  )
}
