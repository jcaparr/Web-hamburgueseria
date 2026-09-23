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

export interface Rating {
  id: number
  userId: number
  userName: string
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
  name: string
  email: string
}

export interface ProfileStats {
  ratingsCount: number
  averageScore: number | null
}

export interface MyRating {
  id: number
  burgerJointId: number
  burgerJointName: string
  photoUrl: string | null
  score: number
  comment: string | null
  createdAt: string
}
