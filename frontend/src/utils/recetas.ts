/**
 * Las hamburguesas de los avatares: de qué se puede armar una y cuál le toca a cada uno.
 *
 * Aparte del dibujo (AvatarDeUsuario) porque son datos: el dibujo los pinta, y lo demás
 * los usa sin pintar nada.
 */

/**
 * El fondo y, con él, el color del contorno.
 *
 * El contorno es el texto que el tema pone sobre ese fondo (currentColor): crema sobre
 * el kétchup del tema claro, marrón sobre el kétchup más claro del oscuro. Así cada capa
 * se despega del fondo en los dos temas, aunque el pan y el fondo se parezcan.
 */
export const FONDOS = [
  'bg-primary text-primary-content',
  'bg-secondary text-secondary-content',
  'bg-accent text-accent-content',
  'bg-info text-info-content',
  // El color del texto como fondo: marrón en el tema claro y crema en el oscuro. El
  // neutral del tema oscuro es casi el de la página, y el círculo se perdía.
  'bg-base-content text-base-100',
] as const

/** El pan va fijo y no sale del tema, igual que en el logo: es pan, no un color de la marca. */
export const PANES = {
  clasico: { relleno: '#cf8636', semillas: true },
  papa: { relleno: '#e9b860', semillas: false },
  negro: { relleno: '#4a3426', semillas: true },
} as const

export const QUESOS = { cheddar: '#f2b705', dambo: '#f6dd8f', sin: null } as const

export const VERDURAS = ['nada', 'lechuga', 'tomate', 'completa'] as const

export interface Receta {
  fondo: number
  pan: keyof typeof PANES
  queso: keyof typeof QUESOS
  verdura: (typeof VERDURAS)[number]
  carnes: 1 | 2 | 3
}

/**
 * La hamburguesa que le toca a cada nombre.
 *
 * Sale del nombre y no de un azar: si cambiara entre pantallas —o entre dos cargas de la
 * misma— dejaría de servir para reconocer a nadie. El hash es FNV-1a; la suma de letras
 * que había antes les daba lo mismo a "ana" y "naa", y con cinco cosas para elegir esos
 * choques se notan más.
 */
export function recetaDe(username: string): Receta {
  let hash = 0x811c9dc5
  for (let i = 0; i < username.length; i++) {
    hash ^= username.charCodeAt(i)
    hash = Math.imul(hash, 0x01000193)
  }
  let resto = hash >>> 0
  const elegir = (cuantos: number) => {
    const valor = resto % cuantos
    resto = Math.floor(resto / cuantos)
    return valor
  }
  return {
    fondo: elegir(FONDOS.length),
    pan: ORDEN_DE_PANES[elegir(ORDEN_DE_PANES.length)],
    // De los dos primeros. Sin queso existe, pero no sale solo: con sin queso, sin verdura y una carne, a uno
    // de cada treinta y seis le tocaba un pan con una carne y nada más.
    queso: ORDEN_DE_QUESOS[elegir(2)],
    verdura: VERDURAS[elegir(VERDURAS.length)],
    carnes: ([1, 2, 3] as const)[elegir(3)],
  }
}

/*
 * Cómo viaja una receta: cinco cifras, una por cosa y en este orden: fondo, pan, queso,
 * lo verde y carnes. Es lo que guarda el servidor, que no sabe qué es cada número; el
 * orden de estas listas es el contrato, así que a una opción nueva se la agrega al
 * final, nunca en el medio.
 */
const ORDEN_DE_PANES = ['clasico', 'papa', 'negro'] as const
const ORDEN_DE_QUESOS = ['cheddar', 'dambo', 'sin'] as const
const CODIGO = /^([0-4])([0-2])([0-2])([0-3])([1-3])$/

export function codigoDe(receta: Receta): string {
  return [
    receta.fondo,
    ORDEN_DE_PANES.indexOf(receta.pan),
    ORDEN_DE_QUESOS.indexOf(receta.queso),
    VERDURAS.indexOf(receta.verdura),
    receta.carnes,
  ].join('')
}

/** Null si el código no es uno que se pueda dibujar. */
function recetaDeCodigo(codigo: string): Receta | null {
  const partes = CODIGO.exec(codigo)
  if (!partes) return null
  const [fondo, pan, queso, verdura, carnes] = partes.slice(1).map(Number)
  return {
    fondo,
    pan: ORDEN_DE_PANES[pan],
    queso: ORDEN_DE_QUESOS[queso],
    verdura: VERDURAS[verdura],
    carnes: carnes as Receta['carnes'],
  }
}

/**
 * La hamburguesa de alguien: la que eligió, o la de su nombre si no eligió ninguna.
 *
 * Un código que no se entiende también cae en la del nombre, en vez de romper el dibujo.
 * No debería llegar ninguno —el servidor los valida al guardar—, pero el avatar está en
 * cada tarjeta del feed y no vale la pena arriesgar una pantalla por eso.
 */
export function recetaPara(username: string, hamburguesa?: string | null): Receta {
  return (hamburguesa && recetaDeCodigo(hamburguesa)) || recetaDe(username)
}
