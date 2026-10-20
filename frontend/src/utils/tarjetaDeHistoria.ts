import { nota } from './numeros'

/**
 * La imagen vertical que se comparte en las historias de Instagram (#278): la foto de la
 * reseña a pantalla completa, y abajo, sobre un velo oscuro, el barrio, el nombre del
 * local, la nota en el tablero del logo, lo que escribió la persona y quién es.
 *
 * Se dibuja en un canvas, en el teléfono: sin servidor y sin pedirle nada a Google.
 */

const ANCHO = 1080
const ALTO = 1920
const MARGEN = 64
/**
 * Instagram tapa arriba con la fila del usuario (hasta unos 140 px de una historia de
 * 1920) y abajo con «Enviar mensaje». En el celular de Juan ese campo queda debajo de la
 * imagen, así que la línea del usuario puede apoyar casi en el borde de abajo; la marca
 * va pegada bajo la fila del usuario.
 */
const MARCA_ARRIBA = 150
const BORDE_LIBRE_ABAJO = 110
const MAXIMO_DEL_COMENTARIO = 150

const TINTA = '#3a2a1e'
const MOSTAZA = '#f2b705'
const KETCHUP = '#b23a2e'
const FUENTE = '"Roboto", system-ui, "Segoe UI", sans-serif'

export interface DatosDeLaTarjeta {
  nombreDelLocal: string
  barrio: string | null
  puntaje: number
  comentario: string | null
  username: string
  /** La portada de la reseña, si tiene. */
  foto: string | null
}

function cargarImagen(src: string, conCors = false): Promise<HTMLImageElement> {
  return new Promise((resolver, rechazar) => {
    const img = new Image()
    // Sin CORS el canvas queda "manchado" y no se puede exportar; en producción las
    // fotos son del mismo origen y no cambia nada.
    if (conCors) img.crossOrigin = 'anonymous'
    img.onload = () => resolver(img)
    img.onerror = () => rechazar(new Error(`No cargó ${src}`))
    img.src = src
  })
}

/** El logo como imagen: con ancho y alto escritos, porque un SVG sin ellos no se escala igual en todos los navegadores. */
async function cargarLogo(): Promise<HTMLImageElement | null> {
  try {
    const texto = await (await fetch('/favicon.svg')).text()
    const conMedidas = texto.replace('<svg ', '<svg width="284" height="240" ')
    return await cargarImagen(`data:image/svg+xml;charset=utf-8,${encodeURIComponent(conMedidas)}`)
  } catch {
    return null
  }
}

/** Parte el texto en renglones que entren en el ancho; si sobran, el último termina en "…". */
function renglones(ctx: CanvasRenderingContext2D, texto: string, ancho: number, maximo: number): string[] {
  const lineas: string[] = []
  let actual = ''
  const palabras = texto.split(/\s+/).filter(Boolean)
  for (let i = 0; i < palabras.length; i++) {
    const prueba = actual ? `${actual} ${palabras[i]}` : palabras[i]
    if (ctx.measureText(prueba).width <= ancho || !actual) {
      actual = prueba
      continue
    }
    lineas.push(actual)
    actual = palabras[i]
    if (lineas.length === maximo) {
      actual = ''
      lineas[maximo - 1] = recortar(ctx, `${lineas[maximo - 1]} ${palabras.slice(i).join(' ')}`, ancho)
      return lineas
    }
  }
  if (actual) lineas.push(actual)
  if (lineas.length > maximo) lineas.length = maximo
  return lineas
}

/** El texto en un solo renglón, con "…" al final si no entra. */
function recortar(ctx: CanvasRenderingContext2D, texto: string, ancho: number): string {
  if (ctx.measureText(texto).width <= ancho) return texto
  let corte = texto.length
  while (corte > 1 && ctx.measureText(`${texto.slice(0, corte).trimEnd()}…`).width > ancho) corte--
  return `${texto.slice(0, corte).trimEnd()}…`
}

function dibujarFoto(ctx: CanvasRenderingContext2D, img: HTMLImageElement) {
  // "cover": llena toda la historia recortando lo que sobre, centrado.
  const escala = Math.max(ANCHO / img.width, ALTO / img.height)
  const w = img.width * escala
  const h = img.height * escala
  ctx.drawImage(img, (ANCHO - w) / 2, (ALTO - h) / 2, w, h)
}

function dibujarFondoSinFoto(ctx: CanvasRenderingContext2D, logo: HTMLImageElement | null) {
  const fondo = ctx.createRadialGradient(ANCHO / 2, ALTO * 0.38, 0, ANCHO / 2, ALTO * 0.38, ALTO * 0.7)
  fondo.addColorStop(0, '#f0b46a')
  fondo.addColorStop(0.45, '#b9703a')
  fondo.addColorStop(1, '#4b2a1a')
  ctx.fillStyle = fondo
  ctx.fillRect(0, 0, ANCHO, ALTO)
  if (logo) ctx.drawImage(logo, ANCHO * 0.2, ALTO * 0.3, ANCHO * 0.6, (ANCHO * 0.6 * 240) / 284)
}

function dibujarVelos(ctx: CanvasRenderingContext2D) {
  const velo = ctx.createLinearGradient(0, 0, 0, ALTO)
  // Con la marca más arriba, el velo se sostiene hasta debajo de ella: sobre una foto
  // blanca, el texto blanco da 5 : 1 (calculado con la fórmula de WCAG).
  velo.addColorStop(0, 'rgba(28,21,17,0.7)')
  velo.addColorStop(0.17, 'rgba(28,21,17,0.62)')
  velo.addColorStop(0.3, 'rgba(28,21,17,0)')
  // Más cerrado que el del diseño de muestra: sobre una foto clara (queso, pan) el
  // nombre en blanco se perdía a media altura.
  velo.addColorStop(0.48, 'rgba(28,21,17,0.62)')
  velo.addColorStop(0.7, 'rgba(28,21,17,0.86)')
  velo.addColorStop(1, 'rgba(28,21,17,0.94)')
  ctx.fillStyle = velo
  ctx.fillRect(0, 0, ANCHO, ALTO)
}

/** El tablero del sello: el arco gris, el arco lleno hasta la nota y la aguja de kétchup. */
function dibujarTablero(ctx: CanvasRenderingContext2D, x: number, y: number, ancho: number, fraccion: number) {
  const escala = ancho / 24
  const cx = x + 12 * escala
  const cy = y + 12 * escala
  const radio = 9 * escala
  ctx.lineCap = 'butt'
  ctx.lineWidth = 3.2 * escala
  ctx.strokeStyle = 'rgba(58,42,30,0.25)'
  ctx.beginPath()
  ctx.arc(cx, cy, radio, Math.PI, 2 * Math.PI)
  ctx.stroke()
  const f = Math.min(Math.max(fraccion, 0), 1)
  ctx.strokeStyle = TINTA
  ctx.beginPath()
  ctx.arc(cx, cy, radio, Math.PI, Math.PI + Math.PI * f)
  ctx.stroke()
  const angulo = Math.PI + Math.PI * f
  ctx.lineCap = 'round'
  ctx.lineWidth = 2.2 * escala
  ctx.strokeStyle = KETCHUP
  ctx.beginPath()
  ctx.moveTo(cx, cy)
  ctx.lineTo(cx + 7.4 * escala * Math.cos(angulo), cy + 7.4 * escala * Math.sin(angulo))
  ctx.stroke()
  ctx.fillStyle = TINTA
  ctx.beginPath()
  ctx.arc(cx, cy, 2 * escala, 0, 2 * Math.PI)
  ctx.fill()
}

function rectanguloRedondeado(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

export async function armarTarjetaDeHistoria(datos: DatosDeLaTarjeta): Promise<Blob> {
  // La tipografía de la web se carga perezosa: sin esto el canvas podía dibujar con la
  // de reemplazo la primera vez.
  await Promise.all([document.fonts.load(`700 60px ${FUENTE}`), document.fonts.load(`400 40px ${FUENTE}`)])

  const [logo, foto] = await Promise.all([
    cargarLogo(),
    datos.foto ? cargarImagen(datos.foto, true).catch(() => null) : Promise.resolve(null),
  ])

  const canvas = document.createElement('canvas')
  canvas.width = ANCHO
  canvas.height = ALTO
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('El navegador no puede dibujar la imagen')

  if (foto) dibujarFoto(ctx, foto)
  else dibujarFondoSinFoto(ctx, logo)
  dibujarVelos(ctx)

  ctx.textBaseline = 'alphabetic'
  ctx.textAlign = 'left'

  // La marca, arriba a la izquierda, justo debajo del encabezado de Instagram.
  if (logo) ctx.drawImage(logo, MARGEN, MARCA_ARRIBA, 118, 100)
  ctx.fillStyle = '#ffffff'
  ctx.font = `700 52px ${FUENTE}`
  ctx.fillText('Burgómetro', MARGEN + 118 + 20, MARCA_ARRIBA + 68)

  // El bloque de abajo se arma de abajo hacia arriba, desde el borde libre.
  const ancho = ANCHO - MARGEN * 2
  let y = ALTO - BORDE_LIBRE_ABAJO

  ctx.fillStyle = 'rgba(255,255,255,0.85)'
  ctx.font = `400 36px ${FUENTE}`
  ctx.fillText(recortar(ctx, `@${datos.username} en burgometro.com.ar`, ancho), MARGEN, y)
  y -= 36 + 36

  const comentario = datos.comentario?.trim()
  if (comentario) {
    ctx.fillStyle = '#ffffff'
    ctx.font = `400 46px ${FUENTE}`
    // Se corta en una palabra entera: "sin du…" no dice nada.
    const texto =
      comentario.length > MAXIMO_DEL_COMENTARIO
        ? `${comentario.slice(0, MAXIMO_DEL_COMENTARIO).replace(/\s+\S*$/, '').trimEnd()}…`
        : comentario
    const lineas = renglones(ctx, `«${texto}»`, ancho, 4)
    for (let i = lineas.length - 1; i >= 0; i--) {
      ctx.fillText(lineas[i], MARGEN, y)
      y -= 64
    }
    y -= 12
  }

  // El sello de la nota, como el de la ficha: amarillo, con el tablero y el número.
  const altoSello = 128
  const textoNota = nota(datos.puntaje)
  ctx.font = `700 76px ${FUENTE}`
  const anchoTablero = 104
  const anchoSello = 28 + anchoTablero + 20 + ctx.measureText(textoNota).width + 36
  y -= altoSello
  ctx.fillStyle = MOSTAZA
  rectanguloRedondeado(ctx, MARGEN, y, anchoSello, altoSello, 28)
  ctx.fill()
  dibujarTablero(ctx, MARGEN + 28, y + (altoSello - (anchoTablero * 14) / 24) / 2, anchoTablero, datos.puntaje / 5)
  ctx.fillStyle = TINTA
  ctx.textBaseline = 'middle'
  ctx.fillText(textoNota, MARGEN + 28 + anchoTablero + 20, y + altoSello / 2 + 4)
  ctx.textBaseline = 'alphabetic'
  y -= 44

  // El nombre del local, en hasta tres renglones.
  ctx.fillStyle = '#ffffff'
  ctx.font = `700 104px ${FUENTE}`
  const lineasDelNombre = renglones(ctx, datos.nombreDelLocal, ancho, 3)
  for (let i = lineasDelNombre.length - 1; i >= 0; i--) {
    ctx.fillText(lineasDelNombre[i], MARGEN, y)
    y -= 116
  }
  y -= 4

  if (datos.barrio) {
    ctx.fillStyle = '#f3e8cf'
    ctx.font = `700 38px ${FUENTE}`
    ctx.fillText(datos.barrio.toUpperCase(), MARGEN, y)
  }

  return new Promise((resolver, rechazar) => {
    canvas.toBlob((blob) => (blob ? resolver(blob) : rechazar(new Error('No se pudo armar la imagen'))), 'image/png')
  })
}
