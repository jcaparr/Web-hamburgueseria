import { IconSearch } from '../components/icons'
import { ResultadosDeGente } from '../components/ResultadosDeGente'
import { useBuscarGente } from '../hooks/useBuscarGente'
import { useTitulo } from '../hooks/useTitulo'

export function BuscarGente() {
  const busqueda = useBuscarGente()
  useTitulo('Buscar gente')

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-2xl md:p-0 md:pt-6">
      <h1 className="titulo-pagina">Buscar gente</h1>

      <label className="input input-bordered flex items-center gap-2 focus-within:border-primary">
        <IconSearch size={16} className="text-base-content/70" />
        <input
          autoFocus
          type="search"
          value={busqueda.texto}
          onChange={(e) => busqueda.setTexto(e.target.value)}
          placeholder="Nombre de usuario…"
          className="grow"
          aria-label="Buscar por nombre de usuario"
          autoComplete="off"
          autoCapitalize="none"
          autoCorrect="off"
          spellCheck={false}
          enterKeyHint="search"
        />
      </label>

      <ResultadosDeGente busqueda={busqueda} />
    </div>
  )
}
