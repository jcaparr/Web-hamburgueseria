import type { TuMes } from '../types'
import { nota } from './numeros'
import { ALTO, ANCHO, BORDE_LIBRE_ABAJO, FUENTE, MARCA_ARRIBA, MARGEN, cargarLogo, recortar, renglones } from './tarjetaDeHistoria'
import { FONDOS, anioDelMes, nombreDelMes } from './tuMes'

/**
 * La imagen vertical de "Tu mes en Burgómetro" (#308), para compartir en historias: una
 * sola, la del cierre, con las cifras del mes. Mismo lienzo y mismas zonas seguras que la
 * historia de una reseña (#278): la marca bajo el encabezado de Instagram y la línea de
 * la persona casi al borde de abajo. Se dibuja en el teléfono, sin servidor ni Google.
 *
 * Fondo y texto son los de la tarjeta de portada (5,84 : 1); el texto va siempre en el
 * color pleno, sin transparencias, para no bajar de 4,5 : 1.
 */
export async function armarTarjetaDeResumen(resumen: TuMes, username: string): Promise<Blob> {
  await Promise.all([document.fonts.load(`700 60px ${FUENTE}`), document.fonts.load(`400 40px ${FUENTE}`)])
  const logo = await cargarLogo()

  const canvas = document.createElement('canvas')
  canvas.width = ANCHO
  canvas.height = ALTO
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('El navegador no puede dibujar la imagen')

  const { fondo, texto } = FONDOS.portada
  ctx.fillStyle = fondo
  ctx.fillRect(0, 0, ANCHO, ALTO)
  ctx.fillStyle = texto
  ctx.textBaseline = 'alphabetic'
  ctx.textAlign = 'left'
  const ancho = ANCHO - MARGEN * 2

  if (logo) ctx.drawImage(logo, MARGEN, MARCA_ARRIBA, 118, 100)
  ctx.font = `700 52px ${FUENTE}`
  ctx.fillText('Burgómetro', MARGEN + 118 + 20, MARCA_ARRIBA + 68)

  // De arriba hacia abajo, a partir de un cuarto de la altura.
  let y = 640
  ctx.font = `700 64px ${FUENTE}`
  ctx.fillText(`Mi ${nombreDelMes(resumen.mes)} de ${anioDelMes(resumen.mes)}`, MARGEN, y)
  y += 250
  ctx.font = `700 260px ${FUENTE}`
  ctx.fillText(String(resumen.resenias), MARGEN, y)
  y += 90
  ctx.font = `400 60px ${FUENTE}`
  ctx.fillText(resumen.resenias === 1 ? 'reseña' : 'reseñas', MARGEN, y)

  if (resumen.barrios.length > 0) {
    y += 150
    ctx.font = `700 52px ${FUENTE}`
    const barrios = resumen.barrios.map((b) => b.nombre).join(', ')
    for (const linea of renglones(ctx, barrios, ancho, 2)) {
      ctx.fillText(linea, MARGEN, y)
      y += 68
    }
  }
  if (resumen.mejor) {
    y += 70
    ctx.font = `400 44px ${FUENTE}`
    ctx.fillText(`Mi mejor nota: ${nota(resumen.mejor.puntaje)}`, MARGEN, y)
    y += 64
    ctx.font = `700 52px ${FUENTE}`
    for (const linea of renglones(ctx, resumen.mejor.nombre, ancho, 2)) {
      ctx.fillText(linea, MARGEN, y)
      y += 68
    }
  }

  ctx.font = `400 36px ${FUENTE}`
  ctx.fillText(recortar(ctx, `@${username} en burgometro.com.ar`, ancho), MARGEN, ALTO - BORDE_LIBRE_ABAJO)

  return new Promise((resolver, rechazar) => {
    canvas.toBlob((blob) => (blob ? resolver(blob) : rechazar(new Error('No se pudo armar la imagen'))), 'image/png')
  })
}
