import { useEffect, useRef } from 'react'

/**
 * Renders Google's own sign-in button.
 *
 * It has to be Google's: the button is rendered by their script inside an iframe, and
 * a look-alike of our own cannot produce the credential. The script is loaded here
 * rather than in index.html so pages that never sign in do not pay for it.
 */

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string
            callback: (response: { credential: string }) => void
          }) => void
          renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void
        }
      }
    }
  }
}

const SCRIPT_SRC = 'https://accounts.google.com/gsi/client'
const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined

/**
 * Shared across every caller and every mount.
 *
 * Checking for the script tag instead would resolve the moment the tag exists, which
 * is well before it has run: React mounts effects twice in development, and the
 * second mount would find the tag the first one just added, resolve immediately and
 * then find no `window.google` to call.
 */
let scriptPromise: Promise<void> | null = null

function loadScript(): Promise<void> {
  if (window.google?.accounts?.id) {
    return Promise.resolve()
  }
  if (scriptPromise) {
    return scriptPromise
  }

  scriptPromise = new Promise<void>((resolve, reject) => {
    const script =
      (document.querySelector(`script[src="${SCRIPT_SRC}"]`) as HTMLScriptElement | null) ??
      Object.assign(document.createElement('script'), { src: SCRIPT_SRC, async: true })

    script.addEventListener('load', () => resolve())
    script.addEventListener('error', () => {
      // Cleared so a later attempt can retry instead of reusing a rejected promise.
      scriptPromise = null
      reject(new Error('No se pudo cargar el script de Google'))
    })

    if (!script.isConnected) {
      document.head.appendChild(script)
    }
  })

  return scriptPromise
}

export function GoogleSignInButton({
  onCredential,
  onError,
  text = 'continue_with',
}: {
  onCredential: (credential: string) => void
  onError: (message: string) => void
  /** Google's own wording options, so the button reads right on each screen. */
  text?: 'continue_with' | 'signin_with' | 'signup_with'
}) {
  const container = useRef<HTMLDivElement>(null)
  // Kept in a ref so re-renders do not re-initialise Google's script with a stale
  // callback, which would leave the button posting the credential nowhere.
  const callback = useRef(onCredential)
  callback.current = onCredential

  useEffect(() => {
    // Without a client id the feature is off; showing a button that cannot work is
    // worse than showing none.
    if (!CLIENT_ID || !container.current) return

    let cancelled = false
    loadScript()
      .then(() => {
        if (cancelled || !container.current || !window.google) return
        window.google.accounts.id.initialize({
          client_id: CLIENT_ID,
          callback: (response) => callback.current(response.credential),
        })
        window.google.accounts.id.renderButton(container.current, {
          theme: 'outline',
          size: 'large',
          width: 320,
          text,
          locale: 'es-419',
        })
      })
      .catch(() => {
        if (!cancelled) onError('No pudimos cargar el ingreso con Google')
      })

    return () => {
      cancelled = true
    }
    // onError is intentionally not a dependency: it would re-run this on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [text])

  if (!CLIENT_ID) return null

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-3">
        <span className="h-px flex-1 bg-base-300" />
        <span className="text-xs text-base-content/50">o</span>
        <span className="h-px flex-1 bg-base-300" />
      </div>
      <div ref={container} className="flex justify-center" />
    </div>
  )
}
