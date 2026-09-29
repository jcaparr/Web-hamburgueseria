export interface BurgerJoint {
  id: number
  /** El identificador del local en Google, para abrir su ficha en Maps. */
  placeId: string | null
  name: string
  address: string
  area: string | null
  photoUrl: string | null
  latitude: number | null
  longitude: number | null
  averageScore: number | null
  ratingsCount: number
  inWishlist: boolean
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
  score: number
  comment: string | null
  createdAt: string
}

export interface RankingItem {
  burgerJointId: number
  placeId: string | null
  name: string
  address: string
  area: string | null
  photoUrl: string | null
  latitude: number | null
  longitude: number | null
  averageScore: number
  ratingsCount: number
  myScore: number | null
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  last: boolean
}

export interface User {
  userId: number
  /** El único nombre que tiene una cuenta. Único y siempre en minúsculas. */
  username: string
  email: string
}

export interface ProfileStats {
  ratingsCount: number
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
  resenias: number
  loSigo: boolean
}

/** El perfil de otra persona. Sin email: de otro solo se ve lo que eligió mostrar. */
export interface PerfilPublico {
  userId: number
  username: string
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
  burgerJointId: number
  burgerJointName: string
  photoUrl: string | null
  area: string | null
  score: number
  comment: string | null
  /** Cuándo se escribió, no cuándo se editó. */
  createdAt: string
  editada: boolean
}

/** Un tramo del feed. `siguiente` viene en null cuando no hay más. */
export interface PaginaDeFeed {
  items: ItemDeFeed[]
  siguiente: string | null
}
