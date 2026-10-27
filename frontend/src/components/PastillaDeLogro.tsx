import { useEffect, useRef, useState } from 'react'
import { apiClient } from '../api/client'
import { useAuth } from '../context/useAuth'
import type { PantallaDeLogros } from '../types'
import { CATALOGO, nombreDe } from '../utils/logros'
import { MedallaDeLogro } from './MedallaDeLogro'

/**
 * El logro exhibido en el perfil (#319): una pastilla debajo de las cifras con solo la
 * medalla y el nombre.
 *
 * En tu perfil, tocarla abre el selector de tus logros ganados; sin ninguno elegido dice
 * "Elegí logro para mostrar". En el de otra persona abre la hoja con sus logros; sin
 * ninguno elegido dice "Ver logros".
 *
 * @param logros los códigos que se pueden ver de la otra persona (en el perfil propio se
 *               piden los tuyos al servidor)
 */
export function PastillaDeLogro({
  exhibido,
  propia,
  logros,
}: {
  exhibido: string | null
  propia: boolean
  logros?: string[]
}) {
  const [abierta, setAbierta] = useState(false)
  const dialogo = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const ventana = dialogo.current
    if (!ventana) return
    if (abierta && !ventana.open) ventana.showModal()
    if (!abierta && ventana.open) ventana.close()
  }, [abierta])

  const nombre = exhibido ? nombreDe(exhibido) : propia ? 'Elegí logro para mostrar' : 'Ver logros'
  return (
    <>
      <button
        type="button"
        onClick={() => setAbierta(true)}
        className={`mx-auto mt-1 inline-flex min-h-12 max-w-full items-center gap-2.5 rounded-full px-4 py-1.5 text-sm font-semibold ${
          exhibido ? 'bg-base-100 shadow-[var(--sombra-tarjeta)]' : 'text-primary shadow-[inset_0_0_0_1.5px_var(--color-base-300)]'
        }`}
      >
        {exhibido && <MedallaDeLogro codigo={exhibido} ganado size={36} sinNumero />}
        <span className="min-w-0 truncate">{nombre}</span>
      </button>
      <dialog
        ref={dialogo}
        aria-labelledby="titulo-logros-del-perfil"
        className="modal modal-bottom sm:modal-middle"
        onCancel={(e) => {
          e.preventDefault()
          setAbierta(false)
        }}
      >
        <div className="modal-box flex flex-col gap-4">
          {abierta &&
            (propia ? (
              <Selector exhibido={exhibido} onListo={() => setAbierta(false)} />
            ) : (
              <Lista codigos={logros ?? []} onListo={() => setAbierta(false)} />
            ))}
        </div>
        <form method="dialog" className="modal-backdrop">
          <button type="button" tabIndex={-1} aria-hidden="true" onClick={() => setAbierta(false)}>
            Cerrar
          </button>
        </form>
      </dialog>
    </>
  )
}

/** Tus logros ganados, para elegir cuál mostrar. */
function Selector({ exhibido, onListo }: { exhibido: string | null; onListo: () => void }) {
  const { exhibirLogro } = useAuth()
  const [ganados, setGanados] = useState<string[] | null>(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    let vigente = true
    apiClient
      .get<PantallaDeLogros>('/logros')
      .then(({ data }) => vigente && setGanados(data.logros.filter((l) => l.ganado).map((l) => l.codigo)))
      .catch(() => vigente && setError(true))
    return () => {
      vigente = false
    }
  }, [])

  async function elegir(codigo: string | null) {
    try {
      await exhibirLogro(codigo)
      onListo()
    } catch {
      setError(true)
    }
  }

  return (
    <>
      <h2 id="titulo-logros-del-perfil" className="font-display text-lg font-bold">
        Elegí el logro para mostrar
      </h2>
      {error && (
        <p role="alert" className="text-sm text-error">
          No pudimos guardarlo. Probá de nuevo.
        </p>
      )}
      {!ganados ? (
        <p role="status" className="text-sm text-base-content/70">
          Cargando…
        </p>
      ) : (
        <ul className="grid grid-cols-3 gap-2">
          {CATALOGO.filter((l) => ganados.includes(l.codigo)).map((l) => (
            <li key={l.codigo}>
              <button
                type="button"
                onClick={() => elegir(l.codigo)}
                aria-pressed={exhibido === l.codigo}
                className={`flex min-h-24 w-full flex-col items-center gap-1 rounded-box p-2 text-center text-[0.8125rem] font-semibold ${
                  exhibido === l.codigo ? 'bg-base-200 ring-2 ring-primary' : 'bg-base-200/60'
                }`}
              >
                <MedallaDeLogro codigo={l.codigo} ganado size={44} sinNumero />
                <span className="line-clamp-2 [overflow-wrap:anywhere]">{nombreDe(l.codigo)}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
      <div className="flex flex-col gap-2">
        {exhibido && (
          <button type="button" onClick={() => elegir(null)} className="btn btn-outline">
            Sacar el logro del perfil
          </button>
        )}
        <button type="button" onClick={onListo} className="btn btn-ghost">
          Cerrar
        </button>
      </div>
    </>
  )
}

/** Los logros que se ven de otra persona. */
function Lista({ codigos, onListo }: { codigos: string[]; onListo: () => void }) {
  return (
    <>
      <h2 id="titulo-logros-del-perfil" className="font-display text-lg font-bold">
        Logros
      </h2>
      {codigos.length === 0 ? (
        <p className="text-sm text-base-content/70">Todavía no tiene logros para mostrar.</p>
      ) : (
        <ul className="grid grid-cols-3 gap-2">
          {CATALOGO.filter((l) => codigos.includes(l.codigo)).map((l) => (
            <li key={l.codigo} className="flex min-h-24 flex-col items-center gap-1 rounded-box bg-base-200/60 p-2 text-center text-[0.8125rem] font-semibold">
              <MedallaDeLogro codigo={l.codigo} ganado size={44} sinNumero />
              <span className="line-clamp-2 [overflow-wrap:anywhere]">{nombreDe(l.codigo)}</span>
            </li>
          ))}
        </ul>
      )}
      <button type="button" onClick={onListo} className="btn btn-primary">
        Cerrar
      </button>
    </>
  )
}

