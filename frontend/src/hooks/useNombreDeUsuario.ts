import { useState } from 'react'
import { apiClient } from '../api/client'

/**
 * El campo del nombre de usuario, que aparece en dos lugares.
 *
 * Se pide al registrarse con contraseña y al entrar con Google por primera vez. Son
 * dos pantallas distintas con las mismas reglas, así que el estado del campo —lo
 * escrito, si está tomado, qué proponer en su lugar— vive acá una sola vez.
 */
export function useNombreDeUsuario(inicial = '') {
  const [valor, setValor] = useState(inicial)
  const [problema, setProblema] = useState<string | null>(null)
  const [sugerencia, setSugerencia] = useState('')
  const [chequeando, setChequeando] = useState(false)

  /**
   * Lo que se escribe se normaliza al toque, en vez de rechazarlo después.
   *
   * Que el campo muestre exactamente lo que se va a guardar evita la sorpresa de
   * escribir "JuanCa" y terminar siendo "juanca" sin que nada lo haya dicho.
   */
  function escribir(texto: string) {
    setValor(texto.toLowerCase().replace(/[^a-z0-9_]/g, '').slice(0, 20))
    setProblema(null)
    setSugerencia('')
  }

  function usarSugerencia() {
    setValor(sugerencia)
    setProblema(null)
    setSugerencia('')
  }

  /**
   * Se pregunta al salir del campo, no en cada tecla.
   *
   * El endpoint cuelga de /api/auth/, donde el servidor limita cuántas veces puede
   * llamar una misma dirección; una consulta por tecla se comería ese presupuesto
   * antes de terminar de escribir el nombre.
   */
  async function chequear() {
    if (valor.length < 3) return

    setChequeando(true)
    try {
      const { data } = await apiClient.get('/auth/username-available', {
        params: { username: valor },
      })
      if (!data.available) {
        setProblema('Ese nombre ya está en uso')
        setSugerencia(data.suggestion ?? '')
      }
    } catch {
      // Que no se pueda consultar no es razón para trabar el formulario: el servidor
      // vuelve a chequearlo al mandarlo, que es donde de verdad importa.
    } finally {
      setChequeando(false)
    }
  }

  /**
   * Para el catch de quien manda el formulario.
   *
   * @return si el error era sobre este campo. Cuando lo era, el cartel general no
   *   tiene que mostrarlo: ya quedó marcado acá, que es donde se arregla.
   */
  function rechazar(err: any): boolean {
    const data = err?.response?.data
    if (data?.code !== 'USERNAME_TAKEN') return false

    setProblema(data.error ?? 'Ese nombre ya está en uso')
    return true
  }

  return { valor, escribir, chequear, problema, sugerencia, usarSugerencia, chequeando, rechazar }
}

export type CampoDeNombre = ReturnType<typeof useNombreDeUsuario>
