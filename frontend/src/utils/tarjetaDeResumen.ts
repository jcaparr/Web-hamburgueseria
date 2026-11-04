import type { TuMes } from '../types'
import { nota } from './numeros'
import {
  ALTO, ANCHO, BORDE_LIBRE_ABAJO, FUENTE, MARCA_ARRIBA, MARGEN,
  cargarLogo, recortar, rectanguloRedondeado, renglones,
} from './tarjetaDeHistoria'
import { anioDelMes, nombreDelMes } from './tuMes'

const CAFE = '#3a2a1e'

/**
 * La imagen vertical de "Tu mes en Burgómetro" (#308), para compartir en historias: una
 * sola, la del cierre, con el mismo estilo que las tarjetas: el fondo de marca (brasa y
 * mostaza, con rayos) y, encima, un panel crema casi opaco con las cifras. El texto va
 * siempre sobre el panel, en café pleno: pasa 4,5 : 1 sin importar lo que haya detrás.
 *
 * Mismas zonas seguras que la historia de una reseña (#278): la marca bajo el encabezado
 * de Instagram y la línea de la persona casi al borde de abajo. Se dibuja en el teléfono,
 * sin servidor ni Google.
 */
export async function armarTarjetaDeResumen(resumen: TuMes, username: string): Promise<Blob> {
  await Promise.all([document.fonts.load(`700 60px ${FUENTE}`), document.fonts.load(`400 40px ${FUENTE}`)])
  const logo = await cargarLogo()

  const canvas = document.createElement('canvas')
  canvas.width = ANCHO
  canvas.height = ALTO
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('El navegador no puede dibujar la imagen')

  // Brasa y mostaza, como el fondo de las tarjetas.
  const fondo = ctx.createLinearGradient(0, 0, ANCHO * 0.5, ALTO)
  fondo.addColorStop(0, '#6b2a1c')
  fondo.addColorStop(1, CAFE)
  ctx.fillStyle = fondo
  ctx.fillRect(0, 0, ANCHO, ALTO)
  const mostaza = ctx.createRadialGradient(ANCHO * 0.85, ALTO * 0.12, 0, ANCHO * 0.85, ALTO * 0.12, ALTO * 0.55)
  mostaza.addColorStop(0, 'rgba(242,183,5,0.75)')
  mostaza.addColorStop(1, 'rgba(242,183,5,0)')
  ctx.fillStyle = mostaza
  ctx.fillRect(0, 0, ANCHO, ALTO)
  const brasa = ctx.createRadialGradient(ANCHO * 0.08, ALTO * 0.92, 0, ANCHO * 0.08, ALTO * 0.92, ALTO * 0.55)
  brasa.addColorStop(0, 'rgba(178,58,46,0.95)')
  brasa.addColorStop(1, 'rgba(178,58,46,0)')
  ctx.fillStyle = brasa
  ctx.fillRect(0, 0, ANCHO, ALTO)
  // Los rayos del medidor, abiertos en abanico desde el centro del panel.
  const cx = ANCHO / 2
  const cy = ALTO * 0.38
  ctx.fillStyle = 'rgba(250,243,227,0.14)'
  for (let angulo = 0; angulo < 360; angulo += 16) {
    const a0 = (angulo * Math.PI) / 180
    const a1 = ((angulo + 6) * Math.PI) / 180
    ctx.beginPath()
    ctx.moveTo(cx, cy)
    ctx.lineTo(cx + Math.cos(a0) * ALTO * 2, cy + Math.sin(a0) * ALTO * 2)
    ctx.lineTo(cx + Math.cos(a1) * ALTO * 2, cy + Math.sin(a1) * ALTO * 2)
    ctx.closePath()
    ctx.fill()
  }

  ctx.textBaseline = 'alphabetic'
  ctx.textAlign = 'left'
  const ancho = ANCHO - MARGEN * 2

  if (logo) ctx.drawImage(logo, MARGEN, MARCA_ARRIBA, 118, 100)
  ctx.fillStyle = '#ffffff'
  ctx.font = `700 52px ${FUENTE}`
  ctx.fillText('Burgómetro', MARGEN + 118 + 20, MARCA_ARRIBA + 68)

  // El panel de vidrio: crema casi opaco, con el arco de medidor arriba.
  const panelY = 520
  const panelH = 900
  ctx.fillStyle = 'rgba(250,243,227,0.93)'
  rectanguloRedondeado(ctx, MARGEN, panelY, ancho, panelH, 60)
  ctx.fill()

  ctx.fillStyle = CAFE
  const x = MARGEN + 56
  const interior = ancho - 112
  let y = panelY + 130
  ctx.font = `700 60px ${FUENTE}`
  ctx.fillText(resumen.anual ? `Mi ${anioDelMes(resumen.mes)} en Burgómetro` : `Mi ${nombreDelMes(resumen.mes)} de ${anioDelMes(resumen.mes)}`, x, y)
  y += 250
  ctx.font = `700 260px ${FUENTE}`
  ctx.fillText(String(resumen.resenias), x, y)
  y += 90
  ctx.font = `400 60px ${FUENTE}`
  ctx.fillText(resumen.resenias === 1 ? 'reseña' : 'reseñas', x, y)

  if (resumen.barrios.length > 0) {
    y += 130
    ctx.font = `700 50px ${FUENTE}`
    for (const linea of renglones(ctx, resumen.barrios.map((b) => b.nombre).join(', '), interior, 2)) {
      ctx.fillText(linea, x, y)
      y += 64
    }
  }
  if (resumen.mejor) {
    y += 50
    ctx.font = `400 44px ${FUENTE}`
    ctx.fillText(`Mi mejor nota: ${nota(resumen.mejor.puntaje)}`, x, y)
    y += 62
    ctx.font = `700 50px ${FUENTE}`
    for (const linea of renglones(ctx, resumen.mejor.nombre, interior, 2)) {
      ctx.fillText(linea, x, y)
      y += 64
    }
  }

  ctx.fillStyle = '#ffffff'
  ctx.font = `400 36px ${FUENTE}`
  ctx.fillText(recortar(ctx, `@${username} en burgometro.com.ar`, ancho), MARGEN, ALTO - BORDE_LIBRE_ABAJO)

  return new Promise((resolver, rechazar) => {
    canvas.toBlob((blob) => (blob ? resolver(blob) : rechazar(new Error('No se pudo armar la imagen'))), 'image/png')
  })
}
