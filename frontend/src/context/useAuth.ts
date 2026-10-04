import { useContext } from 'react'
import { AuthContext } from './sesion'

/** Quién está adentro, y cómo entrar y salir. Solo funciona debajo de AuthProvider. */
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
