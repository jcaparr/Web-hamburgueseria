import { useState, type FormEvent } from 'react'
import { CampoNombreDeUsuario } from './CampoNombreDeUsuario'
import { useNombreDeUsuario } from '../hooks/useNombreDeUsuario'
import type { GoogleSignIn } from '../hooks/useGoogleSignIn'

/**
 * Lo único que Google no puede contestar por la persona.
 *
 * Aparece una sola vez, la primera que entra. Hasta que mande este formulario la
 * cuenta no existe, así que salir de acá no deja nada a medio hacer.
 */
export function PasoNombreDeGoogle({ google }: { google: GoogleSignIn }) {
  const campo = useNombreDeUsuario(google.pendiente?.sugerencia ?? '')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      await google.confirmarNombre(campo.valor)
    } catch (err: any) {
      // Si el problema es el nombre, queda marcado en el campo y no hace falta cartel.
      if (!campo.rechazar(err)) {
        setError(err.response?.data?.error ?? 'No pudimos crear tu cuenta')
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="flex flex-col gap-4 p-4 md:mx-auto md:max-w-sm md:p-0 md:pt-8">
      <div className="card card-border md:p-2">
        <div className="card-body gap-3">
          <h1 className="card-title font-display">Elegí tu nombre de usuario</h1>
          <p className="text-sm text-base-content/60">
            Es lo último que falta. Con esto te van a encontrar tus amigos.
          </p>

          <form onSubmit={onSubmit} className="flex flex-col gap-3">
            <CampoNombreDeUsuario campo={campo} autoFocus />
            {error && <p className="text-xs text-error">{error}</p>}
            <button
              type="submit"
              disabled={enviando || campo.chequeando}
              className="btn btn-primary"
            >
              {enviando ? 'Creando...' : 'Crear cuenta'}
            </button>
          </form>

          <button type="button" onClick={google.cancelar} className="btn btn-ghost btn-sm">
            Cancelar
          </button>
        </div>
      </div>
    </div>
  )
}
