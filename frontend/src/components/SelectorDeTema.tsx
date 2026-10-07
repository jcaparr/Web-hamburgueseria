import { useTema, type Tema } from '../hooks/useTema'

const OPCIONES: { id: Tema; texto: string }[] = [
  { id: 'auto', texto: 'Automático' },
  { id: 'claro', texto: 'Claro' },
  { id: 'oscuro', texto: 'Oscuro' },
]

/**
 * Claro, oscuro o lo que diga el sistema, como tres botones pegados.
 *
 * Por dentro son radios y no botones: es una sola elección entre tres, y así el lector de
 * pantalla dice "1 de 3, seleccionado" y las flechas pasan de una a otra.
 */
export function SelectorDeTema() {
  const [tema, elegir] = useTema()

  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-sm font-semibold">Apariencia</legend>
      <div className="flex rounded-full bg-base-200 p-1">
        {OPCIONES.map((opcion) => (
          <label
            key={opcion.id}
            className={`relative flex min-h-11 flex-1 cursor-pointer items-center justify-center rounded-full text-sm font-semibold transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-primary ${
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
      <p className="text-xs text-base-content/70">Automático sigue lo que tengas elegido en el teléfono o la compu.</p>
    </fieldset>
  )
}
