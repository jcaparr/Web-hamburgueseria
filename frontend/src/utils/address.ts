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
  const street = calleDe(address)

  if (!street) {
    return area ?? ''
  }
  if (!area) {
    return street
  }

  // Alguna dirección ya trae el barrio; no repetirlo.
  if (street.toLowerCase().includes(area.toLowerCase())) {
    return street
  }

  return `${street}, ${area}`
}

/**
 * La calle con su altura, dentro de la dirección completa.
 *
 * Casi siempre es lo que va antes de la primera coma, pero no siempre: Google le
 * antepone a algunas direcciones el barrio o la comuna, y quedaba "Comuna 11" o
 * "Parque Chacabuco" en la tarjeta, que no le sirven a nadie para llegar.
 *
 * Así que primero se sacan los tramos que nunca son una calle —código postal, comuna,
 * ciudad y país— y de los que quedan se elige el primero que tenga altura. Si ninguno
 * la tiene —hay locales en una esquina o en un pasaje— queda el primero, y si no queda
 * ninguno, la dirección era solo el código postal y se muestra el barrio solo.
 */
function calleDe(address: string): string {
  if (!address) {
    return ''
  }

  const tramos = address
    .split(',')
    .map(tramo => tramo.trim())
    .filter(tramo => tramo && !esCodigoPostal(tramo) && !esComuna(tramo) && !esCiudadOPais(tramo))

  // La altura es lo que distingue una calle del nombre de un barrio.
  return tramos.find(tramo => /[0-9]/.test(tramo)) ?? tramos[0] ?? ''
}

/** "C1414BNC Cdad. Autónoma…", "B1870 CKB": el código postal argentino y lo que le sigue. */
function esCodigoPostal(tramo: string): boolean {
  return /^[A-Z][0-9]{4}/.test(tramo)
}

/** "Comuna 11": la división administrativa, que tiene número y no es una calle. */
function esComuna(tramo: string): boolean {
  return /^comuna\s/i.test(tramo)
}

/**
 * La ciudad y el país, que van al final de toda dirección y no aportan nada acá.
 * Hace falta sacarlos porque hay locales de los que Google no tiene la calle
 * —"NACHOBURGER" trae solo el código postal— y sin esto quedaba "Argentina".
 */
function esCiudadOPais(tramo: string): boolean {
  return /^argentina$/i.test(tramo) || /(cdad\.?|ciudad)\s*aut[oó]noma/i.test(tramo)
}
