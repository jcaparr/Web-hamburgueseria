import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { apiClient, SESSION_EXPIRED_EVENT } from '../api/client'
import type { User } from '../types'

interface AuthContextValue {
  user: User | null
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function readStoredUser(): User | null {
  const raw = localStorage.getItem('user')
  return raw ? (JSON.parse(raw) as User) : null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(readStoredUser)

  function saveSession(data: { token: string; userId: number; name: string; email: string }) {
    const currentUser: User = { userId: data.userId, name: data.name, email: data.email }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(currentUser))
    setUser(currentUser)
  }

  async function login(email: string, password: string) {
    const { data } = await apiClient.post('/auth/login', { email, password })
    saveSession(data)
  }

  async function register(name: string, email: string, password: string) {
    const { data } = await apiClient.post('/auth/register', { name, email, password })
    saveSession(data)
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }

  useEffect(() => {
    function handleSessionExpired() {
      setUser(null)
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired)
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
