import { useEffect, useId, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { useAuth } from '../context/useAuth'
import { anioDelMes, nombreDelMes } from '../utils/tuMes'
import { IconBell } from './icons'
import { Personaje } from './Personaje'

interface AvisoDeTuMes {
  hayResumen: boolean
  mes: string
  anual: boolean
  visto: boolean
}

/** Cuánto espera, en total, a que se cierre otra ventana antes de dejar el aviso para la campana. */
const ESPERA_MAXIMA_MS = 15000

/** Cuánto se queda el cartel que apunta a la campana después de tocar "Después" (#418). */
const CARTEL_MS = 6000

/**
 * El pop up de "Tu mes en Burgómetro" (#405): al abrir la app con sesión, la primera vez que
 * hay un resumen sin ver, invita a verlo. Sale una sola vez por resumen y por persona: se
 * anota como visto en el servidor apenas se muestra (aunque se cierre con "Después", con Esc o
 * se cierre la pestaña), así que no vuelve en otro dispositivo ni al volver a entrar. El
 * aviso sigue en la campana para abrirlo cuando quiera.
 *
 * No se abre encima de otra ventana (la de reseñar, el aviso de un logro): espera a que se
 * cierre, como hace AvisoDeLogro. Y tampoco estando ya en el resumen.
 */
export function PopupDeTuMes() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  // El aviso lleva de quién es: al cambiar de cuenta no se hereda el de la anterior.
  const [pendiente, setPendiente] = useState<{ para: number; aviso: AvisoDeTuMes } | null>(null)
  const dialogo = useRef<HTMLDialogElement>(null)
  const botonDeVerlo = useRef<HTMLButtonElement>(null)
  // Cartel de "queda en notificaciones": solo al elegir "Después", no al ir a verlo.
  const [cartel, setCartel] = useState(false)
  const idDelTitulo = useId()
  // A quién ya se le preguntó: una vez por sesión de la persona, no en cada pantalla.
  const preguntado = useRef<number | null>(null)
  const rutaActual = useRef(pathname)
  useEffect(() => {
    rutaActual.current = pathname
  }, [pathname])
  const userId = user?.userId ?? null
  const aviso = pendiente && pendiente.para === userId ? pendiente.aviso : null

  useEffect(() => {
    if (userId === null) {
      // Se perdió la sesión: si el pop up estaba abierto, no queda una ventana vacía tapando todo.
      dialogo.current?.close()
      preguntado.current = null
      return
    }
    if (preguntado.current === userId) return
    let vigente = true
    let espera: number | undefined
    apiClient
      .get<AvisoDeTuMes>('/profile/tu-mes/aviso')
      .then(({ data }) => {
        if (!vigente) return
        // Se anota acá y no antes: con StrictMode el efecto corre dos veces y la primera respuesta se descarta.
        preguntado.current = userId
        if (!data.hayResumen || data.visto) return
        const desde = Date.now()
        const intentar = () => {
          if (!vigente) return
          // Otra ventana abierta, o ya en el resumen: se espera, y si no se libera, queda en la campana.
          if (document.querySelector('dialog[open]') || rutaActual.current === '/tu-mes') {
            if (rutaActual.current === '/tu-mes' || Date.now() - desde > ESPERA_MAXIMA_MS) return
            espera = window.setTimeout(intentar, 400)
            return
          }
          setPendiente({ para: userId, aviso: data })
        }
        intentar()
      })
      .catch(() => {
        // Sin pop up no pasa nada: el aviso está en la campana.
      })
    return () => {
      vigente = false
      window.clearTimeout(espera)
    }
  }, [userId])

  // Al tener algo para mostrar se abre, y se anota como visto en ese momento.
  useEffect(() => {
    const ventana = dialogo.current
    if (!ventana || !aviso) return
    if (!ventana.open) {
      ventana.showModal()
      botonDeVerlo.current?.focus()
      apiClient.post('/profile/tu-mes/visto').catch(() => {})
    }
  }, [aviso])

  // El cartel se va solo. Se limpia el temporizador si se desmonta o si se pierde la sesión.
  useEffect(() => {
    if (!cartel) return
    const t = window.setTimeout(() => setCartel(false), CARTEL_MS)
    return () => window.clearTimeout(t)
  }, [cartel])

  function cerrar(ir: boolean) {
    dialogo.current?.close()
    setPendiente(null)
    if (ir) navigate('/tu-mes')
    else setCartel(true)
  }

  // El contenedor role="status" está siempre montado y solo cambia lo de adentro: así el lector de pantalla lo anuncia.
  const cartelDeLaCampana = (
    <div
      role="status"
      className="pointer-events-none fixed inset-x-0 bottom-0 z-50 flex justify-center px-4 pb-[max(1rem,env(safe-area-inset-bottom))]"
    >
      {cartel && userId !== null && (
        <p className="pointer-events-auto flex min-h-11 max-w-sm items-center gap-2 cartel-de-tu-mes rounded-box px-4 py-2 text-sm shadow-lg">
          <IconBell size={18} />
          Queda en notificaciones para volver a verlo.
        </p>
      )}
    </div>
  )

  // Sin sesión no se muestra nada (si se pierde con el aviso abierto, tampoco queda tapando).
  if (!aviso || userId === null) {
    return (
      <>
        <dialog ref={dialogo} />
        {cartelDeLaCampana}
      </>
    )
  }

  const periodo = aviso.anual ? String(anioDelMes(aviso.mes)) : nombreDelMes(aviso.mes)
  return (
    <>
    <dialog
      ref={dialogo}
      aria-labelledby={idDelTitulo}
      className="modal"
      // Esc es "Después"; el navegador cierra la ventana solo y acá se acomoda el estado.
      onCancel={(e) => {
        e.preventDefault()
        cerrar(false)
      }}
    >
      <div className="modal-box flex max-w-sm flex-col items-center gap-4 text-center">
        <Personaje pose="mapa" className="h-28 w-auto" />
        <div className="flex flex-col gap-2">
          <h2 id={idDelTitulo} className="font-display text-xl font-bold">
            Tu {periodo} en Burgómetro ya está
          </h2>
          <p className="text-base-content/80">
            {aviso.anual
              ? 'Mirá cómo te fue en todo el año: tus reseñas, tus barrios, tu mejor nota y más.'
              : 'Mirá cómo te fue: tus reseñas, tus barrios, tu mejor nota y más.'}
          </p>
        </div>
        <div className="flex w-full flex-col gap-2">
          <button type="button" ref={botonDeVerlo} onClick={() => cerrar(true)} className="btn btn-primary min-h-12">
            Verlo
          </button>
          <button type="button" onClick={() => cerrar(false)} className="btn btn-ghost min-h-12">
            Después
          </button>
        </div>
        <p className="text-xs text-base-content/70">Lo vas a encontrar siempre en las notificaciones.</p>
      </div>
      {/* El clic afuera es "Después". */}
      <form method="dialog" className="modal-backdrop">
        <button type="button" tabIndex={-1} aria-hidden="true" onClick={() => cerrar(false)}>
          Cerrar
        </button>
      </form>
    </dialog>
    {cartelDeLaCampana}
    </>
  )
}
