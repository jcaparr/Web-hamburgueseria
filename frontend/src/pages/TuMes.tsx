import { useCallback, useEffect, useRef, useState, type KeyboardEvent, type MouseEvent, type ReactNode } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { apiClient } from '../api/client'
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
import { FONDOS, anioDelMes, nombreDelMes, tarjetasDe, type TipoDeTarjeta } from '../utils/tuMes'

/**
 * "Tu mes en Burgómetro" (#308): el resumen del mes en tarjetas a pantalla completa, como
 * las historias. Se pasa deslizando, tocando los costados, con los botones o con las
 * flechas del teclado. Una cifra por tarjeta.
 *
 * Los colores de cada tarjeta son fijos (ver FONDOS): no dependen del tema.
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
      .then((res) => vigente && setResumen(res.data))
      .catch((err) => {
        if (!vigente) return
        if (isSessionExpired(err)) navigate('/login')
        else setError(err)
      })
    return () => {
      vigente = false
    }
  }, [mesPedido, navigate, intento])

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
      <Pantalla fondo="#faf3e3" texto="#3a2a1e" alCerrar={cerrar}>
        <div className="mx-auto w-full max-w-[480px] p-4">
          <LoadError error={error} onRetry={() => { setError(null); setIntento((n) => n + 1) }} />
        </div>
      </Pantalla>
    )
  }
  if (!resumen) {
    return (
      <Pantalla fondo="#faf3e3" texto="#3a2a1e" alCerrar={cerrar}>
        <p role="status" className="p-8 text-center text-lg">Armando tu mes…</p>
      </Pantalla>
    )
  }
  if (resumen.resenias === 0) {
    return (
      <Pantalla fondo="#faf3e3" texto="#3a2a1e" alCerrar={cerrar}>
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

/** La capa que tapa la barra y la navegación: es una pantalla aparte, no una página más. */
function Pantalla({
  fondo,
  texto,
  alCerrar,
  centro,
  children,
}: {
  fondo: string
  texto: string
  alCerrar: () => void
  centro?: ReactNode
  children: ReactNode
}) {
  return (
    <div
      className="fixed inset-0 z-40 flex flex-col transition-colors duration-300 motion-reduce:transition-none"
      style={{ backgroundColor: fondo, color: texto }}
    >
      <header className="mx-auto flex w-full max-w-[480px] items-center gap-2 px-2 pt-[max(0.5rem,env(safe-area-inset-top))]">
        <button type="button" onClick={alCerrar} className="flex min-h-11 min-w-11 items-center justify-center gap-1.5 rounded-field px-2 font-semibold" aria-label="Cerrar y volver al perfil">
          <IconArrowLeft size={20} />
          <span className="text-sm">Cerrar</span>
        </button>
        <div className="flex-1">{centro}</div>
      </header>
      {children}
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

  const irA = useCallback(
    (indice: number) => {
      const el = pista.current
      if (!el) return
      const i = Math.min(Math.max(indice, 0), tarjetas.length - 1)
      // Sin animación si la persona pidió menos movimiento.
      const sinMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches
      el.scrollTo({ left: i * el.clientWidth, behavior: sinMovimiento ? 'auto' : 'smooth' })
    },
    [tarjetas.length],
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

  // Tocar un costado de la tarjeta pasa a la anterior o a la siguiente, salvo que lo
  // tocado sea un botón o un enlace.
  function alTocar(e: MouseEvent<HTMLElement>) {
    if ((e.target as HTMLElement).closest('a, button')) return
    // Soltar el mouse al terminar de seleccionar texto no es un toque.
    if (window.getSelection()?.toString()) return
    const caja = e.currentTarget.getBoundingClientRect()
    irA((e.clientX - caja.left) / caja.width < 0.35 ? actual - 1 : actual + 1)
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

  const { fondo, texto } = FONDOS[tarjetas[actual] ?? 'portada']

  return (
    <Pantalla
      fondo={fondo}
      texto={texto}
      alCerrar={alCerrar}
      centro={
        // Los puntitos son adorno: lo que se anuncia es "1 de 5".
        <div className="flex items-center justify-center gap-1.5" aria-hidden="true">
          {tarjetas.map((t, i) => (
            <span
              key={t}
              className="h-1.5 rounded-full transition-all motion-reduce:transition-none"
              style={{ backgroundColor: texto, width: i === actual ? 24 : 8, opacity: i === actual ? 1 : 0.6 }}
            />
          ))}
        </div>
      }
    >
      <div
        ref={pista}
        role="region"
        aria-roledescription="carrusel"
        aria-label={`Tu ${nombreDelMes(resumen.mes)} en Burgómetro`}
        tabIndex={0}
        onKeyDown={alTeclear}
        onScroll={alDeslizar}
        className="flex min-h-0 flex-1 snap-x snap-mandatory overflow-x-auto overscroll-x-contain scroll-smooth [scrollbar-width:none] motion-reduce:scroll-auto focus-visible:outline-none [&::-webkit-scrollbar]:hidden"
      >
        {tarjetas.map((tipo, i) => (
          <section
            key={tipo}
            role="group"
            aria-roledescription="tarjeta"
            aria-label={`${i + 1} de ${tarjetas.length}`}
            onClick={alTocar}
            className="flex w-full shrink-0 snap-center flex-col"
            // Las que no se ven no entran en el orden de tabulación ni las lee el lector de pantalla.
            inert={i !== actual}
            style={{ backgroundColor: FONDOS[tipo].fondo, color: FONDOS[tipo].texto }}
          >
            <div className="mx-auto flex w-full max-w-[480px] flex-1 flex-col justify-center gap-4 px-6 py-4">
              <Contenido tipo={tipo} resumen={resumen} compartir={compartir} armando={armando} aviso={aviso} />
            </div>
          </section>
        ))}
      </div>
      <nav
        aria-label="Cambiar de tarjeta"
        className="mx-auto flex w-full max-w-[480px] items-center justify-between px-2 pb-[max(0.5rem,env(safe-area-inset-bottom))]"
      >
        <button type="button" onClick={() => irA(actual - 1)} disabled={actual === 0} className="flex min-h-11 min-w-11 items-center gap-1 rounded-field px-3 font-semibold disabled:opacity-40">
          <IconChevronRight size={20} className="rotate-180" />
          Anterior
        </button>
        <p className="text-sm font-semibold" aria-live="polite">
          {actual + 1} de {tarjetas.length}
        </p>
        <button type="button" onClick={() => irA(actual + 1)} disabled={actual === tarjetas.length - 1} className="flex min-h-11 min-w-11 items-center gap-1 rounded-field px-3 font-semibold disabled:opacity-40">
          Siguiente
          <IconChevronRight size={20} />
        </button>
      </nav>
    </Pantalla>
  )
}

/** Un número grande, en la letra del logo. */
function Cifra({ children }: { children: ReactNode }) {
  return <p className="font-logo text-[clamp(5rem,32vw,9rem)] leading-none">{children}</p>
}

function Contenido({
  tipo,
  resumen,
  compartir,
  armando,
  aviso,
}: {
  tipo: TipoDeTarjeta
  resumen: TuMesDatos
  compartir: () => void
  armando: boolean
  aviso: string | null
}) {
  const mes = nombreDelMes(resumen.mes)
  switch (tipo) {
    case 'portada':
      return (
        <>
          <Personaje pose="mapa" className="h-40 w-auto self-start" />
          <h1 className="font-logo text-[clamp(2.5rem,12vw,3.75rem)] leading-tight [text-wrap:balance]">
            Tu {mes} en Burgómetro
          </h1>
          <p className="text-lg">Deslizá para ver cómo te fue.</p>
        </>
      )
    case 'resenias':
      return (
        <>
          <p className="text-lg font-semibold">En {mes} hiciste</p>
          <Cifra>{cantidad(resumen.resenias)}</Cifra>
          <p className="text-2xl font-semibold">{resumen.resenias === 1 ? 'reseña' : 'reseñas'}</p>
        </>
      )
    case 'barrios':
      return (
        <>
          <p className="text-lg font-semibold">
            {resumen.barriosEnTotal === 1 ? 'Comiste en un barrio' : `Comiste en ${resumen.barriosEnTotal} barrios`}
          </p>
          <ol className="flex flex-col gap-3">
            {resumen.barrios.map((b) => (
              <li key={b.nombre} className="line-clamp-2 break-words font-logo text-[clamp(1.75rem,9vw,2.5rem)] leading-tight">
                {b.nombre}
                <span className="ml-2 font-sans text-lg font-semibold">{b.resenias === 1 ? '1 reseña' : `${b.resenias} reseñas`}</span>
              </li>
            ))}
          </ol>
          {resumen.barriosEnTotal > resumen.barrios.length && (
            <p className="text-lg font-semibold">+{resumen.barriosEnTotal - resumen.barrios.length} más</p>
          )}
        </>
      )
    case 'mejor': {
      const mejor = resumen.mejor
      if (!mejor) return null
      return (
        <>
          <p className="text-lg font-semibold">Tu mejor nota de {mes}</p>
          <Cifra>{nota(mejor.puntaje)}</Cifra>
          <div className="flex items-center gap-4">
            {mejor.foto && (
              <img src={mejor.foto} alt="" className="size-20 shrink-0 rounded-box object-cover" loading="lazy" />
            )}
            <div className="min-w-0">
              <p className="line-clamp-3 break-words text-2xl font-bold leading-tight">{mejor.nombre}</p>
              {mejor.barrio && <p className="text-base">{mejor.barrio}</p>}
            </div>
          </div>
          <Link
            to={rutaDelLocal({ id: mejor.localId, slug: mejor.slug })}
            className="flex min-h-11 w-fit items-center font-semibold underline underline-offset-4"
          >
            Ver el local
          </Link>
        </>
      )
    }
    case 'anio':
      return resumen.anio ? (
        <>
          <p className="text-lg font-semibold">Y en todo {anioDelMes(resumen.mes)}</p>
          <Cifra>{cantidad(resumen.anio.resenias)}</Cifra>
          <p className="text-2xl font-semibold">
            {resumen.anio.resenias === 1 ? 'reseña' : 'reseñas'} en {cantidad(resumen.anio.locales)}{' '}
            {resumen.anio.locales === 1 ? 'local' : 'locales'}
          </p>
        </>
      ) : null
    case 'cierre':
      return (
        <>
          <Personaje pose="celu" className="h-36 w-auto self-start" />
          <p className="text-lg font-semibold">Hoy sos</p>
          <p className="font-logo text-[clamp(3rem,16vw,4.5rem)] leading-none">Nivel {resumen.nivel}</p>
          {resumen.logros > 0 && (
            <p className="text-lg font-semibold">
              Y ganaste {resumen.logros === 1 ? 'un logro' : `${resumen.logros} logros`} en {mes}.
            </p>
          )}
          <button type="button" onClick={compartir} disabled={armando} className="btn btn-primary min-h-11 w-fit gap-2">
            <IconShare size={16} />
            {armando ? 'Armando la imagen…' : 'Compartir mi mes'}
          </button>
          {aviso && <p role="status" className="text-sm font-semibold">{aviso}</p>}
        </>
      )
  }
}
