/**
 * La dirección como la diría alguien de acá.
 *
 * Google devuelve la dirección completa y postal: "Honduras 5509, C1414BNC Cdad.
 * Autónoma de Buenos Aires, Argentina". El código postal y el país no le dicen nada
 * a alguien que busca dónde comer, y ocupan dos renglones en cada tarjeta.
 *
 * Queda la calle con la altura más el barrio, que es el dato que la gente usa:
 * "Honduras 5509, Palermo".
 */
export function shortAddress(address: string, area: string | null): string {
  if (!address) {
    return area ?? ''
  }

  // La calle y la altura son lo que va antes de la primera coma. Lo que sigue es
  // siempre código postal, ciudad y país.
  const street = address.split(',')[0].trim()

  if (!area) {
    return street
  }

  // Alguna dirección ya trae el barrio; no repetirlo.
  if (street.toLowerCase().includes(area.toLowerCase())) {
    return street
  }

  return `${street}, ${area}`
}
