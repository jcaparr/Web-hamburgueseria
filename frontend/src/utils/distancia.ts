import { kilometros } from './numeros'

/**
 * Los metros en línea recta entre dos puntos (fórmula de haversine). Alcanza para decir
 * "a 1,2 km" en la tarjeta del mapa; no es lo que se camina, y no pretende serlo.
 */
export function metrosEntre(a: { lat: number; lon: number }, b: { lat: number; lon: number }): number {
  const RADIO_DE_LA_TIERRA = 6_371_000
  const aRadianes = (grados: number) => (grados * Math.PI) / 180
  const dLat = aRadianes(b.lat - a.lat)
  const dLon = aRadianes(b.lon - a.lon)
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(aRadianes(a.lat)) * Math.cos(aRadianes(b.lat)) * Math.sin(dLon / 2) ** 2
  return 2 * RADIO_DE_LA_TIERRA * Math.asin(Math.sqrt(h))
}

/** "350 m" por debajo del kilómetro, "1,2 km" por encima. */
export function distanciaLegible(metros: number): string {
  return metros < 1000 ? `${Math.round(metros / 10) * 10} m` : `${kilometros(metros / 1000)} km`
}
