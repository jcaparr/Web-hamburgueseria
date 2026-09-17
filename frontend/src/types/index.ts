export interface Hamburgueseria {
  id: number
  nombre: string
  direccion: string
  zona: string | null
  fotoUrl: string | null
  latitud: number | null
  longitud: number | null
  promedio: number | null
  cantidadCalificaciones: number
  enListaDeseados: boolean
}

export interface Calificacion {
  id: number
  usuarioId: number
  usuarioNombre: string
  puntaje: number
  comentario: string | null
  fecha: string
}

export interface RankingItem {
  hamburgueseriaId: number
  nombre: string
  direccion: string
  zona: string | null
  fotoUrl: string | null
  promedio: number
  cantidadCalificaciones: number
  miPuntaje: number | null
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  last: boolean
}

export interface Usuario {
  usuarioId: number
  nombre: string
  email: string
}
