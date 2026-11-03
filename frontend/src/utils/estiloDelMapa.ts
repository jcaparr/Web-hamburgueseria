import { DARK, LIGHT, layers, type Flavor } from '@protomaps/basemaps'
import type { StyleSpecification } from 'maplibre-gl'

/**
 * El estilo del mapa base (#395): las capas de Protomaps teñidas con la paleta de la app.
 *
 * De día son los cremas de "burger"; de noche, los marrones de "burger-noche". Se parte de
 * los estilos que trae Protomaps (LIGHT y DARK) y se pisan los colores del suelo, el agua,
 * los parques, los edificios y las calles; lo que no se nombra acá queda como viene.
 *
 * Contraste (fórmula WCAG) de los textos sobre su fondo: calles 6,96 : 1 de día (#5c4a3a
 * sobre #f1e9d6) y 9,29 : 1 de noche (#c9b89c sobre #1c1510); nombres del agua 5,13 : 1 y
 * 5,96 : 1. Todos pasan 4,5 : 1.
 */

/** Los nombres de lugares (parques, estaciones, aeropuertos) venían en azul y verde: acá van en la paleta. */
const POIS_CLARO = Object.fromEntries(Object.keys(LIGHT.pois ?? {}).map((k) => [k, '#5c4a3a'])) as unknown as NonNullable<Flavor['pois']>
const POIS_OSCURO = Object.fromEntries(Object.keys(DARK.pois ?? {}).map((k) => [k, '#c9b89c'])) as unknown as NonNullable<Flavor['pois']>

const CLARO: Flavor = {
  ...LIGHT,
  background: '#f1e9d6',
  earth: '#f1e9d6',
  park_a: '#d9e3c0',
  park_b: '#cddcae',
  wood_a: '#d3e0b8',
  wood_b: '#c4d6a4',
  scrub_a: '#e0e6c8',
  scrub_b: '#d4dcb4',
  water: '#b8d4d6',
  hospital: '#efdcd3',
  industrial: '#ebe1ca',
  school: '#ede2c8',
  pedestrian: '#f6efe0',
  sand: '#eee3c6',
  beach: '#f0e6c8',
  buildings: '#e4d9bf',

  minor_service: '#fffdf8',
  minor_a: '#fffdf8',
  minor_b: '#fffdf8',
  minor_casing: '#e3d7bb',
  minor_service_casing: '#e8dec6',
  link: '#fffdf8',
  link_casing: '#e3d7bb',
  major: '#fbe9b4',
  major_casing_early: '#e3cf94',
  major_casing_late: '#e3cf94',
  highway: '#f6dfa6',
  highway_casing_early: '#dcc27d',
  highway_casing_late: '#dcc27d',
  other: '#fffdf8',

  roads_label_minor: '#5c4a3a',
  roads_label_minor_halo: '#f1e9d6',
  roads_label_major: '#4a3a2c',
  roads_label_major_halo: '#f1e9d6',
  subplace_label: '#5c4a3a',
  subplace_label_halo: '#f1e9d6',
  city_label: '#3a2a1e',
  city_label_halo: '#f1e9d6',
  state_label: '#6b5848',
  state_label_halo: '#f1e9d6',
  country_label: '#6b5848',
  ocean_label: '#335659',
  address_label: '#6b5848',
  address_label_halo: '#f1e9d6',
  pois: POIS_CLARO,
}

const OSCURO: Flavor = {
  ...DARK,
  background: '#1c1510',
  earth: '#1c1510',
  park_a: '#1e2716',
  park_b: '#222d19',
  wood_a: '#1e2716',
  wood_b: '#222d19',
  scrub_a: '#1f2517',
  scrub_b: '#232a1a',
  water: '#10262a',
  hospital: '#271b17',
  industrial: '#211912',
  school: '#241a13',
  pedestrian: '#241a13',
  sand: '#241c14',
  beach: '#261e15',
  buildings: '#241a13',

  minor_service: '#33271d',
  minor_a: '#33271d',
  minor_b: '#33271d',
  minor_casing: '#1c1510',
  minor_service_casing: '#1c1510',
  link: '#3f3024',
  link_casing: '#1c1510',
  major: '#4a3824',
  major_casing_early: '#1c1510',
  major_casing_late: '#1c1510',
  highway: '#5a4529',
  highway_casing_early: '#1c1510',
  highway_casing_late: '#1c1510',
  other: '#33271d',

  roads_label_minor: '#c9b89c',
  roads_label_minor_halo: '#1c1510',
  roads_label_major: '#e3d4b6',
  roads_label_major_halo: '#1c1510',
  subplace_label: '#c9b89c',
  subplace_label_halo: '#1c1510',
  city_label: '#f3e8cf',
  city_label_halo: '#1c1510',
  state_label: '#c9b89c',
  state_label_halo: '#1c1510',
  country_label: '#c9b89c',
  ocean_label: '#7fa6ab',
  address_label: '#c9b89c',
  address_label_halo: '#1c1510',
  pois: POIS_OSCURO,
}

/** Atribución obligatoria de OpenStreetMap (licencia ODbL) y de Protomaps: siempre visible. */
export const ATRIBUCION =
  '<a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">© OpenStreetMap</a> · ' +
  '<a href="https://protomaps.com" target="_blank" rel="noopener noreferrer">Protomaps</a>'

/**
 * Dónde vive el archivo del mapa. Caddy lo sirve en producción; Vite, en desarrollo (ver
 * vite.config.ts). En desarrollo se puede apuntar a un extracto más chico con
 * VITE_MAPA_ARCHIVO=/tiles/amba.pmtiles en el .env.local.
 */
export const ARCHIVO_DEL_MAPA: string = import.meta.env.VITE_MAPA_ARCHIVO || '/tiles/argentina.pmtiles'

export function estiloDelMapa(tema: 'claro' | 'oscuro'): StyleSpecification {
  const flavor = tema === 'oscuro' ? OSCURO : CLARO
  return {
    version: 8,
    // Las tipografías de las calles salen de nuestro propio servidor (public/mapa-fuentes):
    // la política de contenido no deja pedirlas afuera, y así nadie le deja su IP a un tercero.
    glyphs: `${window.location.origin}/mapa-fuentes/{fontstack}/{range}.pbf`,
    sources: {
      protomaps: {
        type: 'vector',
        url: `pmtiles://${window.location.origin}${ARCHIVO_DEL_MAPA}`,
        attribution: ATRIBUCION,
      },
    },
    // Nombres en castellano donde OpenStreetMap los tiene.
    layers: layers('protomaps', flavor, { lang: 'es' }),
  }
}
