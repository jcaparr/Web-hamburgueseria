/** Lo mínimo que hace falta de un local para mandarlo a Maps. */
interface Parada {
  name: string
  placeId: string | null
  latitude: number | null
  longitude: number | null
}

/**
 * El enlace que abre todo el recorrido en Maps, con las paradas en orden.
 *
 * Maps admite hasta nueve escalas además del destino; el backend nunca arma recorridos
 * de más de diez paradas, así que entra siempre.
 *
 * Va el identificador de cada local junto con su nombre —Maps exige los dos— para que
 * el recorrido muestre "Burger couple" y no un punto suelto en el mapa. En los que no
 * tienen identificador se cae a las coordenadas, que es lo que había antes.
 *
 * El modo viaja en el enlace, así que Maps abre directamente el recorrido a pie o en
 * auto según lo que se haya elegido acá, sin tener que cambiarlo allá.
 */
export function routeUrl(
  paradas: Parada[],
  opciones: { desde?: { lat: number; lon: number }; enAuto?: boolean } = {},
): string {
  const { desde, enAuto } = opciones
  const puntos = paradas.filter((p) => p.latitude != null && p.longitude != null)
  if (puntos.length === 0) return ''

  const destino = puntos[puntos.length - 1]
  // Con la ubicación de quien camina, las escalas son todas las paradas menos la
  // última; sin ella se arranca en la primera y las escalas son las del medio.
  const escalas = desde ? puntos.slice(0, -1) : puntos.slice(1, -1)
  const origen = desde ? `${desde.lat},${desde.lon}` : donde(puntos[0])

  const params = new URLSearchParams({
    api: '1',
    origin: origen,
    destination: donde(destino),
    travelmode: enAuto ? 'driving' : 'walking',
  })
  if (!desde && puntos[0].placeId) params.set('origin_place_id', puntos[0].placeId)
  if (destino.placeId) params.set('destination_place_id', destino.placeId)

  if (escalas.length > 0) {
    params.set('waypoints', escalas.map(donde).join('|'))
    // Maps pide el identificador de cada escala en el mismo orden, y solo los acepta si
    // los tienen todas: con una sola sin ficha, se mandan las coordenadas y listo.
    if (escalas.every((p) => p.placeId)) {
      params.set('waypoint_place_ids', escalas.map((p) => p.placeId).join('|'))
    }
  }

  return `https://www.google.com/maps/dir/?${params.toString()}`
}

function donde(parada: Parada): string {
  return parada.placeId ? parada.name : `${parada.latitude},${parada.longitude}`
}

/**
 * El enlace a Google Maps de un local.
 *
 * Antes se mandaban las coordenadas como búsqueda, y Maps abría un pin suelto
 * titulado "34°35'12.3\"S 58°26'20.7\"W": sin nombre, sin fotos, sin horarios y sin
 * reseñas. Con el identificador del local, Maps abre su ficha.
 *
 * El identificador de Google es público —viaja en cualquier enlace compartido a
 * Maps—, así que exponerlo no filtra nada.
 */
export function mapsUrl(
  placeId: string | null | undefined,
  name: string,
  latitude: number | null,
  longitude: number | null,
): string {
  // Google pide igual un "query" aunque venga el identificador. Va el nombre, que es
  // lo que muestra mientras carga la ficha.
  const query = name || (latitude != null && longitude != null ? `${latitude},${longitude}` : '')
  const base = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`

  // Sin identificador queda el comportamiento de antes: los locales cargados a mano,
  // si algún día los hay, no tienen ficha en Google a la que apuntar.
  return placeId ? `${base}&query_place_id=${encodeURIComponent(placeId)}` : base
}

/**
 * Cómo llegar a un local desde donde está quien mira.
 *
 * Sin origen, Maps arranca desde la ubicación del teléfono, que es lo que se quiere
 * casi siempre. No sirve {@link routeUrl} con una sola parada: sin ubicación usa la
 * primera parada como origen, y acá esa parada es también el destino.
 */
export function comoLlegarUrl(local: Parada): string {
  if (local.latitude == null || local.longitude == null) {
    return mapsUrl(local.placeId, local.name, local.latitude, local.longitude)
  }
  const params = new URLSearchParams({ api: '1', destination: donde(local) })
  if (local.placeId) params.set('destination_place_id', local.placeId)
  return `https://www.google.com/maps/dir/?${params.toString()}`
}
