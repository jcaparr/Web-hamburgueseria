import { useId, useMemo, useRef, useState, type KeyboardEvent } from 'react'
import { Link } from 'react-router-dom'
import type { LocalDelMapa } from '../types'
import { IconSearch } from './icons'

const MAXIMO_DE_SUGERENCIAS = 6

/** Sin acentos ni mayúsculas, igual que el buscador de Explorar: nadie escribe los acentos. */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim()
}

/**
 * El buscador del mapa (#395): busca un local por su nombre, no por barrio, entre los que el mapa
 * ya trajo. No hace pedidos: filtra la lista que tiene.
 *
 * Es un combobox: mientras se escribe salen hasta seis sugerencias (nombre y barrio), se recorren
 * con las flechas, Enter elige la resaltada —o la primera— y Escape las cierra. Cada sugerencia
 * mide 44 px de alto. Sin coincidencias avisa y ofrece buscarlo en la lista de Explorar, donde
 * están también los locales que no están en el mapa.
 */
export function BuscadorDelMapa({
  locales,
  onElegir,
}: {
  locales: LocalDelMapa[]
  onElegir: (local: LocalDelMapa) => void
}) {
  const [texto, setTexto] = useState('')
  const [abierto, setAbierto] = useState(false)
  const [activo, setActivo] = useState(0)
  const campo = useRef<HTMLInputElement>(null)
  const idDeLaLista = useId()

  const nombres = useMemo(() => locales.map((l) => normalizar(l.name)), [locales])

  const sugerencias = useMemo(() => {
    const palabras = normalizar(texto).split(/\s+/).filter(Boolean)
    if (palabras.length === 0) return []
    const encontradas: { local: LocalDelMapa; empieza: boolean }[] = []
    locales.forEach((local, i) => {
      if (palabras.every((p) => nombres[i].includes(p))) {
        encontradas.push({ local, empieza: nombres[i].startsWith(palabras[0]) })
      }
    })
    // Primero los que empiezan como se escribió, y después los más reseñados.
    return encontradas
      .sort((a, b) => Number(b.empieza) - Number(a.empieza) || b.local.ratingsCount - a.local.ratingsCount)
      .slice(0, MAXIMO_DE_SUGERENCIAS)
      .map((s) => s.local)
  }, [locales, nombres, texto])

  const hayTexto = texto.trim() !== ''
  const listaAbierta = abierto && hayTexto
  const resaltada = Math.min(activo, sugerencias.length - 1)

  function elegir(local: LocalDelMapa) {
    setTexto(local.name)
    setAbierto(false)
    campo.current?.blur()
    onElegir(local)
  }

  function alTocarUnaTecla(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault()
      if (sugerencias.length === 0) return
      setAbierto(true)
      const paso = e.key === 'ArrowDown' ? 1 : -1
      setActivo((resaltada + paso + sugerencias.length) % sugerencias.length)
    } else if (e.key === 'Enter') {
      e.preventDefault()
      // Enter sin elegir va a la primera: es lo que se espera de un buscador.
      const local = sugerencias[Math.max(resaltada, 0)]
      if (local) elegir(local)
    } else if (e.key === 'Escape') {
      if (listaAbierta) {
        // Cierra solo las sugerencias; el Escape que cierra la tarjeta del mapa no se entera.
        e.stopPropagation()
        setAbierto(false)
      } else if (hayTexto) {
        e.stopPropagation()
        setTexto('')
      }
    }
  }

  return (
    <div className="relative min-w-0 flex-1 md:max-w-sm">
      <label className="flex min-h-11 items-center gap-2 rounded-full bg-base-100 pl-4 pr-1 shadow-[var(--sombra-control)] focus-within:ring-2 focus-within:ring-primary">
        <IconSearch size={16} className="flex-none text-base-content/70" />
        <input
          ref={campo}
          type="search"
          role="combobox"
          aria-label="Buscar un local"
          aria-expanded={listaAbierta}
          aria-controls={idDeLaLista}
          aria-autocomplete="list"
          aria-activedescendant={listaAbierta && sugerencias.length > 0 ? `${idDeLaLista}-${resaltada}` : undefined}
          autoComplete="off"
          enterKeyHint="search"
          placeholder="Buscar un local"
          value={texto}
          onChange={(e) => {
            setTexto(e.target.value)
            setActivo(0)
            setAbierto(true)
          }}
          onFocus={() => setAbierto(true)}
          onBlur={() => setAbierto(false)}
          onKeyDown={alTocarUnaTecla}
          className="min-w-0 flex-1 bg-transparent py-2 text-base outline-none placeholder:text-base-content/70 [&::-webkit-search-cancel-button]:hidden"
        />
        {hayTexto && (
          <button
            type="button"
            aria-label="Borrar la búsqueda"
            // mousedown y no click: así el campo no pierde el foco antes de que se borre.
            onMouseDown={(e) => {
              e.preventDefault()
              setTexto('')
              setAbierto(false)
              campo.current?.focus()
            }}
            className="grid h-11 w-11 flex-none place-items-center rounded-full text-xl leading-none text-base-content/70 hover:text-base-content"
          >
            <span aria-hidden="true">×</span>
          </button>
        )}
      </label>

      {listaAbierta && (
        <div
          id={idDeLaLista}
          role={sugerencias.length > 0 ? 'listbox' : undefined}
          aria-label="Locales"
          className="absolute inset-x-0 top-full z-30 mt-1.5 overflow-hidden rounded-box bg-base-100 shadow-xl ring-1 ring-inset ring-base-content/15"
        >
          {sugerencias.length > 0 ? (
            sugerencias.map((local, i) => (
              <div
                key={local.id}
                id={`${idDeLaLista}-${i}`}
                role="option"
                aria-selected={i === resaltada}
                // mousedown y no click: el campo pierde el foco con el mousedown y cerraría la lista antes.
                onMouseDown={(e) => {
                  e.preventDefault()
                  elegir(local)
                }}
                onMouseEnter={() => setActivo(i)}
                className={`flex min-h-11 cursor-pointer items-center gap-3 px-3 py-1.5 ${i === resaltada ? 'bg-base-200' : ''}`}
              >
                <span className="h-8 w-8 flex-none overflow-hidden rounded-full bg-base-300">
                  {local.miniatura && <img src={local.miniatura} alt="" className="h-full w-full object-cover" />}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-semibold">{local.name}</span>
                  {local.area && <span className="block truncate text-xs text-base-content/70">{local.area}</span>}
                </span>
              </div>
            ))
          ) : (
            <div role="status" className="p-3 text-sm">
              <p>No encontramos ese local en el mapa.</p>
              <Link
                to={`/?q=${encodeURIComponent(texto.trim())}`}
                onMouseDown={(e) => e.preventDefault()}
                className="mt-1 inline-flex min-h-11 items-center font-semibold text-primary underline underline-offset-4"
              >
                Buscarlo en la lista
              </Link>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
