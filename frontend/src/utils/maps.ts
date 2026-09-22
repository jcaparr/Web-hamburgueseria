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
