import { createContext, useContext, useState, type ReactNode } from 'react'
import { apiClient } from '../api/client'
import type { Usuario } from '../types'

interface AuthContextValue {
  usuario: Usuario | null
  login: (email: string, password: string) => Promise<void>
  registrar: (nombre: string, email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function leerUsuarioGuardado(): Usuario | null {
  const raw = localStorage.getItem('usuario')
  return raw ? (JSON.parse(raw) as Usuario) : null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(leerUsuarioGuardado)

  function guardarSesion(data: { token: string; usuarioId: number; nombre: string; email: string }) {
    const usuarioActual: Usuario = { usuarioId: data.usuarioId, nombre: data.nombre, email: data.email }
    localStorage.setItem('token', data.token)
    localStorage.setItem('usuario', JSON.stringify(usuarioActual))
    setUsuario(usuarioActual)
  }

  async function login(email: string, password: string) {
    const { data } = await apiClient.post('/auth/login', { email, password })
    guardarSesion(data)
  }

  async function registrar(nombre: string, email: string, password: string) {
    const { data } = await apiClient.post('/auth/registro', { nombre, email, password })
    guardarSesion(data)
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('usuario')
    setUsuario(null)
  }

  return (
    <AuthContext.Provider value={{ usuario, login, registrar, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
