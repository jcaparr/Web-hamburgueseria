import type { BurgerJoint, Horario } from '../types'
import { nota } from './numeros'
import { shortAddress } from './address'

/** Los días como los nombra schema.org, en el orden de FranjaHoraria.dia: 0 es domingo. */
const DIAS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']

/** Minutos desde la medianoche a "HH:MM". Un cierre después de medianoche da la vuelta. */
function hora(minutos: number): string {
  const delDia = ((minutos % 1440) + 1440) % 1440
  const h = Math.floor(delDia / 60)
  const m = delDia % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

/**
 * La ficha de un local en schema.org: lo que Google usa para mostrar la nota con
 * estrellas, la dirección y el horario en los resultados de búsqueda.
 *
 * La nota va solo si hay reseñas: una nota sin reseñas detrás Google la descarta, y
 * puede tomarlo como datos engañosos para todo el sitio.
 */
export function restauranteDe(local: BurgerJoint, horario: Horario | null, origen: string) {
  return {
    '@context': 'https://schema.org',
    '@type': 'Restaurant',
    name: local.name,
    url: `${origen}/burger-joints/${local.id}`,
    servesCuisine: 'Hamburguesas',
    ...(local.photoUrl ? { image: `${origen}${local.photoUrl}` } : {}),
    address: {
      '@type': 'PostalAddress',
      streetAddress: local.address,
      ...(local.area ? { addressLocality: local.area } : {}),
      addressRegion: 'Buenos Aires',
      addressCountry: 'AR',
    },
    ...(local.latitude !== null && local.longitude !== null
      ? { geo: { '@type': 'GeoCoordinates', latitude: local.latitude, longitude: local.longitude } }
      : {}),
    ...(local.averageScore !== null && local.ratingsCount > 0
      ? {
          aggregateRating: {
            '@type': 'AggregateRating',
            ratingValue: Number(local.averageScore.toFixed(1)),
            bestRating: 5,
            worstRating: 1,
            ratingCount: local.ratingsCount,
          },
        }
      : {}),
    ...(horario && horario.franjas.length > 0
      ? {
          openingHoursSpecification: horario.franjas.map((franja) => ({
            '@type': 'OpeningHoursSpecification',
            dayOfWeek: `https://schema.org/${DIAS[franja.dia]}`,
            opens: hora(franja.abre),
            closes: hora(franja.cierra),
          })),
        }
      : {}),
  }
}

/** La descripción de la ficha de un local para los resultados de búsqueda. */
export function descripcionDe(local: BurgerJoint): string {
  const donde = shortAddress(local.address, local.area) || 'Buenos Aires'
  if (local.averageScore !== null && local.ratingsCount > 0) {
    const resenias = local.ratingsCount === 1 ? '1 reseña' : `${local.ratingsCount} reseñas`
    return `${local.name}: nota ${nota(local.averageScore)} de 5 con ${resenias} en Burgómetro. ${donde}.`
  }
  return `${local.name}, hamburguesería en ${donde}. Mirá dónde queda, su horario y las reseñas en Burgómetro.`
}
