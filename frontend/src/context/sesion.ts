import { createContext } from 'react'
import type { User } from '../types'

export interface AuthContextValue {
  user: User | null
  /** False until the session has been checked, so pages do not flash "signed out". */
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  /** Does not start a session: the account is unusable until the emailed code is entered. */
  register: (username: string, email: string, password: string) => Promise<string>
  verifyEmail: (email: string, code: string) => Promise<void>
  /**
   * Sin nombre de usuario la primera vez el servidor contesta NEEDS_USERNAME y no crea
   * nada: quien llama vuelve con el mismo credential y el nombre que la persona eligió.
   */
  loginWithGoogle: (credential: string, username?: string) => Promise<void>
  logout: () => Promise<void>
}

/**
 * El contexto de la sesión.
 *
 * Vive aparte de AuthContext.tsx, igual que useAuth, para que ese archivo exporte solo
 * el proveedor: la recarga en caliente de Vite funciona únicamente en los archivos que
 * exportan componentes y nada más, y con el hook al lado se recargaba la página entera
 * cada vez que se lo tocaba (#109).
 */
export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
