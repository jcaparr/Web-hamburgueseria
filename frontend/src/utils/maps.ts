export function mapsUrl(latitude: number | null, longitude: number | null, fallbackQuery: string): string {
  const query = latitude != null && longitude != null ? `${latitude},${longitude}` : fallbackQuery
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`
}
