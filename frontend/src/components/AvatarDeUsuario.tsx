/**
 * La cara de alguien que todavía no tiene foto de perfil.
 *
 * Una inicial sobre un color sacado del propio nombre. No es decoración: en un feed
 * donde todas las tarjetas tienen la misma forma, el color es lo que deja reconocer de
 * un vistazo que dos publicaciones son de la misma persona, antes de leer el arroba.
 */
const COLORES = [
  'bg-primary text-primary-content',
  'bg-secondary text-secondary-content',
  'bg-accent text-accent-content',
  'bg-neutral text-secondary',
  'bg-info text-info-content',
  'bg-success text-success-content',
  'bg-warning text-warning-content',
  'bg-error text-error-content',
]

/**
 * El mismo nombre da siempre el mismo color.
 *
 * Suma de caracteres y no un azar: si cambiara entre pantallas —o entre dos cargas de
 * la misma— el color dejaría de servir para reconocer a nadie.
 */
function colorDe(username: string): string {
  let suma = 0
  for (let i = 0; i < username.length; i++) {
    suma += username.charCodeAt(i)
  }
  return COLORES[suma % COLORES.length]
}

export function AvatarDeUsuario({
  username,
  size = 40,
}: {
  username: string
  size?: number
}) {
  return (
    <div
      className={`flex flex-none items-center justify-center rounded-full font-display font-bold ${colorDe(username)}`}
      style={{ width: size, height: size, fontSize: size * 0.42 }}
      aria-hidden
    >
      {username.charAt(0).toUpperCase()}
    </div>
  )
}
