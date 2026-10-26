import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import { useAuth } from '../context/useAuth'
import { EVENTO_REVISAR_LOGROS } from '../utils/avisosDeLogros'
import { AVISOS, DESBLOQUEOS, nombreDe } from '../utils/logros'
import { codigoDe, recetaPara } from '../utils/recetas'
import { AvatarDeUsuario } from './AvatarDeUsuario'
import { MedallaDeLogro } from './MedallaDeLogro'

interface Novedades {
  xp: number
  sinVer: string[]
  retroactivos: number
}

/**
 * El aviso al ganar un logro (#317): una ventana sobre cualquier pantalla.
 *
 * Se monta una vez en App. Pide las novedades cuando algo pudo dar un logro (una reseña, un
 * recorrido, una guardada o la hamburguesa, ver {@link esAccionQueDaLogros}) y cuando la
 * campana dice que hay logros sin ver. Si hay otra ventana abierta, como la de reseña, espera
 * a que se cierre. Con varios juntos es una sola ventana con "1 de N", primero el de nivel.
 *
 * Los de la carga inicial (lo que ya merecías al lanzarse los logros) van en un único aviso,
 * "Ya tenías N logros", sin animación y con enlace a Logros.
 *
 * Un logro se avisa una sola vez: se marca como visto apenas se muestra, y perderlo y volver
 * a ganarlo no lo repite (eso lo garantiza el servidor).
 */
export function AvisoDeLogro() {
  const { user } = useAuth()
  const [novedades, setNovedades] = useState<Novedades | null>(null)
  const [paso, setPaso] = useState(0)
  const dialogo = useRef<HTMLDialogElement>(null)
  const pidiendo = useRef(false)
  const espera = useRef<number | undefined>(undefined)

  const hayUsuario = !!user

  useEffect(() => {
    if (!hayUsuario) return
    async function pedir() {
      if (pidiendo.current) return
      pidiendo.current = true
      try {
        const { data } = await apiClient.get<Novedades>('/logros/novedades')
        if (data.sinVer.length > 0 || data.retroactivos > 0) mostrarCuandoSePueda(data)
      } catch {
        // Sin aviso no pasa nada: la próxima acción vuelve a preguntar.
      } finally {
        pidiendo.current = false
      }
    }
    function mostrarCuandoSePueda(data: Novedades) {
      window.clearTimeout(espera.current)
      // La ventana de reseña (u otra) abierta tapa todo: se espera a que se cierre.
      if (document.querySelector('dialog[open]')) {
        espera.current = window.setTimeout(() => mostrarCuandoSePueda(data), 200)
        return
      }
      setPaso(0)
      setNovedades(data)
    }
    window.addEventListener(EVENTO_REVISAR_LOGROS, pedir)
    return () => {
      window.removeEventListener(EVENTO_REVISAR_LOGROS, pedir)
      window.clearTimeout(espera.current)
    }
  }, [hayUsuario])

  // Los de nivel primero; después, en el orden en que se ganaron.
  const pasos = novedades ? ordenar(novedades) : []
  const actual = pasos[paso]

  // Se abre al tener algo para mostrar y se marca como visto después de dibujarlo: si el dibujo
  // fallara, no se perdería el aviso.
  useEffect(() => {
    const ventana = dialogo.current
    if (!ventana || !novedades) return
    if (!ventana.open) ventana.showModal()
    apiClient
      .post('/logros/vistos', { codigos: novedades.sinVer, retroactivos: novedades.retroactivos > 0 })
      .catch(() => {})
  }, [novedades])

  function cerrar() {
    dialogo.current?.close()
    setNovedades(null)
  }

  if (!user || !novedades || !actual) return <dialog ref={dialogo} />

  return (
    <dialog
      ref={dialogo}
      aria-labelledby="titulo-aviso-logro"
      onCancel={(e) => {
        e.preventDefault()
        cerrar()
      }}
      className="fixed inset-0 m-auto h-dvh max-h-none w-screen max-w-none bg-transparent p-0 backdrop:bg-[rgb(32_26_21/0.97)]"
    >
      <div className="flex h-full flex-col items-center justify-center gap-4 overflow-y-auto px-6 pb-8 pt-6 text-center text-[#f3e8cf]">
        {actual === 'retroactivos' ? (
          <Retroactivos cuantos={novedades.retroactivos} onCerrar={cerrar} />
        ) : (
          <Logro
            codigo={actual}
            usuario={user}
            xp={novedades.xp}
            posicion={paso + 1}
            total={pasos.length}
            esElUltimo={paso === pasos.length - 1}
            detalle={null}
            onSiguiente={() => setPaso(paso + 1)}
            onCerrar={cerrar}
          />
        )}
      </div>
    </dialog>
  )
}

/** Los códigos a mostrar, con el de los retroactivos al final como un solo paso. */
function ordenar(n: Novedades): string[] {
  const nivel = n.sinVer.filter((c) => c.startsWith('NIVEL_'))
  const resto = n.sinVer.filter((c) => !c.startsWith('NIVEL_'))
  return [...nivel, ...resto, ...(n.retroactivos > 0 ? ['retroactivos'] : [])]
}

function Logro({
  codigo,
  usuario,
  xp,
  posicion,
  total,
  esElUltimo,
  detalle,
  onSiguiente,
  onCerrar,
}: {
  codigo: string
  usuario: { username: string; hamburguesa: string | null }
  xp: number
  posicion: number
  total: number
  esElUltimo: boolean
  detalle: string | null
  onSiguiente: () => void
  onCerrar: () => void
}) {
  const desbloqueo = DESBLOQUEOS[codigo]
  return (
    <>
      <p className="text-sm font-bold uppercase tracking-[0.12em] text-[#f2b705]">
        Logro desbloqueado{total > 1 ? ` · ${posicion} de ${total}` : ''}
      </p>
      {desbloqueo ? (
        /* La capa nueva cae sobre tu hamburguesa de verdad. */
        <div className="cae-capa">
          <AvatarDeUsuario
            username={usuario.username}
            hamburguesa={codigoConPremio(usuario, desbloqueo.premio)}
            xp={xp}
            size={144}
          />
        </div>
      ) : (
        <div className="medalla-aviso">
          <MedallaDeLogro codigo={codigo} ganado size={128} />
        </div>
      )}
      <h2 id="titulo-aviso-logro" className="font-display text-[1.75rem] font-extrabold leading-tight text-[#fffdf8]">
        {nombreDe(codigo, detalle)}
      </h2>
      <p className="max-w-[28ch] text-base leading-snug">{AVISOS[codigo] ?? ''}</p>
      <div className="mt-2 flex w-full max-w-xs flex-col gap-2.5">
        {desbloqueo && (
          <Link to="/profile/hamburguesa" onClick={onCerrar} className="btn btn-primary w-full">
            {desbloqueo.boton}
          </Link>
        )}
        <button
          type="button"
          onClick={esElUltimo ? onCerrar : onSiguiente}
          autoFocus
          className={`btn w-full ${desbloqueo ? 'btn-outline border-[#f3e8cf]/50 text-[#f3e8cf]' : 'btn-primary'}`}
        >
          {esElUltimo ? 'Listo' : 'Siguiente'}
        </button>
      </div>
    </>
  )
}

/** La hamburguesa del usuario con el premio recién desbloqueado puesto, para mostrar la capa. */
function codigoConPremio(usuario: { username: string; hamburguesa: string | null }, premio: 'huevo' | 'bacon' | 'carnes'): string {
  const receta = recetaPara(usuario.username, usuario.hamburguesa)
  if (premio === 'carnes') return codigoDe({ ...receta, carnes: 4 })
  return codigoDe({ ...receta, [premio]: true })
}

/** El aviso único de los que ya merecías cuando se lanzaron los logros: sin animación, con enlace. */
function Retroactivos({ cuantos, onCerrar }: { cuantos: number; onCerrar: () => void }) {
  return (
    <>
      <h2 id="titulo-aviso-logro" className="font-display text-[1.75rem] font-extrabold leading-tight text-[#fffdf8]">
        Ya tenías {cuantos} {cuantos === 1 ? 'logro' : 'logros'}
      </h2>
      <p className="max-w-[28ch] text-base leading-snug">
        Llegaron los logros a Burgómetro y ya tenés los que te tocaban por todo lo que hiciste.
      </p>
      <div className="mt-2 flex w-full max-w-xs flex-col gap-2.5">
        <Link to="/logros" onClick={onCerrar} autoFocus className="btn btn-primary w-full">
          Ver mis logros
        </Link>
        <button type="button" onClick={onCerrar} className="btn btn-outline w-full border-[#f3e8cf]/50 text-[#f3e8cf]">
          Listo
        </button>
      </div>
    </>
  )
}
