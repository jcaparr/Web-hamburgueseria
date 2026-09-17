export interface BurgerJoint {
  id: number
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
