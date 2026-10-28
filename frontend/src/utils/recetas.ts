/**
 * Las hamburguesas de los avatares: de qué se puede armar una y cuál le toca a cada uno.
 *
 * Aparte del dibujo (AvatarDeUsuario) porque son datos: el dibujo los pinta, y lo demás
 * los usa sin pintar nada.
 */

/**
 * El fondo del avatar y el color del trazo, fijos: los del tema oscuro (#195).
 *
 * Salían de los colores del tema, así que la hamburguesa de cada uno cambiaba al pasar
 * de oscuro a claro. Es la cara de alguien, y tiene que ser la misma en los dos temas.
 *
 * Diez fondos (#370), cada uno un degradé radial que va del tono claro, arriba a la
 * izquierda, a uno más profundo y de otro matiz, no solo más oscuro: el kétchup va del
 * coral al rojo quemado y la mostaza del limón al ámbar. Un filo de luz interior le da
 * volumen al círculo. El trazo oscuro da 4,6 : 1 o más contra el extremo más oscuro de
 * cada uno (el más justo es el kétchup), medido con la fórmula de WCAG. Las clases van
 * escritas enteras y no armadas con una función: Tailwind solo ve las que están en el texto.
 */
export const FONDOS = [
  'bg-[radial-gradient(circle_at_30%_25%,#f58a6a,#d9503a)] shadow-[inset_0_0_0_1.5px_rgb(255_255_255/0.4)] text-[#17110d]',
  'bg-[radial-gradient(circle_at_30%_25%,#fbe27a,#dc9a1e)] shadow-[inset_0_0_0_1.5px_rgb(255_255_255/0.4)] text-[#17110d]',
  'bg-[radial-gradient(circle_at_30%_25%,#b4d98a,#5f9248)] shadow-[inset_0_0_0_1.5px_rgb(255_255_255/0.4)] text-[#17110d]',
  'bg-[radial-gradient(circle_at_30%_25%,#aee0e6,#4f93ad)] shadow-[inset_0_0_0_1.5px_rgb(255_255_255/0.4)] text-[#17110d]',
  // Crema. Sobre la página del tema claro, que es casi del mismo color, el círculo se
  // perdía: el borde, fijo, lo marca igual en los dos temas. Borde y no anillo, porque
  // el anillo es lo que marca el elegido en "Armá tu hamburguesa".
  'bg-[radial-gradient(circle_at_30%_25%,#fbf2dc,#e0c994)] shadow-[inset_0_0_0_1.5px_rgb(255_255_255/0.4)] text-[#241a13] border border-[#241a13]/20',
  'bg-[radial-gradient(circle_at_30%_25%,#fbb4c2,#d9657f)] shadow-[inset_0_0_0_1.5px_rgb(255_255_255/0.4)] text-[#17110d]',
  'bg-[radial-gradient(circle_at_30%_25%,#d6c8f2,#8c72c6)] shadow-[inset_0_0_0_1.5px_rgb(255_255_255/0.4)] text-[#17110d]',
  'bg-[radial-gradient(circle_at_30%_25%,#fcc88a,#da7a22)] shadow-[inset_0_0_0_1.5px_rgb(255_255_255/0.4)] text-[#17110d]',
  'bg-[radial-gradient(circle_at_30%_25%,#c4f0dc,#5eb690)] shadow-[inset_0_0_0_1.5px_rgb(255_255_255/0.4)] text-[#17110d]',
  'bg-[radial-gradient(circle_at_30%_25%,#fcdcc2,#e8966a)] shadow-[inset_0_0_0_1.5px_rgb(255_255_255/0.4)] text-[#17110d]',
] as const

/** Los fondos que había antes de ampliarlos a diez (#370): de ellos sale el de quien no armó la suya. */
const FONDOS_DE_ORIGEN = 5

/** El pan va fijo y no sale del tema, igual que en el logo: es pan, no un color de la marca. */
export const PANES = {
  clasico: { relleno: '#cf8636', semillas: true },
  papa: { relleno: '#e9b860', semillas: false },
  negro: { relleno: '#4a3426', semillas: true },
} as const

export const QUESOS = { cheddar: '#f2b705', dambo: '#f6dd8f', sin: null } as const

export const VERDURAS = ['nada', 'lechuga', 'tomate', 'completa'] as const

/** El color de cada fondo, para lo que se dibuja encima (los rayos del nivel 15). Mismo orden que FONDOS. */
export const COLORES_DE_FONDOS = [
  '#d9503a',
  '#dc9a1e',
  '#5f9248',
  '#4f93ad',
  '#e0c994',
  '#d9657f',
  '#8c72c6',
  '#da7a22',
  '#5eb690',
  '#e8966a',
] as const

/**
 * El nivel en que se desbloquea cada premio de la hamburguesa (#313): el huevo frito, el
 * bacon, la cuarta carne y el fondo de rayos de impacto. Los mismos de `Premios.java`.
 */
export const NIVEL_DE = { huevo: 3, bacon: 5, cuartaCarne: 10, rayos: 15 } as const

export interface Receta {
  fondo: number
  pan: keyof typeof PANES
  queso: keyof typeof QUESOS
  verdura: (typeof VERDURAS)[number]
  carnes: 1 | 2 | 3 | 4
  huevo: boolean
  bacon: boolean
  rayos: boolean
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
    // De los cinco de origen y no de todos: sumar fondos no tiene que cambiarle la cara a quien
    // nunca eligió una, ni correr lo que sale después del fondo.
    fondo: elegir(FONDOS_DE_ORIGEN),
    pan: ORDEN_DE_PANES[elegir(ORDEN_DE_PANES.length)],
    // De los dos primeros. Sin queso existe, pero no sale solo: con sin queso, sin verdura y una carne, a uno
    // de cada treinta y seis le tocaba un pan con una carne y nada más.
    queso: ORDEN_DE_QUESOS[elegir(2)],
    verdura: VERDURAS[elegir(VERDURAS.length)],
    carnes: ([1, 2, 3] as const)[elegir(3)],
    // Los premios no salen del nombre: se ganan, y cada uno elige si los usa.
    huevo: false,
    bacon: false,
    rayos: false,
  }
}

/*
 * Cómo viaja una receta: cinco cifras, una por cosa y en este orden: fondo, pan, queso,
 * lo verde y carnes (de 1 a 4). Si usa algún premio, tres más al final: huevo, bacon y
 * fondo de rayos, 0 o 1. Es lo que guarda el servidor, que no sabe qué es cada número;
 * el orden de estas listas es el contrato, así que a una opción nueva se la agrega al
 * final, nunca en el medio.
 *
 * Sin premios se escriben las cinco cifras de siempre, así las pestañas viejas siguen
 * entendiendo el código.
 */
const ORDEN_DE_PANES = ['clasico', 'papa', 'negro'] as const
const ORDEN_DE_QUESOS = ['cheddar', 'dambo', 'sin'] as const
const CODIGO = /^([0-9])([0-2])([0-2])([0-3])([1-4])([01]{3})?$/

export function codigoDe(receta: Receta): string {
  const base = [
    receta.fondo,
    ORDEN_DE_PANES.indexOf(receta.pan),
    ORDEN_DE_QUESOS.indexOf(receta.queso),
    VERDURAS.indexOf(receta.verdura),
    receta.carnes,
  ].join('')
  if (!receta.huevo && !receta.bacon && !receta.rayos) return base
  return base + [receta.huevo, receta.bacon, receta.rayos].map((usa) => (usa ? 1 : 0)).join('')
}

/** Null si el código no es uno que se pueda dibujar. */
function recetaDeCodigo(codigo: string): Receta | null {
  const partes = CODIGO.exec(codigo)
  if (!partes) return null
  const [fondo, pan, queso, verdura, carnes] = partes.slice(1, 6).map(Number)
  const extras = partes[6] ?? '000'
  return {
    fondo,
    pan: ORDEN_DE_PANES[pan],
    queso: ORDEN_DE_QUESOS[queso],
    verdura: VERDURAS[verdura],
    carnes: carnes as Receta['carnes'],
    huevo: extras[0] === '1',
    bacon: extras[1] === '1',
    rayos: extras[2] === '1',
  }
}

/**
 * La hamburguesa sin los premios que este nivel todavía no alcanza (#313). El servidor ya
 * no deja guardar una bloqueada y recorta al bajar de nivel; esto es la defensa de acá,
 * por si llega una vieja: se dibuja lo que corresponde y no se rompe nada.
 */
export function recetaVisible(receta: Receta, nivel: number): Receta {
  return {
    ...receta,
    huevo: receta.huevo && nivel >= NIVEL_DE.huevo,
    bacon: receta.bacon && nivel >= NIVEL_DE.bacon,
    carnes: receta.carnes === 4 && nivel < NIVEL_DE.cuartaCarne ? 3 : receta.carnes,
    rayos: receta.rayos && nivel >= NIVEL_DE.rayos,
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
