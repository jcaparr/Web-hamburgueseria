import { useCallback, useEffect, useRef, useState, type CSSProperties, type KeyboardEvent, type ReactNode } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { apiClient } from '../api/client'
import { AvatarDeUsuario } from '../components/AvatarDeUsuario'
import { IconArrowLeft, IconChevronRight, IconShare } from '../components/icons'
import { LoadError } from '../components/LoadError'
import { Personaje } from '../components/Personaje'
import { useAuth } from '../context/useAuth'
import { useTitulo } from '../hooks/useTitulo'
import type { TuMes as TuMesDatos } from '../types'
import { isSessionExpired } from '../utils/errors'
import { compartirImagen } from '../utils/compartirImagen'
import { cantidad, nota } from '../utils/numeros'
import { rutaDelLocal } from '../utils/rutaDelLocal'
import { armarTarjetaDeResumen } from '../utils/tarjetaDeResumen'
import { NITIDEZ, anioDelMes, comparacionCon, diaDe, franjaEnPalabras, nombreDelDia, nombreDelMes, nombreDelMesNumero, periodoDe, tarjetasDe, type TipoDeTarjeta } from '../utils/tuMes'
import { DibujoDeReaccion } from '../components/DibujoDeReaccion'
import { reaccion } from '../utils/reacciones'
import { MedallaDeLogro } from '../components/MedallaDeLogro'
import { nombreDe } from '../utils/logros'
import './TuMes.css'

/**
 * "Tu mes en Burgómetro" (#308): el resumen del mes en tarjetas a pantalla completa, como
 * las historias. Se pasa deslizando, tocando los costados, con los botones o con las
 * flechas del teclado. Una cifra por tarjeta.
 *
 * El fondo es de marca (brasa y mostaza, con rayos que se van prendiendo de una tarjeta a la
 * otra), sin la foto del local: así todas se ven igual. Los estilos y qué se anima,
 * en TuMes.css.
 */
export function TuMes() {
  const [parametros] = useSearchParams()
  const mesPedido = parametros.get('mes')
  const navigate = useNavigate()
  const [resumen, setResumen] = useState<TuMesDatos | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    apiClient
      .get<TuMesDatos>('/profile/tu-mes', { params: mesPedido ? { mes: mesPedido } : undefined })
      // Con un servidor que todavía no manda logros ni podios (despliegue a medias), listas vacías.
      .then((res) => vigente && setResumen({ ...res.data, logrosGanados: res.data.logrosGanados ?? [], podios: res.data.podios ?? [] }))
      .catch((err) => {
        if (!vigente) return
        if (isSessionExpired(err)) navigate('/login')
        else setError(err)
      })
    return () => {
      vigente = false
    }
  }, [mesPedido, navigate, intento])

  // Entrar al resumen (desde la campana, el pop up o la dirección) es verlo: el pop up no vuelve (#405).
  const esElDeHoy = resumen !== null && !mesPedido
  useEffect(() => {
    if (esElDeHoy) apiClient.post('/profile/tu-mes/visto').catch(() => {})
  }, [esElDeHoy])

  useTitulo(resumen ? `Tu ${nombreDelMes(resumen.mes)}` : 'Tu mes')

  const cerrar = useCallback(() => navigate('/profile', { replace: true }), [navigate])

  useEffect(() => {
    function alTeclear(e: globalThis.KeyboardEvent) {
      if (e.key === 'Escape') cerrar()
    }
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [cerrar])

  if (error) {
    return (
      <Pantalla alCerrar={cerrar}>
        <div className="mx-auto w-full max-w-[480px] p-4">
          <LoadError error={error} onRetry={() => { setError(null); setIntento((n) => n + 1) }} />
        </div>
      </Pantalla>
    )
  }
  if (!resumen) {
    return (
      <Pantalla alCerrar={cerrar}>
        <p role="status" className="p-8 text-center text-lg">Armando tu mes…</p>
      </Pantalla>
    )
  }
  if (resumen.resenias === 0) {
    return (
      <Pantalla alCerrar={cerrar}>
        <div className="mx-auto flex w-full max-w-[480px] flex-col items-center gap-4 p-8 text-center">
          <Personaje pose="lupa" className="h-32 w-auto" />
          <p className="text-lg font-semibold">En {nombreDelMes(resumen.mes)} no hiciste reseñas.</p>
          <p>Este mes sí. Con la primera, ya tenés tu resumen del próximo.</p>
          <Link to="/" className="btn btn-primary min-h-11">Buscar una hamburguesería</Link>
        </div>
      </Pantalla>
    )
  }
  return <Tarjetas resumen={resumen} alCerrar={cerrar} />
}

/** Las pantallas de estado (cargando, error, vacío): crema, con el botón de cerrar. */
function Pantalla({ alCerrar, children }: { alCerrar: () => void; children: ReactNode }) {
  return (
    <div className="fixed inset-0 z-40 flex flex-col bg-[#faf3e3] text-[#3a2a1e]">
      <header className="mx-auto flex w-full max-w-[480px] items-center gap-2 px-2 pt-[max(0.5rem,env(safe-area-inset-top))]">
        <button type="button" onClick={alCerrar} className="flex min-h-12 min-w-12 items-center justify-center gap-1.5 rounded-field px-2 font-semibold" aria-label="Cerrar y volver al perfil">
          <IconArrowLeft size={20} />
          <span className="text-sm">Cerrar</span>
        </button>
      </header>
      {children}
    </div>
  )
}

/** Si conviene dejar el fondo quieto: pidió menos movimiento, o el celular es modesto. */
function sinMovimiento(): boolean {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return true
  const memoria = (navigator as Navigator & { deviceMemory?: number }).deviceMemory
  return (navigator.hardwareConcurrency ?? 8) <= 4 || (memoria !== undefined && memoria <= 4)
}

/**
 * Una cifra que cuenta desde cero hasta su valor. Para el lector de pantalla es el valor
 * final desde el principio (el conteo va oculto): no anuncia 0, 1, 2.
 */
function Cuenta({ valor, decimales = 0, activa, demora = 0, etiqueta, className }: {
  valor: number
  decimales?: number
  activa: boolean
  demora?: number
  etiqueta: string
  className?: string
}) {
  // Cuánto del conteo va hecho, de 0 a 1. Solo cambia dentro del cuadro de animación, así
  // sin movimiento (o sin estar a la vista) queda en 1 y se ve el valor final.
  const [avance, setAvance] = useState(1)
  useEffect(() => {
    if (!activa || sinMovimiento()) return
    let raf = 0
    const t0 = performance.now() + demora
    const paso = (ahora: number) => {
      const t = Math.min(1, Math.max(0, (ahora - t0) / 1000))
      setAvance(1 - Math.pow(1 - t, 3))
      if (t < 1) raf = requestAnimationFrame(paso)
    }
    raf = requestAnimationFrame(paso)
    return () => cancelAnimationFrame(raf)
  }, [activa, demora])
  const visto = decimales ? nota(valor * avance) : cantidad(Math.round(valor * avance))
  return (
    <p className={className} role="img" aria-label={etiqueta}>
      {visto}
    </p>
  )
}

type TipoDeHoja = 'logros' | 'barrios'

/**
 * La lista completa de los logros o de los barrios, en una hoja que sube desde abajo.
 *
 * Mientras está abierta, lo de atrás (la barra de arriba, las tarjetas y los botones) queda
 * `inert`: no se puede deslizar, ni tocar, ni llegar con el teclado, así que el resumen no
 * avanza por debajo. El foco pasa al botón de cerrar y vuelve al botón que la abrió.
 */
function Hoja({ tipo, resumen, alCerrar }: { tipo: TipoDeHoja; resumen: TuMesDatos; alCerrar: () => void }) {
  const cerrar = useRef<HTMLButtonElement>(null)
  useEffect(() => cerrar.current?.focus({ preventScroll: true }), [])
  const titulo =
    tipo === 'barrios'
      ? resumen.barriosEnTotal === 1 ? 'Recorriste un barrio' : `Recorriste ${resumen.barriosEnTotal} barrios`
      : resumen.logrosGanados.length === 1 ? 'Ganaste un logro' : `Ganaste ${resumen.logrosGanados.length} logros`
  return (
    <div className="tm-hoja-fondo" onClick={alCerrar}>
      <div
        className="tm-hoja"
        role="dialog"
        aria-modal="true"
        aria-labelledby="tm-hoja-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="tm-hoja-cab">
          <h2 id="tm-hoja-titulo">{titulo}</h2>
          <button ref={cerrar} type="button" className="tm-hoja-cerrar" onClick={alCerrar} aria-label="Cerrar la lista">
            <svg viewBox="0 0 20 20" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" aria-hidden="true"><path d="M4 4l12 12M16 4L4 16" /></svg>
          </button>
        </div>
        <div className="tm-hoja-cuerpo" tabIndex={0} role="region" aria-label={tipo === "barrios" ? "Lista de barrios" : "Lista de logros"}>
          {tipo === 'barrios' ? (
            <ol className="tm-barrios">
              {resumen.barrios.map((b) => (
                <li key={b.nombre}>
                  <b>{b.nombre}</b>
                  <span className="tm-nota" role="img" aria-label={b.resenias === 1 ? '1 reseña' : `${b.resenias} reseñas`}>{b.resenias}</span>
                </li>
              ))}
            </ol>
          ) : (
            <ul className="tm-logros">
              {resumen.logrosGanados.map((l) => (
                <li key={l.codigo}>
                  <MedallaDeLogro codigo={l.codigo} ganado size={68} />
                  <span className="tm-nombre">{nombreDe(l.codigo, l.detalle)}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}

function Tarjetas({ resumen, alCerrar }: { resumen: TuMesDatos; alCerrar: () => void }) {
  const { user } = useAuth()
  const tarjetas = tarjetasDe(resumen)
  const [actual, setActual] = useState(0)
  const pista = useRef<HTMLDivElement>(null)
  const [armando, setArmando] = useState(false)
  const [aviso, setAviso] = useState<string | null>(null)
  const [quieta] = useState(sinMovimiento)
  // La lista completa de logros o de barrios, y el botón que la abrió para devolverle el foco.
  const [hoja, setHoja] = useState<TipoDeHoja | null>(null)
  const quienAbrio = useRef<HTMLElement | null>(null)
  function abrirHoja(tipo: TipoDeHoja, boton: HTMLElement) {
    quienAbrio.current = boton
    setHoja(tipo)
  }
  useEffect(() => {
    if (hoja) return
    quienAbrio.current?.focus({ preventScroll: true })
    quienAbrio.current = null
  }, [hoja])
  // Esc cierra primero la lista, y recién con la lista cerrada, el resumen: por eso va en la
  // fase de captura y corta el aviso al otro oyente.
  useEffect(() => {
    if (!hoja) return
    function alTeclear(e: globalThis.KeyboardEvent) {
      if (e.key !== "Escape") return
      e.stopImmediatePropagation()
      setHoja(null)
    }
    window.addEventListener("keydown", alTeclear, true)
    return () => window.removeEventListener("keydown", alTeclear, true)
  }, [hoja])

  const irA = useCallback(
    (indice: number) => {
      const el = pista.current
      if (!el) return
      const i = Math.min(Math.max(indice, 0), tarjetas.length - 1)
      el.scrollTo({ left: i * el.clientWidth, behavior: quieta ? 'auto' : 'smooth' })
    },
    [tarjetas.length, quieta],
  )

  // Foco inicial en las tarjetas: las flechas del teclado andan sin tocar nada.
  useEffect(() => pista.current?.focus({ preventScroll: true }), [])

  function alDeslizar() {
    const el = pista.current
    if (el) setActual(Math.round(el.scrollLeft / el.clientWidth))
  }

  function alTeclear(e: KeyboardEvent) {
    if (e.key === 'ArrowRight') irA(actual + 1)
    else if (e.key === 'ArrowLeft') irA(actual - 1)
  }

  async function compartir() {
    if (!user) return
    setArmando(true)
    setAviso(null)
    try {
      const imagen = await armarTarjetaDeResumen(resumen, user.username)
      setAviso(await compartirImagen(imagen, `burgometro-${resumen.mes}.png`))
    } catch {
      setAviso('No pudimos armar la imagen. Probá de nuevo.')
    } finally {
      setArmando(false)
    }
  }

  const escena = { '--nitidez': NITIDEZ[tarjetas[actual] ?? 'portada'] } as CSSProperties
  const ultima = actual === tarjetas.length - 1

  return (
    <div className={`tm ${quieta ? 'tm-quieta' : ''}`} style={escena}>
      {/* El fondo de marca: brasa y mostaza, con rayos que se van prendiendo de una tarjeta a la otra. */}
      <div className="tm-marca" aria-hidden="true">
        <div className="tm-rayos">
          <div className="tm-giro" />
        </div>
      </div>
      <div className="tm-velo" aria-hidden="true" />

      <header className="tm-arriba" inert={hoja !== null}>
        <div className="tm-pasos" aria-hidden="true">
          {tarjetas.map((t, i) => (
            <b key={t} className={i <= actual ? 'hecho' : ''} />
          ))}
        </div>
        <button type="button" className="tm-cerrar" onClick={alCerrar} aria-label="Cerrar y volver al perfil">
          <IconArrowLeft size={20} className="text-[#faf3e3]" />
        </button>
      </header>

      <div
        ref={pista}
        role="region"
        aria-roledescription="carrusel"
        aria-label={`Tu ${nombreDelMes(resumen.mes)} en Burgómetro`}
        tabIndex={0}
        onKeyDown={alTeclear}
        onScroll={alDeslizar}
        className="tm-pista"
        inert={hoja !== null}
      >
        {tarjetas.map((tipo, i) => (
          <section
            key={tipo}
            role="group"
            aria-roledescription="tarjeta"
            aria-label={ETIQUETAS[tipo]}
            // Las que no se ven no entran en el orden de tabulación ni las lee el lector de pantalla.
            inert={i !== actual}
            className={`tm-tarjeta ${i === actual ? 'on' : ''} ${tipo === 'mejor' ? 'tm-mejor' : ''} ${tipo === 'cierre' ? 'fin' : ''}`}
          >
            <Contenido tipo={tipo} resumen={resumen} activa={i === actual} abrir={abrirHoja} username={user?.username ?? ''} hamburguesa={user?.hamburguesa ?? null} xp={resumen.xp} />
          </section>
        ))}
      </div>

      <p role="status" className="tm-aviso" inert={hoja !== null}>{aviso}</p>
      <nav className="tm-abajo" aria-label="Cambiar de tarjeta" inert={hoja !== null}>
        <button type="button" onClick={() => irA(actual - 1)} aria-disabled={actual === 0}>
          <IconChevronRight size={20} className="rotate-180" />
          Anterior
        </button>
        {ultima ? (
          <button type="button" className="comp" onClick={compartir} disabled={armando}>
            <IconShare size={20} />
            {armando ? 'Armando…' : 'Compartir'}
          </button>
        ) : (
          <button type="button" className="sig" onClick={() => irA(actual + 1)}>
            Siguiente
            <IconChevronRight size={20} />
          </button>
        )}
        {!ultima && (
          <button type="button" className="comp" onClick={compartir} disabled={armando} aria-label="Compartir mi mes">
            <IconShare size={20} />
            Compartir
          </button>
        )}
      </nav>
      <p className="sr-only" aria-live="polite" inert={hoja !== null}>{actual + 1} de {tarjetas.length}</p>
      {hoja && <Hoja tipo={hoja} resumen={resumen} alCerrar={() => setHoja(null)} />}
    </div>
  )
}

/** Cómo se anuncia cada tarjeta: el "n de N" lo anuncia aparte la región en vivo. */
const ETIQUETAS: Record<TipoDeTarjeta, string> = {
  portada: "Portada",
  resenias: "Reseñas del mes",
  barrios: "Barrios",
  mejor: "Mejor nota",
  logros: "Logros del mes",
  podio: "Podio",
  mesActivo: "Mes más activo",
  dia: "Día favorito",
  reacciones: "Reacciones recibidas",
  nivel: "Tu nivel en el año",
  cierre: "Cierre",
}

/** Las líneas de un título, que suben de a una. */
function Lineas({ children, desde = 1, className }: { children: string[]; desde?: number; className?: string }) {
  return (
    <>
      {children.map((l, i) => (
        <span key={l} className={`tm-linea ${className ?? ''}`}>
          <span style={{ '--d': desde + i } as CSSProperties}>{l}</span>
        </span>
      ))}
    </>
  )
}

function Arco() {
  return (
    <svg className="tm-arco" viewBox="0 0 335 170" aria-hidden="true">
      <path d="M16 170A151.5 151.5 0 0 1 319 170" />
    </svg>
  )
}

function Contenido({ tipo, resumen, activa, abrir, username, hamburguesa, xp }: {
  tipo: TipoDeTarjeta
  resumen: TuMesDatos
  activa: boolean
  abrir: (tipo: TipoDeHoja, boton: HTMLElement) => void
  username: string
  hamburguesa: string | null
  xp: number
}) {
  const mes = periodoDe(resumen)
  switch (tipo) {
    case 'portada':
      return (
        <div className="tm-vidrio tm-portada">
          <Arco />
          <div className="tm-avatar">
            <AvatarDeUsuario username={username} hamburguesa={hamburguesa} xp={xp} size={140} />
          </div>
          <p className="tm-usuario tm-parte" style={{ '--d': 1 } as CSSProperties}>@{username}</p>
          <h1>
            <Lineas>{[`Tu ${mes}`]}</Lineas>
            <span className="tm-linea grande"><span style={{ '--d': 2 } as CSSProperties}>en Burgómetro</span></span>
          </h1>
          <div className="tm-sello" aria-hidden="true">
            {resumen.anual ? "AÑO" : mes.slice(0, 3).toUpperCase()}
            <br />
            {anioDelMes(resumen.mes)}
          </div>
        </div>
      )
    case 'resenias':
      return (
        <div className="tm-vidrio">
          <p className="tm-dec tm-parte" style={{ '--d': 0 } as CSSProperties}>Escribiste</p>
          <Cuenta
            valor={resumen.resenias}
            activa={activa}
            demora={500}
            etiqueta={`${resumen.resenias} ${resumen.resenias === 1 ? 'reseña' : 'reseñas'}`}
            className="tm-cifra tm-gran tm-parte"
          />
          <p className="tm-pie tm-parte" style={{ '--d': 3 } as CSSProperties}>
            {resumen.resenias === 1 ? "reseña" : "reseñas"} en {mes}
          </p>
          {comparacionCon(resumen) && (
            <p className="tm-comparacion tm-parte" style={{ '--d': 5 } as CSSProperties}>{comparacionCon(resumen)}</p>
          )}
        </div>
      )
    case 'barrios': {
      const primeros = resumen.barrios.slice(0, 3)
      return (
        <div className="tm-vidrio">
          <p className="tm-dec tm-parte" style={{ '--d': 0 } as CSSProperties}>
            {resumen.barriosEnTotal === 1 ? 'Recorriste un barrio' : `Recorriste ${resumen.barriosEnTotal} barrios`}
          </p>
          <ol className="tm-barrios">
            {primeros.map((b, i) => (
              <li key={b.nombre} style={{ '--d': i } as CSSProperties}>
                <b>{b.nombre}</b>
                <span className="tm-nota" role="img" aria-label={b.resenias === 1 ? '1 reseña' : `${b.resenias} reseñas`}>{b.resenias}</span>
              </li>
            ))}
          </ol>
          {resumen.barrios.length > primeros.length && (
            <button
              type="button"
              className="tm-vertodos tm-parte"
              style={{ '--d': 5 } as CSSProperties}
              onClick={(e) => abrir('barrios', e.currentTarget)}
            >
              Ver todos ({resumen.barriosEnTotal})
            </button>
          )}
        </div>
      )
    }
    case 'mejor': {
      const mejor = resumen.mejor
      if (!mejor) return null
      const fin = `${-90 + (180 * mejor.puntaje) / 5}deg`
      return (
        <>
          <div className="tm-cartel"><h2>Tu mejor nota</h2></div>
          <div className="tm-vidrio claro">
            <svg className="tm-medidor" viewBox="0 0 260 150" aria-hidden="true">
              <path d="M23.0 127.3A107 107 0 0 1 93.8 29.3" fill="none" stroke="#b23a2e" strokeWidth="24" />
              <path d="M100.1 27.2A107 107 0 0 1 190.1 41.5" fill="none" stroke="#f2b705" strokeWidth="24" />
              <path d="M195.6 45.5A107 107 0 0 1 237.0 127.3" fill="none" stroke="#2f4f23" strokeWidth="24" />
              <g stroke="#3a2a1e" strokeWidth="3" strokeLinecap="round">
                <line x1="7" y1="130" x2="0" y2="130" /><line x1="30.5" y1="57.7" x2="24.8" y2="53.6" />
                <line x1="92" y1="13" x2="89.8" y2="6.4" /><line x1="168" y1="13" x2="170.2" y2="6.4" />
                <line x1="229.5" y1="57.7" x2="235.2" y2="53.6" /><line x1="253" y1="130" x2="260" y2="130" />
              </g>
              <g className="tm-aguja" style={{ '--fin': fin } as CSSProperties}>
                <line x1="130" y1="130" x2="130" y2="40" stroke="#3a2a1e" strokeWidth="7" strokeLinecap="round" />
              </g>
              <circle cx="130" cy="130" r="11" fill="#3a2a1e" />
              <circle cx="130" cy="130" r="4" fill="#f2b705" />
            </svg>
            <Cuenta valor={mejor.puntaje} decimales={1} activa={activa} demora={700} etiqueta={`Nota ${nota(mejor.puntaje)}`} className="tm-cifra tm-parte" />
            <h2 className="tm-local tm-parte" style={{ '--d': 6 } as CSSProperties}>{mejor.nombre}</h2>
            {mejor.barrio && <p className="tm-barrio tm-parte" style={{ '--d': 7 } as CSSProperties}>{mejor.barrio}</p>}
            <Link to={rutaDelLocal({ id: mejor.localId, slug: mejor.slug })} className="tm-ver tm-parte" style={{ '--d': 8 } as CSSProperties}>
              Ver el local
            </Link>
          </div>
        </>
      )
    }
    case 'logros': {
      const ganados = resumen.logrosGanados
      const visibles = ganados.slice(0, 6)
      return (
        <div className="tm-vidrio claro">
          <p className="tm-dec tm-parte" style={{ '--d': 0 } as CSSProperties}>
            {ganados.length === 1 ? 'Ganaste un logro' : `Ganaste ${ganados.length} logros`}
          </p>
          <ul className={`tm-logros ${visibles.length === 1 ? 'uno' : visibles.length === 2 ? 'dos' : ''}`}>
            {visibles.map((l, i) => (
              <li key={l.codigo} style={{ '--d': i } as CSSProperties}>
                <MedallaDeLogro codigo={l.codigo} ganado size={visibles.length <= 2 ? 88 : 68} />
                <span className="tm-nombre">{nombreDe(l.codigo, l.detalle)}</span>
              </li>
            ))}
          </ul>
          {ganados.length > visibles.length && (
            <button
              type="button"
              className="tm-vertodos tm-parte"
              style={{ '--d': 6 } as CSSProperties}
              onClick={(e) => abrir('logros', e.currentTarget)}
            >
              Ver todos ({ganados.length})
            </button>
          )}
        </div>
      )
    }
    case 'podio': {
      // En el recuento del año, los mejores puestos primero; en un mes, en orden de fechas.
      const ordenados = resumen.anual
        ? [...resumen.podios].sort((a, b) => a.puesto - b.puesto || a.desde.localeCompare(b.desde))
        : resumen.podios
      const filas = ordenados.slice(0, 4)
      return (
        <div className="tm-vidrio">
          <p className="tm-dec tm-parte" style={{ '--d': 0 } as CSSProperties}>
            {resumen.podios.length === 1 ? 'Estuviste en el podio' : `Estuviste ${resumen.podios.length} veces en el podio`}
          </p>
          <ol className="tm-barrios">
            {filas.map((p, i) => (
              <li key={`${p.periodo}-${p.desde}`} style={{ '--d': i } as CSSProperties}>
                <b>{p.periodo === 'mes' ? `Todo ${nombreDelMes(p.desde.slice(0, 7))}` : `Semana del ${diaDe(p.desde)}`}</b>
                <span className={`tm-nota puesto-${Math.min(p.puesto, 4)}`} role="img" aria-label={`Puesto ${p.puesto}`}>{p.puesto}º</span>
              </li>
            ))}
          </ol>
          {resumen.podios.length > filas.length && (
            <p className="tm-pie tm-parte" style={{ '--d': 6, marginTop: 12 } as CSSProperties}>+{resumen.podios.length - filas.length} más</p>
          )}
        </div>
      )
    }
    case 'mesActivo': {
      const r = resumen.recuento
      if (!r) return null
      return (
        <div className="tm-vidrio">
          <p className="tm-dec tm-parte" style={{ '--d': 0 } as CSSProperties}>Tu mes más activo fue</p>
          <p className="tm-cifra tm-mes tm-parte" style={{ '--d': 1 } as CSSProperties}>{nombreDelMesNumero(r.mesMasActivo)}</p>
          <p className="tm-pie tm-parte" style={{ '--d': 3 } as CSSProperties}>
            con {r.reseniasDelMes} {r.reseniasDelMes === 1 ? 'reseña' : 'reseñas'}
          </p>
        </div>
      )
    }
    case 'dia': {
      const d = resumen.dia
      if (!d) return null
      return (
        <div className="tm-vidrio">
          <p className="tm-dec tm-parte" style={{ '--d': 0 } as CSSProperties}>Tu día para reseñar</p>
          <p className="tm-cifra tm-mes tm-parte" style={{ '--d': 1 } as CSSProperties}>{nombreDelDia(d.diaDeLaSemana)}</p>
          <p className="tm-pie tm-parte" style={{ '--d': 3 } as CSSProperties}>
            {d.resenias} {d.resenias === 1 ? 'reseña' : 'reseñas'}, sobre todo {franjaEnPalabras(d.franja)}
          </p>
        </div>
      )
    }
    case 'reacciones': {
      const r = resumen.reacciones
      if (!r) return null
      const la = reaccion(r.masUsada)
      return (
        <div className="tm-vidrio claro">
          <p className="tm-dec tm-parte" style={{ '--d': 0 } as CSSProperties}>
            {r.total === 1 ? 'Tus reseñas recibieron una reacción' : `Tus reseñas recibieron ${cantidad(r.total)} reacciones`}
          </p>
          <div className="tm-reaccion tm-parte" style={{ '--d': 1 } as CSSProperties}>
            <DibujoDeReaccion tipo={r.masUsada} className="h-32 w-auto" />
          </div>
          <p className="tm-pie tm-parte" style={{ '--d': 3 } as CSSProperties}>
            {r.total === 1 ? "Fue" : "La favorita"}: {la.nombre.toLowerCase()}
          </p>
        </div>
      )
    }
    case 'nivel': {
      const r = resumen.recuento
      if (!r) return null
      const subio = r.nivelAlTerminar > r.nivelAlEmpezar
      return (
        <div className="tm-vidrio">
          <p className="tm-dec tm-parte" style={{ '--d': 0 } as CSSProperties}>
            {subio ? `Del nivel ${r.nivelAlEmpezar} al` : 'Terminaste el año en el nivel'}
          </p>
          <p className="tm-cifra tm-gran tm-parte" style={{ '--d': 1 } as CSSProperties}>
            <span className="sr-only">Nivel </span>
            {r.nivelAlTerminar}
          </p>
          <p className="tm-pie tm-parte" style={{ '--d': 3 } as CSSProperties}>
            {subio ? `Subiste ${r.nivelAlTerminar - r.nivelAlEmpezar} ${r.nivelAlTerminar - r.nivelAlEmpezar === 1 ? 'nivel' : 'niveles'}` : 'Seguís firme'}
          </p>
        </div>
      )
    }
    case 'cierre':
      return (
        <div className="tm-vidrio tm-cierre">
          <Arco />
          <div className="tm-burgui">
            <Personaje pose="celu" className="h-40 w-auto" />
          </div>
          <h1>
            <Lineas desde={3}>{[`Listo, tu ${mes}.`]}</Lineas>
          </h1>
          <p className="tm-parte" style={{ '--d': 5 } as CSSProperties}>
            {resumen.logros > 0
              ? `Ganaste ${resumen.logros === 1 ? "un logro" : `${resumen.logros} logros`} ${resumen.anual ? "este año" : "este mes"}.`
              : 'Mostrales a tus amigos cómo te fue.'}
          </p>
          <p className="tm-nivel tm-parte" style={{ '--d': 6 } as CSSProperties}>Hoy sos nivel {resumen.nivel}</p>
        </div>
      )
  }
}
