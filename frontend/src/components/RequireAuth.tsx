import { useEffect, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

/**
 * Keeps a page from rendering to someone who is not signed in.
 *
 * The waiting matters: with the session in a cookie, the app cannot tell who is
 * signed in until the server answers. Redirecting before that would throw a
 * perfectly valid session out to the login screen on every page load.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (!loading && !user) {
      navigate('/login', { replace: true })
    }
  }, [loading, user, navigate])

  if (loading || !user) {
    return null
  }

  return <>{children}</>
}
