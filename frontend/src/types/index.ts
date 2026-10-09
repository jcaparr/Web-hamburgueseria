export interface BurgerJoint {
  id: number
  /** El identificador del local en Google, para abrir su ficha en Maps. */
  placeId: string | null
  name: string
  address: string
  area: string | null
  /** El promedio de la hamburguesería, distinto de la nota de esta reseña. */
  promedioDelLocal: number | null
  photoUrl: string | null
  latitude: number | null
  longitude: number | null
  averageScore: number | null
  ratingsCount: number
  seguidores: number
  inWishlist: boolean
}

/**
 * Una cadena de comida rápida en el buscador de Explorar (#206): una tarjeta por cadena,
 * no una por sucursal.
 */
export interface Cadena {
  /** La clave, "mcdonalds": es la que va en la dirección de su página. */
  marca: string
  /** Cómo se escribe, "McDonald's". */
  nombre: string
  /** Cuántas hay en los barrios elegidos, o en total si no hay ninguno. */
  sucursales: number
  fotoUrl: string | null
}

/** La página de una cadena: todas sus sucursales, por barrio y después por nombre. */
export interface SucursalesDeCadena {
  marca: string
  nombre: string
  sucursales: BurgerJoint[]
}

/** Una parada de un recorrido, con lo que hay que caminar para llegar. */
export interface TourStop {
  orden: number
  kilometros: number
  visitada: boolean
  local: BurgerJoint
}

export interface Tour {
  paradas: TourStop[]
  kilometros: number
  minutos: number
  candidatos: number
  /** Qué no se pudo cumplir del pedido, o nulo si salió entero. */
  aviso: string | null
}

/** Un recorrido que alguien guardó en su perfil. */
export interface SavedTour {
  id: number
  name: string
  kilometros: number
  minutos: number
  modo: 'A_PIE' | 'EN_AUTO'
  creadoEl: string
  paradas: TourStop[]
}

export interface Rating {
  id: number
  userId: number
  username: string
  /** La hamburguesa de su avatar, en cinco cifras, o null si no eligió ninguna. */
  hamburguesa: string | null
  score: number
  comment: string | null
  /** Las fotos, en orden: la primera es la portada. */
  fotos: string[]
  createdAt: string
  reacciones: Reacciones
}

/** Las reacciones que se le pueden poner a una reseña (#186). */
export type TipoDeReaccion = 'HAMBRE' | 'FUEGO' | 'APLAUSO' | 'RISA' | 'SORPRESA'

/**
 * Un aviso del buzón (#210): alguien te siguió, o alguien reaccionó a una reseña tuya.
 * Los campos de cada tipo vienen vacíos en el otro.
 */
export interface Notificacion {
  tipo: 'SEGUIMIENTO' | 'REACCION'
  cuando: string
  /** Si llegó después de la última vez que se abrió el buzón. */
  nueva: boolean
  userId: number
  username: string
  hamburguesa: string | null
  /** Solo en un seguimiento: si ya la seguís, para el botón de seguir de vuelta. */
  loSigo: boolean
  reaccion: TipoDeReaccion | null
  localId: number | null
  localNombre: string | null
}

export interface Buzon {
  notificaciones: Notificacion[]
  nuevas: number
}

/** Las reacciones de una reseña: cuántas de cada una, y la de quien mira. */
export interface Reacciones {
  /** Solo las que alguien usó, de la más usada a la menos. */
  cuantas: { tipo: TipoDeReaccion; cuantas: number }[]
  /** La tuya, o null si no reaccionaste o no tenés sesión. */
  mia: TipoDeReaccion | null
}

/** Cuántas reseñas tiene una nota. */
export interface NotaYCuantas {
  nota: number
  cuantas: number
}

/** Un tema del que hablan las reseñas, y en cuántas de ellas la nota fue buena. */
export interface TemaDeResenias {
  tema: string
  menciones: number
  aFavor: number
}

/** Lo que va arriba de la lista de reseñas de un local. */
export interface ResumenDeResenias {
  /** Las cinco notas, del 1 al 5, con cero incluido. */
  distribucion: NotaYCuantas[]
  /** De qué habla la gente. Vacía mientras no haya reseñas escritas suficientes. */
  temas: TemaDeResenias[]
  /** Las reseñas de la gente que seguís. Vacía sin sesión. */
  deQuienesSigo: Rating[]
}

/**
 * Un tramo en que el local está abierto.
 *
 * El cierre se cuenta desde la misma medianoche que la apertura, así que pasa de 1440 si
 * cierra al día siguiente: un viernes de 19 a 1 es abre 1140 y cierra 1500.
 */
export interface FranjaHoraria {
  /** 0 es domingo y 6 es sábado. */
  dia: number
  /** Minutos desde la medianoche. */
  abre: number
  cierra: number
}

export interface Horario {
  /** Vacía si Google no tiene el horario o si todavía no se le preguntó. */
  franjas: FranjaHoraria[]
  consultado: boolean
}

export interface RankingItem {
  burgerJointId: number
  placeId: string | null
  name: string
  address: string
  area: string | null
  /** El promedio de la hamburguesería, distinto de la nota de esta reseña. */
  promedioDelLocal: number | null
  photoUrl: string | null
  latitude: number | null
  longitude: number | null
  averageScore: number
  ratingsCount: number
  seguidores: number
  myScore: number | null
}

/**
 * Una página de resultados, en el formato estable de Spring Data: la lista, y aparte
 * dónde está parada. Antes los datos de la página venían sueltos al lado de la lista, en
 * un formato que Spring no garantizaba y que una actualización podía cambiar (#103).
 */
export interface PageResponse<T> {
  content: T[]
  page: {
    size: number
    /** Desde cero. */
    number: number
    totalElements: number
    totalPages: number
  }
}

export interface User {
  userId: number
  /** El único nombre que tiene una cuenta. Único y siempre en minúsculas. */
  username: string
  /** La hamburguesa que eligió para su avatar, o null: se dibuja la de su nombre. */
  hamburguesa: string | null
  email: string
}

export interface ProfileStats {
  ratingsCount: number
  seguidores: number
  averageScore: number | null
}

export interface ReseniaDePerfil {
  id: number
  burgerJointId: number
  burgerJointName: string
  photoUrl: string | null
  score: number
  comment: string | null
  createdAt: string
}

/** Una persona en los resultados del buscador. */
export interface UsuarioBuscado {
  userId: number
  username: string
  hamburguesa: string | null
  resenias: number
  loSigo: boolean
}

/** El perfil de otra persona. Sin email: de otro solo se ve lo que eligió mostrar. */
export interface PerfilPublico {
  userId: number
  username: string
  hamburguesa: string | null
  resenias: number
  promedio: number | null
  seguidores: number
  siguiendo: number
  loSigo: boolean
  soyYo: boolean
  ultimasResenias: ReseniaDePerfil[]
}

/** Una reseña como se lee en el feed, con quién, dónde y qué dijo. */
export interface ItemDeFeed {
  ratingId: number
  autorId: number
  autorUsername: string
  autorHamburguesa: string | null
  burgerJointId: number
  burgerJointName: string
  photoUrl: string | null
  area: string | null
  /** El promedio de la hamburguesería, distinto de la nota de esta reseña. */
  promedioDelLocal: number | null
  score: number
  comment: string | null
  /** Las fotos de quien la escribió, en orden: la primera es la portada. */
  fotosDeLaResenia: string[]
  /** Cuándo se escribió, no cuándo se editó. */
  createdAt: string
  editada: boolean
  reacciones: Reacciones
}

/** Un tramo del feed. `siguiente` viene en null cuando no hay más. */
export interface PaginaDeFeed {
  items: ItemDeFeed[]
  siguiente: string | null
}

/** Alguien a quien bloqueaste. Solo aparece en la lista para desbloquearlo. */
export interface UsuarioBloqueado {
  userId: number
  username: string
  bloqueadoEl: string
}
