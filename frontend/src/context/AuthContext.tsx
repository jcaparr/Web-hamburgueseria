import { useEffect, useState, type ReactNode } from 'react'
import { apiClient, SESSION_EXPIRED_EVENT } from '../api/client'
import type { User } from '../types'
import { AuthContext } from './sesion'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  function saveSession(data: User) {
    // Only who the user is. The tokens are in cookies this code cannot read, which
    // is the point: a script injected into the page has nothing to steal.
    setUser({
      userId: data.userId,
      username: data.username,
      hamburguesa: data.hamburguesa ?? null,
      email: data.email,
    })
  }

  // El servidor contesta quién es, como al entrar, y con eso se reemplaza la sesión: el
  // avatar cambia en toda la app sin volver a preguntar.
  async function cambiarHamburguesa(receta: string | null) {
    const { data } = await apiClient.put('/profile/hamburguesa', { receta })
    saveSession(data)
  }

  async function login(email: string, password: string) {
    const { data } = await apiClient.post('/auth/login', { email, password })
    saveSession(data)
  }

  async function register(username: string, email: string, password: string): Promise<string> {
    const { data } = await apiClient.post('/auth/register', { username, email, password })
    return data.message as string
  }

  async function verifyEmail(email: string, code: string) {
    const { data } = await apiClient.post('/auth/verify-email', { email, code })
    saveSession(data)
  }

  async function loginWithGoogle(credential: string, username?: string) {
    const { data } = await apiClient.post('/auth/google', { credential, username })
    saveSession(data)
  }

  async function logout() {
    try {
      // The server has to be told: it is the only side that can revoke the refresh
      // token and clear the cookies.
      await apiClient.post('/auth/logout')
    } finally {
      setUser(null)
    }
  }

  // On load there is no way to look at the cookie from here, so the server is asked
  // who it belongs to. A 401 simply means nobody is signed in.
  useEffect(() => {
    let cancelled = false

    apiClient
      .get('/auth/me')
      .then(({ data }) => {
        if (!cancelled) saveSession(data)
      })
      .catch(() => {
        if (!cancelled) setUser(null)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    function handleSessionExpired() {
      setUser(null)
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired)
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired)
  }, [])

  return (
    <AuthContext.Provider
      value={{ user, loading, login, register, verifyEmail, loginWithGoogle, logout, cambiarHamburguesa }}
    >
      {children}
    </AuthContext.Provider>
  )
}
