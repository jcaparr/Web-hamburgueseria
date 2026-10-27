import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { LoadError } from '../components/LoadError'
import { PodioDePersonas } from '../components/PodioDePersonas'
import { useAuth } from '../context/useAuth'
import { MedallaDeLogro } from '../components/MedallaDeLogro'
import { usePedido } from '../hooks/usePedido'
import { useTitulo } from '../hooks/useTitulo'
import type { LogroDeLaPantalla, PantallaDeLogros } from '../types'
import { CATALOGO, GRUPOS, POR_CODIGO, VISIBLES, nombreDe } from '../utils/logros'

/**
 * La pestaña Mis logros (#316): dónde estás en los niveles, lo que te falta poco para
 * ganar y todo el catálogo por grupos. Tocar un logro abre su hoja.
 *
 * Los secretos se ven como "???" hasta ganarlos, y no cuentan en el total del título.
 * El servidor recalcula antes de responder, así que lo que se ve es lo de hoy.
 */
function MisLogros() {
  const pedido = usePedido('logros', () => apiClient.get<PantallaDeLogros>('/logros').then((r) => r.data))
  const { user, exhibirLogro } = useAuth()
  // Desde la campana (#320): /logros?abrir=CODIGO abre la hoja de ese logro.
  const [parametros, setParametros] = useSearchParams()
  const [abierto, setAbierto] = useState<string | null>(() => parametros.get('abrir'))
  useEffect(() => {
    if (parametros.has('abrir')) setParametros({}, { replace: true })
  }, [parametros, setParametros])
  // A qué tarjeta volver el foco al cerrar la hoja.
  const origen = useRef<HTMLElement | null>(null)

  if (pedido.error) {
    return (
      <div className="p-4 md:mx-auto md:max-w-3xl md:p-0 md:pt-6">
        <LoadError error={pedido.error} onRetry={pedido.reintentar} />
      </div>
    )
  }
  const datos = pedido.datos
  if (!datos) {
    return (
      <p role="status" className="p-4 text-sm text-base-content/70 md:mx-auto md:max-w-3xl md:p-0 md:pt-6">
        Cargando…
      </p>
    )
  }

  const porCodigo = new Map(datos.logros.map((l) => [l.codigo, l]))
  const ganados = datos.logros.filter((l) => l.ganado && !POR_CODIGO[l.codigo]?.secreto).length
  const cerca = datos.logros
    .filter((l) => !l.ganado && l.meta !== null && l.actual !== null && l.actual > 0 && !POR_CODIGO[l.codigo]?.secreto)
    .sort((a, b) => (b.actual! / b.meta!) - (a.actual! / a.meta!))
    .slice(0, 3)

  function abrir(codigo: string, elemento: HTMLElement) {
    origen.current = elemento
    setAbierto(codigo)
  }

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-2">
        <h1 className="titulo-pagina">
          Logros <span className="font-normal text-base-content/70 tabular-nums">({ganados} de {VISIBLES})</span>
        </h1>
        <BarraDeNivel datos={datos} />
      </header>

      {cerca.length > 0 && (
        <section aria-labelledby="cerca" className="flex flex-col gap-1">
          <h2 id="cerca" className="font-display text-lg font-bold">
            Cerca de ganarlos
          </h2>
          <ul className="tarjeta px-4">
            {cerca.map((logro) => (
              <li key={logro.codigo} className="border-t border-base-content/10 first:border-t-0">
                <button
                  type="button"
                  onClick={(e) => abrir(logro.codigo, e.currentTarget)}
                  className="flex min-h-14 w-full items-center gap-3 py-3 text-left"
                >
                  <MedallaDeLogro codigo={logro.codigo} ganado={false} size={40} />
                  <span className="flex min-w-0 flex-1 flex-col gap-1">
                    <span className="truncate text-sm font-semibold">{nombreDe(logro.codigo, logro.detalle)}</span>
                    <Barra actual={logro.actual!} meta={logro.meta!} />
                    <span className="text-sm text-base-content/70">{medidaDe(logro)}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}

      {GRUPOS.map((grupo) => (
        <section key={grupo} aria-labelledby={`grupo-${grupo}`} className="flex flex-col gap-3">
          <h2 id={`grupo-${grupo}`} className="font-display text-lg font-bold">
            {grupo}
          </h2>
          <ul className="grid grid-cols-3 gap-2.5 md:grid-cols-4">
            {CATALOGO.filter((l) => l.grupo === grupo).map((l) => {
              const logro = porCodigo.get(l.codigo)
              return (
                <li key={l.codigo}>
                  <Tarjeta datos={logro} codigo={l.codigo} onAbrir={abrir} />
                </li>
              )
            })}
          </ul>
        </section>
      ))}

      <HojaDeLogro
        logro={abierto ? porCodigo.get(abierto) ?? null : null}
        exhibido={user?.logroExhibido ?? null}
        onExhibir={(codigo) => exhibirLogro(codigo)}
        onCerrar={() => {
          setAbierto(null)
          origen.current?.focus()
        }}
      />
    </div>
  )
}

/** El nivel y la barra hacia el siguiente. La barra va en rojo: la mostaza sobre crema no se ve. */
function BarraDeNivel({ datos }: { datos: PantallaDeLogros }) {
  const siguiente = datos.xpDelSiguiente
  const avance = siguiente === null ? 1 : (datos.xp - datos.xpDelNivel) / (siguiente - datos.xpDelNivel)
  return (
    <div className="flex flex-col gap-1.5">
      <p className="text-sm font-semibold tabular-nums">
        Nivel {datos.nivel} · {datos.xp.toLocaleString('es-AR')} XP
      </p>
      <div
        role="progressbar"
        aria-label="Avance hacia el siguiente nivel"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={Math.round(avance * 100)}
        className="h-2 overflow-hidden rounded-full bg-base-content/15"
      >
        <div className="h-full rounded-full bg-primary" style={{ width: `${Math.max(0, Math.min(1, avance)) * 100}%` }} />
      </div>
    </div>
  )
}

function Barra({ actual, meta }: { actual: number; meta: number }) {
  return (
    <div aria-hidden className="h-2 overflow-hidden rounded-full bg-base-content/15">
      <div className="h-full rounded-full bg-primary" style={{ width: `${Math.min(100, (actual / meta) * 100)}%` }} />
    </div>
  )
}

/** Una chapita con su nombre, y debajo la fecha si se ganó o el avance si todavía no. */
function Tarjeta({
  datos,
  codigo,
  onAbrir,
}: {
  datos: LogroDeLaPantalla | undefined
  codigo: string
  onAbrir: (codigo: string, elemento: HTMLElement) => void
}) {
  const ganado = !!datos?.ganado
  const secreto = !!POR_CODIGO[codigo]?.secreto
  const oculto = secreto && !ganado
  const nombre = oculto ? '???' : nombreDe(codigo, datos?.detalle)
  // En la tarjeta el avance va corto, sin el sustantivo: con tres columnas en 320 px no entra más.
  const pie = ganado ? fechaCorta(datos?.fecha) : oculto ? 'Secreto' : datos ? avanceCorto(datos) : ''
  const etiqueta = oculto
    ? 'Logro secreto, sin ganar'
    : `${nombre}, ${ganado ? `ganado el ${fechaLarga(datos?.fecha ?? null)}` : `sin ganar${datos ? `, ${medidaDe(datos)}` : ''}`}`
  return (
    <button
      type="button"
      onClick={(e) => onAbrir(codigo, e.currentTarget)}
      aria-label={etiqueta}
      className="tarjeta flex h-36 w-full flex-col items-center px-1.5 pb-2 pt-3 text-center"
    >
      <MedallaDeLogro codigo={codigo} ganado={ganado} size={56} oculto={oculto} />
      <strong className="mt-4 flex h-8 w-full items-center justify-center text-[0.8125rem] font-bold leading-4">
        <span className="line-clamp-2 [overflow-wrap:anywhere]">{nombre}</span>
      </strong>
      <span className="whitespace-nowrap text-[0.8125rem] leading-4 text-base-content/70">{pie}</span>
    </button>
  )
}

/** La hoja de un logro: qué es y cuánto falta, o cuándo se ganó. Se cierra con Esc o tocando afuera. */
function HojaDeLogro({
  logro,
  exhibido,
  onExhibir,
  onCerrar,
}: {
  logro: LogroDeLaPantalla | null
  exhibido: string | null
  onExhibir: (codigo: string | null) => Promise<void>
  onCerrar: () => void
}) {
  const dialogo = useRef<HTMLDialogElement>(null)
  const [falla, setFalla] = useState(false)

  useEffect(() => {
    const ventana = dialogo.current
    if (!ventana) return
    if (logro && !ventana.open) ventana.showModal()
    if (!logro && ventana.open) ventana.close()
  }, [logro])

  const datos = logro ? POR_CODIGO[logro.codigo] : null
  const oculto = !!datos?.secreto && !logro?.ganado

  return (
    <dialog
      ref={dialogo}
      aria-labelledby="titulo-hoja"
      className="modal modal-bottom sm:modal-middle"
      onCancel={(e) => {
        e.preventDefault()
        onCerrar()
      }}
    >
      <div className="modal-box flex flex-col items-center gap-4 text-center">
        {logro && datos && (
          <>
            <MedallaDeLogro codigo={logro.codigo} ganado={logro.ganado} size={96} oculto={oculto} />
            <h2 id="titulo-hoja" className="font-display text-xl font-bold">
              {oculto ? 'Logro secreto' : nombreDe(logro.codigo, logro.detalle)}
            </h2>
            <p className="max-w-[34ch] text-sm text-base-content/80">
              {oculto ? 'Es un secreto: se muestra cuando lo ganes.' : datos.texto}
            </p>
            {logro.ganado ? (
              <p className="text-sm font-semibold">Lo ganaste el {fechaLarga(logro.fecha)}.</p>
            ) : (
              !oculto && logro.meta !== null && logro.actual !== null && (
                <div className="flex w-full max-w-xs flex-col gap-1.5">
                  <Barra actual={logro.actual} meta={logro.meta} />
                  <p className="text-sm text-base-content/70">{medidaDe(logro)}</p>
                </div>
              )
            )}
            {logro.ganado && (
              <button
                type="button"
                onClick={() => {
                  setFalla(false)
                  onExhibir(exhibido === logro.codigo ? null : logro.codigo).catch(() => setFalla(true))
                }}
                className="btn btn-outline w-full"
              >
                {exhibido === logro.codigo ? 'Quitar de mi perfil' : 'Exhibir en mi perfil'}
              </button>
            )}
            {falla && (
              <p role="alert" className="text-sm text-error">
                No pudimos guardarlo. Probá de nuevo.
              </p>
            )}
            <button type="button" onClick={onCerrar} className="btn btn-primary w-full">
              Cerrar
            </button>
          </>
        )}
      </div>
      <form method="dialog" className="modal-backdrop">
        <button type="button" tabIndex={-1} aria-hidden="true" onClick={onCerrar}>
          Cerrar
        </button>
      </form>
    </dialog>
  )
}

function medidaDe(logro: LogroDeLaPantalla): string {
  const medida = POR_CODIGO[logro.codigo]?.medida
  return medida && logro.actual !== null && logro.meta !== null ? medida(logro.actual, logro.meta) : ''
}

/** "2 de 3", sin lo que cuenta: el sustantivo va en la hoja. */
function avanceCorto(logro: LogroDeLaPantalla): string {
  return logro.actual !== null && logro.meta !== null
    ? `${logro.actual.toLocaleString('es-AR')} de ${logro.meta.toLocaleString('es-AR')}`
    : ''
}

function fechaCorta(fecha: string | null | undefined): string {
  return fecha ? new Date(fecha).toLocaleDateString('es-AR', { day: 'numeric', month: 'short' }).replace('.', '') : ''
}

function fechaLarga(fecha: string | null): string {
  return fecha ? new Date(fecha).toLocaleDateString('es-AR', { day: 'numeric', month: 'long', year: 'numeric' }) : ''
}

/**
 * La pantalla de Logros: dos pestañas, Mis logros y el Podio de personas (#318).
 */
export function Logros() {
  useTitulo('Logros')
  const { user } = useAuth()
  const [pestania, setPestania] = useState<'logros' | 'podio'>('logros')
  if (!user) return null
  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-3xl md:p-0 md:pt-6">
      <div role="tablist" aria-label="Logros" className="flex rounded-full bg-base-200 p-1">
        {([['logros', 'Mis logros'], ['podio', 'Podio']] as const).map(([id, texto]) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={pestania === id}
            onClick={() => setPestania(id)}
            className={`flex min-h-11 flex-1 items-center justify-center rounded-full text-sm font-semibold ${
              pestania === id ? 'bg-base-100 shadow-[var(--sombra-tarjeta)]' : 'text-base-content/70'
            }`}
          >
            {texto}
          </button>
        ))}
      </div>
      {pestania === 'logros' ? <MisLogros /> : <PodioDePersonas yoId={user.userId} />}
    </div>
  )
}
