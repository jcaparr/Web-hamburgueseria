import { useEffect, useRef, useState } from 'react'
import { IconCamera } from './icons'

/** Lo mismo que acepta el servidor. El WEBP queda afuera: el JDK no lo sabe leer. */
const ACEPTADOS = 'image/jpeg,image/png,image/gif'

/** El mismo tope que el servidor, para avisar acá y no después de subir 12 MB. */
const MAXIMO_MB = 12

/**
 * El mismo mínimo que exige el servidor.
 *
 * Se chequea también acá porque el navegador ya sabe las medidas apenas elige el
 * archivo: enterarse al tocar "publicar", después de escribir toda la reseña, sería
 * hacerle perder el texto por algo que se sabía desde el principio.
 */
const LADO_MINIMO = 600

/** Las mismas que acepta el servidor por reseña (#185). */
export const MAXIMO_DE_FOTOS = 4

/** Una foto en el selector: una que ya estaba guardada, o una recién elegida. */
type Foto = { clave: string; url: string; archivo?: File }

/** Lo que se va a mandar: cuáles de las guardadas se quedan, y cuáles se suman. */
export interface FotosElegidas {
  quedan: string[]
  nuevas: File[]
}

/**
 * Las fotos de la reseña: la primera es obligatoria y se pueden sumar hasta cuatro.
 *
 * La primera ocupa el ancho entero —un recuadro grande y no un botoncito— porque es
 * la portada, la que sale en el feed, y sacarla es parte de reseñar, no un extra. Las
 * demás van debajo, más chicas, con el lugar para sumar otra al final.
 *
 * Muestra lo elegido antes de mandarlo: una foto que se sube a ciegas es una foto que
 * se sube al revés o de otra cosa, y recién se nota cuando ya está publicada.
 *
 * Las guardadas van siempre antes que las nuevas, que es el orden en que las guarda el
 * servidor: lo que se ve acá es lo que va a quedar.
 */
export function SelectorDeFotos({
  yaSubidas,
  onCambio,
}: {
  /** Las que ya están guardadas en la reseña, en orden. */
  yaSubidas: string[]
  onCambio: (elegidas: FotosElegidas) => void
}) {
  // Arranca con las guardadas cada vez que se abre la ventana de reseñar, porque la
  // ventana se vuelve a armar de cero.
  const [fotos, setFotos] = useState<Foto[]>(() => yaSubidas.map((url) => ({ clave: url, url })))
  const [aviso, setAviso] = useState<string | null>(null)
  const [midiendo, setMidiendo] = useState(false)

  // Cada vista previa ocupa memoria hasta que se la suelta: se revocan al sacar una
  // foto y, las que queden, al cerrar.
  const vistas = useRef(new Set<string>())
  useEffect(() => {
    const abiertas = vistas.current
    return () => abiertas.forEach((url) => URL.revokeObjectURL(url))
  }, [])

  function cambiar(nuevas: Foto[]) {
    setFotos(nuevas)
    onCambio({
      quedan: nuevas.filter((f) => !f.archivo).map((f) => f.url),
      nuevas: nuevas.flatMap((f) => (f.archivo ? [f.archivo] : [])),
    })
  }

  async function sumar(elegidos: File[]) {
    setAviso(null)
    if (elegidos.length === 0) return

    const lugar = MAXIMO_DE_FOTOS - fotos.length
    const entran = elegidos.slice(0, lugar)
    const problemas: string[] = []
    if (elegidos.length > lugar) {
      problemas.push(`Una reseña lleva hasta ${MAXIMO_DE_FOTOS} fotos: entraron ${entran.length}.`)
    }

    // Las medidas se leen cargando cada foto, así que se espera a tenerlas todas antes
    // de mostrarlas: una que aparece y a los segundos desaparece por chica confunde más
    // que esperar un momento.
    setMidiendo(true)
    const revisadas = await Promise.all(entran.map(revisar))
    setMidiendo(false)

    const buenas: Foto[] = []
    revisadas.forEach((r) => {
      if ('problema' in r) problemas.push(r.problema)
      else buenas.push(r)
    })
    if (problemas.length > 0) setAviso(problemas.join(' '))
    if (buenas.length > 0) cambiar([...fotos, ...buenas])
  }

  function sacar(clave: string) {
    setAviso(null)
    const sacada = fotos.find((f) => f.clave === clave)
    if (sacada?.archivo) {
      URL.revokeObjectURL(sacada.url)
      vistas.current.delete(sacada.url)
    }
    cambiar(fotos.filter((f) => f.clave !== clave))
  }

  /** Una foto elegida, ya medida: lista para mostrar, o el motivo por el que no sirve. */
  function revisar(archivo: File): Promise<Foto | { problema: string }> {
    const nombre = archivo.name ? `«${archivo.name}»` : 'Esa foto'
    if (archivo.size > MAXIMO_MB * 1024 * 1024) {
      return Promise.resolve({ problema: `${nombre} pesa más de ${MAXIMO_MB} MB.` })
    }

    const url = URL.createObjectURL(archivo)
    return new Promise((resolver) => {
      const prueba = new Image()
      prueba.onload = () => {
        if (Math.min(prueba.naturalWidth, prueba.naturalHeight) < LADO_MINIMO) {
          URL.revokeObjectURL(url)
          resolver({
            problema:
              `${nombre} es muy chica (${prueba.naturalWidth}×${prueba.naturalHeight}) y se ` +
              `vería borrosa: necesita al menos ${LADO_MINIMO} px de lado.`,
          })
          return
        }
        vistas.current.add(url)
        resolver({ clave: url, url, archivo })
      }
      // Si el navegador no la puede leer, la decisión queda para el servidor, que es el
      // que la va a reescribir: puede ser un formato que acá no se muestre y allá sí.
      prueba.onerror = () => {
        vistas.current.add(url)
        resolver({ clave: url, url, archivo })
      }
      prueba.src = url
    })
  }

  const [portada, ...resto] = fotos
  const hayLugar = fotos.length < MAXIMO_DE_FOTOS

  return (
    <div className="flex flex-col gap-2">
      {portada ? (
        <div className="relative overflow-hidden rounded-box">
          <img src={portada.url} alt="La portada de tu reseña" className="aspect-square w-full bg-base-200 object-cover" />
          {/* Solo si hay más de una: con una sola ya se sabe cuál sale en el feed. */}
          {fotos.length > 1 && (
            <span className="absolute bottom-2 left-2 rounded-field bg-black/65 px-2 py-1 text-xs font-semibold text-white">
              Portada
            </span>
          )}
          <BotonSacar onClick={() => sacar(portada.clave)} etiqueta="Sacar la portada" />
        </div>
      ) : (
        <ElegirFotos onElegir={sumar} grande />
      )}

      {/* Las demás, de a tres por fila, y el lugar para sumar otra mientras queden. */}
      {portada && (resto.length > 0 || hayLugar) && (
        <ul className="grid grid-cols-3 gap-2">
          {resto.map((foto, i) => (
            <li key={foto.clave} className="relative overflow-hidden rounded-field">
              <img src={foto.url} alt={`Foto ${i + 2} de tu reseña`} className="aspect-square w-full bg-base-200 object-cover" />
              <BotonSacar onClick={() => sacar(foto.clave)} etiqueta={`Sacar la foto ${i + 2}`} />
            </li>
          ))}
          {hayLugar && (
            <li>
              <ElegirFotos onElegir={sumar} restan={MAXIMO_DE_FOTOS - fotos.length} />
            </li>
          )}
        </ul>
      )}

      {midiendo && (
        <p role="status" className="text-sm text-base-content/70">
          Revisando las fotos…
        </p>
      )}
      {aviso && (
        <p role="alert" className="text-sm text-error">
          {aviso}
        </p>
      )}
    </div>
  )
}

/**
 * El lugar para elegir fotos: grande y vacío cuando todavía no hay ninguna, y chico, en
 * la fila de abajo, para sumar otra.
 */
function ElegirFotos({
  onElegir,
  grande = false,
  restan,
}: {
  onElegir: (archivos: File[]) => void
  grande?: boolean
  restan?: number
}) {
  return (
    // El campo de archivo es invisible, así que el anillo de foco lo dibuja la
    // etiqueta: sin esto, con el teclado no se veía dónde se estaba.
    <label
      className={`group relative flex aspect-square w-full cursor-pointer flex-col items-center justify-center bg-base-200 text-base-content/70 ring-1 ring-inset ring-base-content/10 transition-colors hover:bg-base-300 has-[input:focus-visible]:outline-2 has-[input:focus-visible]:outline-offset-2 has-[input:focus-visible]:outline-primary ${
        grande ? 'gap-2 rounded-box' : 'gap-1 rounded-field'
      }`}
    >
      <IconCamera size={grande ? 34 : 22} />
      {grande ? (
        <>
          <span className="text-sm font-semibold">Agregá una foto</span>
          <span className="text-xs">Sin foto no se publica. Podés subir hasta {MAXIMO_DE_FOTOS}.</span>
        </>
      ) : (
        <>
          <span className="text-xs font-semibold">Sumar otra</span>
          <span className="text-xs">{restan === 1 ? 'Queda 1' : `Quedan ${restan}`}</span>
        </>
      )}
      <input
        type="file"
        accept={ACEPTADOS}
        multiple
        onChange={(e) => {
          onElegir(Array.from(e.target.files ?? []))
          // Vacío de nuevo, para que elegir otra vez la misma foto después de sacarla
          // vuelva a avisar: si no, el navegador no ve ningún cambio.
          e.target.value = ''
        }}
        className="sr-only"
      />
    </label>
  )
}

function BotonSacar({ onClick, etiqueta }: { onClick: () => void; etiqueta: string }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={etiqueta}
      className="btn btn-circle btn-sm absolute right-1.5 top-1.5 border-0 bg-black/60 text-white hover:bg-black/80"
    >
      <span aria-hidden="true">✕</span>
    </button>
  )
}
