/**
 * La foto de una hamburguesería, o un reemplazo cuando no tiene.
 *
 * Antes el reemplazo era una imagen de placehold.co. Eso significaba pedirle una
 * imagen a un tercero en cada visita, con dos problemas: le entregaba la IP de
 * nuestros usuarios a un servicio ajeno, y la política de seguridad del servidor
 * —que solo permite imágenes propias y de Google— la bloqueaba, así que en
 * producción se habrían visto todas rotas.
 *
 * Este se dibuja en el navegador con lo que ya está en la página. No hay pedido que
 * hacer, así que no hay nada que bloquear ni nadie a quien informarle una IP.
 */

/**
 * Tonos del tema, no colores fijos: así el reemplazo acompaña si el tema cambia.
 * Queda afuera primary, que en esta app está reservado para los botones de acción.
 */
const TONOS = [
  'bg-neutral text-neutral-content',
  'bg-accent text-accent-content',
  'bg-secondary text-secondary-content',
  'bg-base-300 text-base-content',
] as const

/**
 * El mismo local siempre recibe el mismo tono. Importa que sea estable: si cambiara
 * entre recargas, la lista parpadearía de colores cada vez que se entra.
 */
function tonoPara(nombre: string) {
  let acumulado = 0
  for (let i = 0; i < nombre.length; i++) {
    acumulado = (acumulado + nombre.charCodeAt(i)) % TONOS.length
  }
  return TONOS[acumulado]
}

/**
 * Hasta dos iniciales. Se saltean los símbolos sueltos —hay nombres como
 * "Empanadas | Hamburguesas" o "BURGER&CO"— para no terminar mostrando "E|".
 */
function inicialesDe(nombre: string) {
  const palabras = nombre
    .split(/[\s|·-]+/)
    .map((palabra) => palabra.replace(/[^\p{L}\p{N}]/gu, ''))
    .filter(Boolean)

  const iniciales = palabras.slice(0, 2).map((palabra) => palabra[0]).join('')
  return (iniciales || '?').toUpperCase()
}

interface Props {
  /** La ruta de la foto guardada, o null si el local no tiene. */
  src?: string | null
  name: string
  className?: string
  /**
   * Lo que dice la foto para un lector de pantalla. Vacío por omisión: en toda la app
   * la foto va al lado del nombre del local, y repetirlo hacía que cada tarjeta se
   * leyera "Burger Joint, Burger Joint".
   */
  alt?: string
  /** Para la foto que se ve apenas se entra, como la de la ficha: se pide primero. */
  prioritaria?: boolean
}

export function JointPhoto({ src, name, className, alt = '', prioritaria = false }: Props) {
  if (src) {
    // Las demás, perezosas: Explorar trae veinte por página y la mayoría queda fuera
    // de la pantalla hasta que se baja.
    return (
      <img
        src={src}
        alt={alt}
        loading={prioritaria ? 'eager' : 'lazy'}
        fetchPriority={prioritaria ? 'high' : undefined}
        decoding="async"
        className={className}
      />
    )
  }

  return (
    <div
      {...(alt ? { role: 'img', 'aria-label': alt } : { 'aria-hidden': true })}
      className={`flex items-center justify-center ${tonoPara(name)} ${className ?? ''}`}
    >
      {/*
        Las iniciales van en un SVG y no en texto suelto porque este mismo componente
        se usa en una portada grande y en miniaturas de 48 píxeles: el viewBox las
        escala solo, sin tener que pasarle un tamaño distinto en cada pantalla.
      */}
      <svg viewBox="0 0 100 100" className="h-1/2 w-1/2" aria-hidden="true">
        <text
          x="50"
          y="50"
          textAnchor="middle"
          dominantBaseline="central"
          fontSize="54"
          fontWeight="700"
          fill="currentColor"
        >
          {inicialesDe(name)}
        </text>
      </svg>
    </div>
  )
}
