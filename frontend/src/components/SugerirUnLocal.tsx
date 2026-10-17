import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'
import { useAuth } from '../context/useAuth'
import { isSessionExpired } from '../utils/errors'
import { Campo } from './Campo'
import { IconCamera } from './icons'

/** En la dirección, para que al volver de iniciar sesión el formulario se abra solo. */
const PARAMETRO = 'sugerir'

/** Lo que acepta el servidor. El WEBP queda afuera: el JDK no lo sabe leer. */
const FOTOS_ACEPTADAS = 'image/jpeg,image/png'

/** El mismo tope que el servidor (#283), para avisar acá y no después de subirla. */
const MAXIMO_MB_DE_FOTO = 8

/** El mínimo que exige el servidor a estas fotos. No se pide recortarla: se adapta allá. */
const LADO_MINIMO_DE_FOTO = 400

/** Cada botón que abre el formulario: el de búsqueda sin resultados y el de la tarjeta. */
const CLASE_DEL_BOTON = 'btn btn-primary h-11 min-h-11'

/**
 * "Sugerir un local" (#257): el botón y el formulario que abre.
 *
 * Si alguien busca una hamburguesería que no está, hasta ahora no tenía cómo avisar. Esto
 * guarda el dato para revisarlo a mano; no consulta a Google, así que no gasta nada.
 *
 * Pide sesión para frenar el spam sin captcha. Sin sesión, el botón lleva a iniciar
 * sesión y vuelve a esta misma pantalla con el formulario abierto.
 */
export function BotonSugerirUnLocal() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [abierto, setAbierto] = useState(false)
  // Se cambia cada vez que se abre, así el formulario arranca de cero.
  const [vez, setVez] = useState(0)

  function abrir() {
    if (!user) {
      navigate('/login', { state: { volverA: conElFormulario(location) } })
      return
    }
    setVez((n) => n + 1)
    setAbierto(true)
  }

  return (
    <>
      <button type="button" onClick={abrir} className={CLASE_DEL_BOTON}>
        Sugerir un local
      </button>
      {abierto && user && (
        <ModalSugerirUnLocal
          key={vez}
          emailDeLaCuenta={user.email}
          onCerrar={() => setAbierto(false)}
          onSesionVencida={() => navigate('/login', { state: { volverA: conElFormulario(location) } })}
        />
      )}
    </>
  )
}

/** Esta misma pantalla, con la marca que abre el formulario al volver de iniciar sesión. */
function conElFormulario(location: { pathname: string; search: string }) {
  const parametros = new URLSearchParams(location.search)
  parametros.set(PARAMETRO, '1')
  return `${location.pathname}?${parametros}`
}

/**
 * El formulario al volver de iniciar sesión: está abierto mientras la marca siga en la
 * dirección, y cerrarlo la saca, así no se vuelve a abrir al ir atrás. Va una sola vez
 * por pantalla: si hay dos botones a la vista, se abriría dos veces.
 */
export function AbrirSugerenciaAlVolver() {
  const { user } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  if (!user || !new URLSearchParams(location.search).has(PARAMETRO)) return null

  function cerrar() {
    const parametros = new URLSearchParams(location.search)
    parametros.delete(PARAMETRO)
    const resto = parametros.toString()
    navigate(`${location.pathname}${resto ? `?${resto}` : ''}`, { replace: true })
  }

  return (
    <ModalSugerirUnLocal
      emailDeLaCuenta={user.email}
      onCerrar={cerrar}
      onSesionVencida={() => navigate('/login', { state: { volverA: conElFormulario(location) } })}
    />
  )
}

/**
 * El formulario, en la misma ventana que el de reseñar: pegada abajo en el teléfono y
 * centrada en pantalla grande. Es un `<dialog>` por lo mismo que aquel: el navegador
 * atrapa el foco, cierra con Esc y deja inerte lo de atrás.
 */
function ModalSugerirUnLocal({
  emailDeLaCuenta,
  onCerrar,
  onSesionVencida,
}: {
  emailDeLaCuenta: string
  onCerrar: () => void
  onSesionVencida: () => void
}) {
  const dialogo = useRef<HTMLDialogElement>(null)
  const [nombre, setNombre] = useState('')
  const [direccion, setDireccion] = useState('')
  // Con la del de la cuenta, que casi siempre es la que sirve; se puede cambiar.
  const [email, setEmail] = useState(emailDeLaCuenta)
  const [comentario, setComentario] = useState('')
  const [foto, setFoto] = useState<File | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [enviada, setEnviada] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const idDelTitulo = useId()
  const idDelComentario = useId()
  const listo = useRef<HTMLButtonElement>(null)

  // Se monta solo cuando hay que mostrarla, así que se abre al montarse.
  useEffect(() => {
    const ventana = dialogo.current
    if (ventana && !ventana.open) ventana.showModal()
  }, [])

  // Al enviar, el formulario se va con el foco adentro: sin esto, el teclado y el
  // lector de pantalla quedaban sin saber dónde estaban.
  useEffect(() => {
    if (enviada) listo.current?.focus()
  }, [enviada])

  const hayAlgoEscrito = !enviada && (nombre.trim() !== '' || direccion.trim() !== '' || comentario.trim() !== '' || foto !== null)

  function intentarCerrar() {
    if (enviando) return
    if (hayAlgoEscrito && !confirm('¿Cerrar sin enviar? Vas a perder lo que escribiste.')) return
    onCerrar()
  }

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      // En multipart porque puede llevar una foto (#283).
      const cuerpo = new FormData()
      cuerpo.append('nombre', nombre.trim())
      cuerpo.append('direccion', direccion.trim())
      cuerpo.append('email', email.trim())
      if (comentario.trim()) cuerpo.append('comentario', comentario.trim())
      if (foto) cuerpo.append('foto', foto)
      await apiClient.post('/sugerencias', cuerpo)
      setEnviada(true)
    } catch (err: any) {
      if (isSessionExpired(err)) {
        onSesionVencida()
      } else if (
        ['RATE_LIMITED', 'EMAIL_NOT_VERIFIED'].includes(err.response?.data?.code) ||
        (foto && err.response?.status === 409)
      ) {
        // Un tope, la cuenta sin verificar o una foto que no sirve: el servidor dice qué
        // pasó y cuándo volver, o qué le pasa a la foto, y es lo único que permite arreglarlo.
        setError(err.response.data.error)
      } else {
        setError('No pudimos enviar tu sugerencia. Revisá los datos y probá de nuevo.')
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <dialog
      ref={dialogo}
      aria-labelledby={idDelTitulo}
      className="modal modal-bottom sm:modal-middle"
      onCancel={(e) => {
        e.preventDefault()
        intentarCerrar()
      }}
    >
      <div className="modal-box flex flex-col gap-4">
        <div className="flex items-start justify-between gap-3">
          <h2 id={idDelTitulo} className="font-display text-lg font-bold">
            Sugerir un local
          </h2>
          <button
            type="button"
            onClick={intentarCerrar}
            aria-label="Cerrar"
            className="btn btn-ghost btn-circle -mr-2 -mt-1 flex-none"
          >
            <span aria-hidden="true">✕</span>
          </button>
        </div>

        {enviada ? (
          <>
            <p role="status">Gracias. La vamos a revisar.</p>
            <button ref={listo} type="button" onClick={onCerrar} className={CLASE_DEL_BOTON}>
              Listo
            </button>
          </>
        ) : (
          <form onSubmit={enviar} className="flex flex-col gap-4">
            <Campo etiqueta="Nombre del local">
              {(campo) => (
                <input
                  {...campo}
                  value={nombre}
                  onChange={(e) => setNombre(e.target.value)}
                  required
                  maxLength={120}
                  autoComplete="off"
                  enterKeyHint="next"
                  className="input input-bordered w-full focus:border-primary"
                />
              )}
            </Campo>
            <Campo etiqueta="Dirección o link" ayuda="La calle y la altura, o su Instagram o Google Maps.">
              {(campo) => (
                <input
                  {...campo}
                  value={direccion}
                  onChange={(e) => setDireccion(e.target.value)}
                  required
                  maxLength={300}
                  autoComplete="off"
                  enterKeyHint="next"
                  className="input input-bordered w-full focus:border-primary"
                />
              )}
            </Campo>
            <Campo etiqueta="Tu email" ayuda="Por si necesitamos preguntarte algo.">
              {(campo) => (
                <input
                  {...campo}
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  maxLength={254}
                  autoComplete="email"
                  autoCapitalize="none"
                  autoCorrect="off"
                  spellCheck={false}
                  enterKeyHint="next"
                  className="input input-bordered w-full focus:border-primary"
                />
              )}
            </Campo>
            <div className="flex flex-col gap-1.5">
              <label htmlFor={idDelComentario} className="text-sm font-semibold">
                Comentario <span className="font-normal text-base-content/70">(opcional)</span>
              </label>
              <textarea
                id={idDelComentario}
                value={comentario}
                onChange={(e) => setComentario(e.target.value)}
                placeholder="¿Qué tiene de bueno?"
                maxLength={500}
                className="textarea textarea-bordered min-h-20 w-full focus:border-primary"
              />
            </div>

            <FotoDelLocal foto={foto} onCambio={setFoto} />

            {error && (
              <p role="alert" className="text-sm text-error">
                {error}
              </p>
            )}

            <button type="submit" disabled={enviando} className={CLASE_DEL_BOTON}>
              {enviando ? 'Enviando…' : 'Enviar sugerencia'}
            </button>
          </form>
        )}
      </div>

      <form method="dialog" className="modal-backdrop">
        <button type="button" tabIndex={-1} aria-hidden="true" onClick={intentarCerrar}>
          Cerrar
        </button>
      </form>
    </dialog>
  )
}

/**
 * La foto opcional del local (#283): una fila chica y no un recuadro, porque el
 * formulario ya es largo en un teléfono y esto no es lo que lo completa.
 *
 * La foto no se publica sola: la revisa Juan al agregar el local. Por eso el aviso dice de
 * quién tiene que ser. Se mide en el navegador, como en las reseñas, para decir "muy
 * chica" apenas se elige y no después de mandar todo.
 */
function FotoDelLocal({ foto, onCambio }: { foto: File | null; onCambio: (foto: File | null) => void }) {
  const [vista, setVista] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const idDelAviso = useId()
  // La vista previa ocupa memoria hasta que se la suelta: se revoca al cambiarla, al
  // sacarla y al cerrar.
  const abierta = useRef<string | null>(null)
  useEffect(() => () => {
    if (abierta.current) URL.revokeObjectURL(abierta.current)
  }, [])

  function mostrar(archivo: File | null, url: string | null) {
    if (abierta.current && abierta.current !== url) URL.revokeObjectURL(abierta.current)
    abierta.current = url
    setVista(url)
    onCambio(archivo)
  }

  function elegir(archivo: File | undefined) {
    setAviso(null)
    if (!archivo) return
    if (archivo.size > MAXIMO_MB_DE_FOTO * 1024 * 1024) {
      setAviso(`Esa foto pesa más de ${MAXIMO_MB_DE_FOTO} MB.`)
      return
    }
    const url = URL.createObjectURL(archivo)
    const prueba = new Image()
    prueba.onload = () => {
      if (Math.min(prueba.naturalWidth, prueba.naturalHeight) < LADO_MINIMO_DE_FOTO) {
        URL.revokeObjectURL(url)
        setAviso(`Esa foto es muy chica (${prueba.naturalWidth}×${prueba.naturalHeight}). Probá con una de ${LADO_MINIMO_DE_FOTO} px o más.`)
        return
      }
      mostrar(archivo, url)
    }
    // Si el navegador no la puede leer, la decisión queda para el servidor.
    prueba.onerror = () => mostrar(archivo, url)
    prueba.src = url
  }

  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-sm font-semibold">
        Foto del local <span className="font-normal text-base-content/70">(opcional)</span>
      </span>
      <div className="flex items-center gap-3">
        {/* El campo de archivo es invisible, así que el anillo de foco lo dibuja la etiqueta. */}
        <label className="btn btn-outline h-11 min-h-11 has-[input:focus-visible]:outline-2 has-[input:focus-visible]:outline-offset-2 has-[input:focus-visible]:outline-primary">
          <IconCamera size={20} />
          {foto ? 'Cambiar foto' : 'Agregar foto'}
          <input
            type="file"
            accept={FOTOS_ACEPTADAS}
            aria-describedby={idDelAviso}
            onChange={(e) => {
              elegir(e.target.files?.[0])
              // Vacío de nuevo, para que elegir otra vez la misma foto vuelva a avisar.
              e.target.value = ''
            }}
            className="sr-only"
          />
        </label>
        {foto && vista && (
          <>
            <img src={vista} alt="La foto que vas a mandar" className="size-14 flex-none rounded-field bg-base-200 object-cover" />
            <button type="button" onClick={() => mostrar(null, null)} className="btn btn-ghost h-11 min-h-11 px-3">
              Sacar
            </button>
          </>
        )}
      </div>
      <p id={idDelAviso} className="text-xs text-base-content/70">
        Subila como la tengas. Tiene que ser tuya o tener permiso del local.
      </p>
      {aviso && (
        <p role="alert" className="text-sm text-error">
          {aviso}
        </p>
      )}
    </div>
  )
}

/**
 * La tarjeta del final de la primera página de Explorar (#257): para quien recorrió todo
 * y no encontró la suya.
 */
export function TarjetaFaltaTuHamburgueseria() {
  return (
    <section
      aria-labelledby="falta-tu-hamburgueseria"
      className="tarjeta flex flex-col items-start gap-3 p-4 sm:flex-row sm:items-center sm:justify-between"
    >
      <div className="flex flex-col gap-0.5">
        <h2 id="falta-tu-hamburgueseria" className="font-display text-lg font-bold">
          ¿Falta tu hamburguesería?
        </h2>
        <p className="text-sm text-base-content/70">Pasanos el dato y la sumamos.</p>
      </div>
      <BotonSugerirUnLocal />
    </section>
  )
}
