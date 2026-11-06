import { createElement } from 'react'
import { flushSync } from 'react-dom'
import { createRoot } from 'react-dom/client'
import { BurguiDeAnimo } from '../components/BurguiDeAnimo'
import { CUADRO_DE_BURGUI, FRASE_DEL_ANIMO, animoDelMes, type Animo } from './animoDelMes'
import type { TuMes } from '../types'
import { nota } from './numeros'
import {
  ALTO, ANCHO, BORDE_LIBRE_ABAJO, FUENTE, MARCA_ARRIBA,
  cargarImagen, cargarLogo, recortar, rectanguloRedondeado,
} from './tarjetaDeHistoria'
import { anioDelMes, nombreDelMes } from './tuMes'

const BRASA = '#2a1a12'
const CREMA = '#faf3e3'
const MOSTAZA = '#f2b705'
const ROJO = '#ff5a3c'
const TINTA = '#141110'
/** La letra del logo: la web trae solo la negrita (ver index.css). */
const LETRA_DEL_LOGO = '"Lexend", system-ui, sans-serif'

/** Los íconos de la tarjeta, con el mismo dibujo que la maqueta: un punto de mapa, una medalla y barras. */
const ICONOS = {
  barrio: ['M12 22s7-6.2 7-12a7 7 0 1 0-14 0c0 5.8 7 12 7 12z'],
  logro: ['M8 2h8l-2 6h-4z', 'M12 8a7 7 0 1 0 0 14a7 7 0 1 0 0-14z'],
  nivel: ['M4 20h4v-6H4zM10 20h4V9h-4zM16 20h4V4h-4z'],
}

/**
 * La imagen vertical de "Tu mes en Burgómetro", para compartir en historias (#308, rediseñada
 * en #421): fondo brasa, Burgui en el centro con el ánimo del mes (la aguja de su cara es el
 * medidor), la cifra grande con una frase y una sola tarjeta con barrios, logros, nivel y el
 * mejor local. Poca cosa a propósito: en una historia se mira un segundo.
 *
 * Mismas zonas seguras que la historia de una reseña (#278): la marca bajo el encabezado de
 * Instagram y la línea de la persona casi al borde de abajo. Se dibuja en el teléfono, sin
 * servidor ni Google.
 */
export async function armarTarjetaDeResumen(resumen: TuMes, username: string): Promise<Blob> {
  await Promise.all([
    document.fonts.load(`700 60px ${FUENTE}`),
    document.fonts.load(`400 40px ${FUENTE}`),
    document.fonts.load(`700 60px ${LETRA_DEL_LOGO}`),
  ])
  const animo = animoDelMes(resumen.resenias)
  const [logo, burgui] = await Promise.all([cargarLogo(), cargarBurgui(animo)])

  const canvas = document.createElement('canvas')
  canvas.width = ANCHO
  canvas.height = ALTO
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('El navegador no puede dibujar la imagen')

  // Brasa, con un resplandor cálido detrás de Burgui.
  ctx.fillStyle = BRASA
  ctx.fillRect(0, 0, ANCHO, ALTO)
  const resplandor = ctx.createRadialGradient(ANCHO / 2, 560, 0, ANCHO / 2, 560, 760)
  resplandor.addColorStop(0, 'rgba(110,52,32,0.85)')
  resplandor.addColorStop(1, 'rgba(110,52,32,0)')
  ctx.fillStyle = resplandor
  ctx.fillRect(0, 0, ANCHO, ALTO)

  ctx.textBaseline = 'alphabetic'
  ctx.textAlign = 'center'
  const centro = ANCHO / 2

  // La marca, centrada bajo el encabezado de Instagram.
  ctx.font = `700 50px ${LETRA_DEL_LOGO}`
  const marca = 'Burgómetro'
  const anchoDeMarca = (logo ? 119 + 24 : 0) + ctx.measureText(marca).width
  const inicio = centro - anchoDeMarca / 2
  const arriba = MARCA_ARRIBA + 23
  if (logo) ctx.drawImage(logo, inicio, arriba, 119, 100)
  ctx.fillStyle = CREMA
  ctx.textAlign = 'left'
  ctx.fillText(marca, inicio + (logo ? 143 : 0), arriba + 68)
  ctx.textAlign = 'center'

  // El período, en mostaza y espaciado.
  const periodo = resumen.anual
    ? `MI ${anioDelMes(resumen.mes)}`
    : `MI ${nombreDelMes(resumen.mes).toUpperCase()} ${anioDelMes(resumen.mes)}`
  ctx.fillStyle = MOSTAZA
  ctx.font = `700 41px ${FUENTE}`
  espaciado(ctx, '6px')
  ctx.fillText(periodo, centro, 356)
  espaciado(ctx, '0px')

  // Burgui, con un filo crema para que el trazo negro no se pierda en el fondo oscuro.
  if (burgui) {
    const ancho = 475
    const alto = (ancho * CUADRO_DE_BURGUI.alto) / CUADRO_DE_BURGUI.ancho
    ctx.save()
    ctx.shadowColor = 'rgba(250,243,227,0.55)'
    ctx.shadowBlur = 6
    ctx.drawImage(burgui, centro - ancho / 2, 367, ancho, alto)
    ctx.restore()
  }

  // La cifra, qué es y la frase de Burgui.
  ctx.fillStyle = '#ffffff'
  ctx.font = `700 230px ${LETRA_DEL_LOGO}`
  ctx.fillText(String(resumen.resenias), centro, 1100)
  ctx.fillStyle = CREMA
  ctx.font = `400 48px ${FUENTE}`
  ctx.fillText(resumen.resenias === 1 ? 'hamburguesa reseñada' : 'hamburguesas reseñadas', centro, 1162)
  ctx.fillStyle = ROJO
  ctx.font = `700 46px ${FUENTE}`
  ctx.fillText(FRASE_DEL_ANIMO[animo], centro, 1228)

  dibujarTarjeta(ctx, resumen)

  ctx.fillStyle = 'rgba(250,243,227,0.85)'
  ctx.font = `700 36px ${FUENTE}`
  ctx.fillText(recortar(ctx, `@${username} · burgometro.com.ar`, ANCHO - 128), centro, ALTO - BORDE_LIBRE_ABAJO)

  return new Promise((resolver, rechazar) => {
    canvas.toBlob((blob) => (blob ? resolver(blob) : rechazar(new Error('No se pudo armar la imagen'))), 'image/png')
  })
}

/** La tarjeta de abajo: tres cifras, cada una centrada bajo su ícono, y el mejor local si hubo. */
function dibujarTarjeta(ctx: CanvasRenderingContext2D, resumen: TuMes) {
  const x = 86
  const y = 1296
  const ancho = ANCHO - x * 2
  const altoDeFila = 236
  const altoDelMejor = resumen.mejor ? 156 : 0
  const alto = altoDeFila + altoDelMejor

  ctx.fillStyle = 'rgba(250,243,227,0.07)'
  rectanguloRedondeado(ctx, x, y, ancho, alto, 54)
  ctx.fill()
  ctx.strokeStyle = 'rgba(250,243,227,0.2)'
  ctx.lineWidth = 3
  ctx.stroke()

  const datos: { icono: string[]; valor: number; nombre: string }[] = [
    { icono: ICONOS.barrio, valor: resumen.barriosEnTotal, nombre: resumen.barriosEnTotal === 1 ? 'barrio' : 'barrios' },
    { icono: ICONOS.logro, valor: resumen.logros, nombre: resumen.logros === 1 ? 'logro' : 'logros' },
    { icono: ICONOS.nivel, valor: resumen.nivel, nombre: 'nivel' },
  ]
  const columna = ancho / datos.length
  ctx.strokeStyle = 'rgba(250,243,227,0.14)'
  datos.forEach((dato, i) => {
    const cx = x + columna * i + columna / 2
    if (i > 0) linea(ctx, x + columna * i, y, x + columna * i, y + altoDeFila)
    dibujarIcono(ctx, dato.icono, cx, y + 34, 54)
    ctx.fillStyle = CREMA
    ctx.font = `700 69px ${LETRA_DEL_LOGO}`
    ctx.fillText(String(dato.valor), cx, y + 165)
    ctx.fillStyle = 'rgba(250,243,227,0.8)'
    ctx.font = `400 32px ${FUENTE}`
    ctx.fillText(dato.nombre, cx, y + 208)
  })

  if (!resumen.mejor) return
  const arriba = y + altoDeFila
  linea(ctx, x, arriba, x + ancho, arriba)
  const izquierda = x + 54
  ctx.textAlign = 'left'
  ctx.fillStyle = 'rgba(250,243,227,0.8)'
  ctx.font = `400 32px ${FUENTE}`
  ctx.fillText('Mi mejor del mes', izquierda, arriba + 56)

  // La nota en una píldora roja, pegada a la derecha; el nombre usa lo que queda.
  const textoDeNota = nota(resumen.mejor.puntaje)
  ctx.font = `700 43px ${FUENTE}`
  const anchoDePildora = ctx.measureText(textoDeNota).width + 56
  const derecha = x + ancho - 54
  ctx.fillStyle = ROJO
  rectanguloRedondeado(ctx, derecha - anchoDePildora, arriba + 74, anchoDePildora, 60, 30)
  ctx.fill()
  ctx.fillStyle = TINTA
  ctx.textAlign = 'center'
  ctx.fillText(textoDeNota, derecha - anchoDePildora / 2, arriba + 119)

  ctx.textAlign = 'left'
  ctx.fillStyle = CREMA
  ctx.font = `700 52px ${FUENTE}`
  ctx.fillText(recortar(ctx, resumen.mejor.nombre, derecha - anchoDePildora - 30 - izquierda), izquierda, arriba + 122)
  ctx.textAlign = 'center'
}

function dibujarIcono(ctx: CanvasRenderingContext2D, partes: string[], cx: number, y: number, lado: number) {
  ctx.save()
  ctx.translate(cx - lado / 2, y)
  ctx.scale(lado / 24, lado / 24)
  ctx.fillStyle = MOSTAZA
  for (const parte of partes) ctx.fill(new Path2D(parte))
  ctx.restore()
}

function linea(ctx: CanvasRenderingContext2D, x0: number, y0: number, x1: number, y1: number) {
  ctx.beginPath()
  ctx.moveTo(x0, y0)
  ctx.lineTo(x1, y1)
  ctx.lineWidth = 3
  ctx.stroke()
}

/** Las letras separadas, donde el navegador lo sabe hacer; si no, quedan juntas y se lee igual. */
function espaciado(ctx: CanvasRenderingContext2D, valor: string) {
  if ('letterSpacing' in ctx) (ctx as CanvasRenderingContext2D & { letterSpacing: string }).letterSpacing = valor
}

/**
 * Burgui como imagen: se arma el componente en un nodo suelto (el mismo dibujo de la app, sin
 * copiarlo) y se pasa su SVG a una imagen que el canvas puede dibujar.
 */
async function cargarBurgui(animo: Animo): Promise<HTMLImageElement | null> {
  const nodo = document.createElement('div')
  const raiz = createRoot(nodo)
  try {
    flushSync(() => raiz.render(createElement(BurguiDeAnimo, { animo })))
    const svg = nodo.innerHTML
    return await cargarImagen(`data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`)
  } catch {
    return null
  } finally {
    raiz.unmount()
  }
}
