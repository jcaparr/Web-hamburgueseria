import type { TuMes } from '../types'
import { nota } from './numeros'
import {
  ALTO, ANCHO, BORDE_LIBRE_ABAJO, FUENTE, MARCA_ARRIBA, MARGEN,
  cargarImagen, cargarLogo, dibujarFoto, recortar, rectanguloRedondeado, renglones,
} from './tarjetaDeHistoria'
import { anioDelMes, nombreDelMes } from './tuMes'

const CAFE = '#3a2a1e'

/**
 * La imagen vertical de "Tu mes en Burgómetro" (#308), para compartir en historias: una
 * sola, la del cierre, con el mismo estilo que las tarjetas. La foto del local de la mejor
 * nota a pantalla completa (o un fondo de marca si no hay) y, encima, un panel crema casi
 * opaco con las cifras del mes. El texto va siempre sobre el panel, en café pleno: pasa
 * 4,5 : 1 aunque la foto detrás sea blanca o negra.
 *
 * Mismas zonas seguras que la historia de una reseña (#278): la marca bajo el encabezado
 * de Instagram y la línea de la persona casi al borde de abajo. Se dibuja en el teléfono,
 * sin servidor ni Google.
 */
export async function armarTarjetaDeResumen(resumen: TuMes, username: string, fotoDelFondo: string | null): Promise<Blob> {
  await Promise.all([document.fonts.load(`700 60px ${FUENTE}`), document.fonts.load(`400 40px ${FUENTE}`)])
  const [logo, foto] = await Promise.all([
    cargarLogo(),
    fotoDelFondo ? cargarImagen(`${fotoDelFondo}${fotoDelFondo.includes('?') ? '&' : '?'}cors=1`, true).catch(() => null) : Promise.resolve(null),
  ])

  const canvas = document.createElement('canvas')
  canvas.width = ANCHO
  canvas.height = ALTO
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('El navegador no puede dibujar la imagen')

  if (foto) {
    dibujarFoto(ctx, foto)
  } else {
    const fondo = ctx.createLinearGradient(0, 0, ANCHO, ALTO)
    fondo.addColorStop(0, '#b23a2e')
    fondo.addColorStop(0.6, '#6b2a1c')
    fondo.addColorStop(1, CAFE)
    ctx.fillStyle = fondo
    ctx.fillRect(0, 0, ANCHO, ALTO)
  }
  // Velo oscuro: la foto se ve, pero el panel y la marca no dependen de cómo sea.
  ctx.fillStyle = 'rgba(58,42,30,0.45)'
  ctx.fillRect(0, 0, ANCHO, ALTO)

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
