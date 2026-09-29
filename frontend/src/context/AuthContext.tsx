import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { apiClient, SESSION_EXPIRED_EVENT } from '../api/client'
import type { User } from '../types'

interface AuthContextValue {
  user: User | null
  /** False until the session has been checked, so pages do not flash "signed out". */
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  /** Does not start a session: the account is unusable until the emailed code is entered. */
  register: (name: string, username: string, email: string, password: string) => Promise<string>
  verifyEmail: (email: string, code: string) => Promise<void>
  /**
   * Sin nombre de usuario la primera vez el servidor contesta NEEDS_USERNAME y no crea
   * nada: quien llama vuelve con el mismo credential y el nombre que la persona eligió.
   */
  loginWithGoogle: (credential: string, username?: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  function saveSession(data: User) {
    // Only who the user is. The tokens are in cookies this code cannot read, which
    // is the point: a script injected into the page has nothing to steal.
    setUser({
      userId: data.userId,
      name: data.name,
      username: data.username,
      email: data.email,
    })
  }

  async function login(email: string, password: string) {
    const { data } = await apiClient.post('/auth/login', { email, password })
    saveSession(data)
  }

  async function register(
    name: string,
    username: string,
    email: string,
    password: string,
  ): Promise<string> {
    const { data } = await apiClient.post('/auth/register', { name, username, email, password })
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
      value={{ user, loading, login, register, verifyEmail, loginWithGoogle, logout }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
