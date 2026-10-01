import axios from 'axios'

export const SESSION_EXPIRED_EVENT = 'auth:session-expired'

/**
 * The session lives in cookies the browser keeps away from JavaScript, so there is
 * no token to attach here: `withCredentials` is what sends it.
 */
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
  withCredentials: true,
  // Un parámetro con varios valores se repite tal cual: "?area=Palermo&area=Boedo".
  //
  // Axios por omisión le agrega corchetes —"area[]=Palermo&area[]=Boedo"— y Spring no
  // lo reconoce: busca "area" y no encuentra nada, así que el filtro se ignora en
  // silencio. No falla, no avisa; simplemente no filtra.
  paramsSerializer: { indexes: null },
})

/** Endpoints where a 401 is the answer, not a sign the session ended. */
const NO_REFRESH = ['/auth/login', '/auth/refresh', '/auth/google', '/auth/verify-email']

let refreshing: Promise<void> | null = null

function refreshSession(): Promise<void> {
  // Shared between callers: several requests failing at once must trigger one
  // refresh, not one each. Rotation would reject all but the first anyway, and
  // that looks exactly like a stolen token, which logs the person out everywhere.
  if (!refreshing) {
    refreshing = apiClient
      .post('/auth/refresh')
      .then(() => undefined)
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config
    const status = error.response?.status

    const shouldRetry =
      status === 401 &&
      config &&
      !config._retried &&
      !NO_REFRESH.some((path) => config.url?.startsWith(path))

    if (shouldRetry) {
      config._retried = true
      try {
        // The access token is short lived by design, so an expired one is routine.
        // Renew it once and replay the request; the user sees nothing.
        await refreshSession()
        return apiClient(config)
      } catch {
        window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
      }
    }

    return Promise.reject(error)
  },
)
