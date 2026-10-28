import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AvatarDeUsuario } from '../components/AvatarDeUsuario'
import { IconArrowLeft } from '../components/icons'
import { useAuth } from '../context/useAuth'
import { useTitulo } from '../hooks/useTitulo'
import { isSessionExpired } from '../utils/errors'
import { IconLock } from '../components/icons'
import { nivelDe } from '../utils/niveles'
import { FONDOS, NIVEL_DE, codigoDe, recetaPara, recetaVisible, type Receta } from '../utils/recetas'

/**
 * Los nombres de los fondos, para el lector de pantalla: en pantalla son círculos de
 * color y nada más. Van por lo que se ve en una hamburguesería y no por el nombre del
 * color del tema. El último era "Tinta" cuando en el tema claro salía marrón; desde
 * #195 es crema en los dos.
 */
const NOMBRES_DE_FONDOS = ['Kétchup', 'Mostaza', 'Pepino', 'Azul', 'Crema', 'Frutilla', 'Uva', 'Naranja', 'Menta', 'Durazno']

/**
 * Donde cada uno arma la hamburguesa de su avatar (#151).
 *
 * Una pantalla aparte y no un desplegable en Ajustes: la gracia es ver cómo cambia
 * mientras se elige, y eso pide una vista previa grande arriba, siempre a la vista.
 *
 * Arranca con la que tiene ahora, sea la que eligió o la que sale de su nombre: así
 * cambiar el pan no obliga a rearmar todo lo demás.
 */
export function ArmarHamburguesa() {
  const { user, cambiarHamburguesa } = useAuth()
  useTitulo('Tu hamburguesa')
  const navigate = useNavigate()
  const nivel = nivelDe(user?.xp ?? 0)
  const [receta, setReceta] = useState<Receta>(() =>
    recetaVisible(recetaPara(user?.username ?? '', user?.hamburguesa), nivel),
  )
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // RequireAuth ya lo garantiza; el compilador no lo ve.
  if (!user) return null

  function cambiar<K extends keyof Receta>(cosa: K, valor: Receta[K]) {
    setReceta((previa) => ({ ...previa, [cosa]: valor }))
    setError(null)
  }

  async function guardar(codigo: string | null) {
    setGuardando(true)
    setError(null)
    try {
      await cambiarHamburguesa(codigo)
      navigate('/profile')
    } catch (err) {
      if (isSessionExpired(err)) navigate('/login')
      // El servidor dice cuál premio todavía no está ganado; sin eso, el aviso de siempre.
      else setError((err as { response?: { data?: { error?: string } } }).response?.data?.error ?? 'No pudimos guardar tu hamburguesa. Probá de nuevo.')
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div className="flex flex-col gap-5 p-4 md:mx-auto md:max-w-3xl md:p-0 md:pt-6">
      <div className="flex flex-col gap-1">
        <Link
          to="/profile"
          className="-ml-1 inline-flex min-h-11 w-fit items-center gap-1.5 rounded-field px-1 text-sm font-semibold text-base-content/70 hover:text-base-content"
        >
          <IconArrowLeft size={16} />
          Tu perfil
        </Link>
        <h1 className="titulo-pagina">Tu hamburguesa</h1>
        <p className="text-sm text-base-content/70">
          Es tu cara en Burgómetro: la ven los demás en tus reseñas, en el feed y en tu perfil.
        </p>
      </div>

      <div className="flex flex-col gap-5 md:flex-row md:items-start md:gap-8">
        {/* La vista previa queda arriba en el teléfono y fija a la izquierda en la compu:
            lo que se elige abajo se tiene que ver cambiar sin correr la pantalla. */}
        <div className="flex flex-col items-center gap-3 tarjeta p-5 md:sticky md:top-24">
          <AvatarDeUsuario username={user.username} hamburguesa={codigoDe(receta)} xp={user.xp} size={168} />
          <span className="font-display text-lg font-bold">@{user.username}</span>
        </div>

        <div className="flex min-w-0 flex-1 flex-col gap-5">
          <fieldset className="flex flex-col gap-2">
            <legend className="mb-2 text-sm font-semibold">Fondo</legend>
            <div className="flex flex-wrap gap-3">
              {FONDOS.map((clases, indice) => (
                <label
                  key={clases}
                  className={`relative size-11 cursor-pointer rounded-full ring-offset-2 ring-offset-base-200 transition-shadow has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-4 has-[:focus-visible]:outline-primary ${clases} ${
                    receta.fondo === indice ? 'ring-3 ring-base-content' : 'ring-1 ring-base-content/15'
                  }`}
                >
                  <input
                    type="radio"
                    name="fondo"
                    value={indice}
                    checked={receta.fondo === indice}
                    onChange={() => cambiar('fondo', indice)}
                    className="sr-only"
                  />
                  <span className="sr-only">{NOMBRES_DE_FONDOS[indice]}</span>
                </label>
              ))}
            </div>
          </fieldset>

          <Eleccion
            titulo="Pan"
            nombre="pan"
            valor={receta.pan}
            opciones={[
              { id: 'clasico', texto: 'Clásico' },
              { id: 'papa', texto: 'De papa' },
              { id: 'negro', texto: 'Negro' },
            ]}
            onCambio={(valor) => cambiar('pan', valor)}
          />
          <Eleccion
            titulo="Queso"
            nombre="queso"
            valor={receta.queso}
            opciones={[
              { id: 'cheddar', texto: 'Cheddar' },
              { id: 'dambo', texto: 'Dambo' },
              { id: 'sin', texto: 'Sin queso' },
            ]}
            onCambio={(valor) => cambiar('queso', valor)}
          />
          <Eleccion
            titulo="Verdura"
            nombre="verdura"
            valor={receta.verdura}
            opciones={[
              { id: 'nada', texto: 'Nada' },
              { id: 'lechuga', texto: 'Lechuga' },
              { id: 'tomate', texto: 'Tomate' },
              { id: 'completa', texto: 'Las dos' },
            ]}
            onCambio={(valor) => cambiar('verdura', valor)}
          />
          <Eleccion
            titulo="Carne"
            nombre="carnes"
            valor={receta.carnes}
            opciones={[
              { id: 1, texto: 'Simple' },
              { id: 2, texto: 'Doble' },
              { id: 3, texto: 'Triple' },
              { id: 4, texto: 'Cuádruple', desbloqueaEn: NIVEL_DE.cuartaCarne },
            ]}
            nivel={nivel}
            onCambio={(valor) => cambiar('carnes', valor)}
          />

          <ExtrasDeNivel
            receta={receta}
            nivel={nivel}
            onCambio={(extra, valor) => cambiar(extra, valor)}
          />

          {error && (
            <p role="alert" className="text-sm text-error">
              {error}
            </p>
          )}

          <div className="flex flex-col gap-2 md:flex-row md:items-center">
            <button
              type="button"
              onClick={() => guardar(codigoDe(receta))}
              disabled={guardando}
              className="btn btn-primary md:px-8"
            >
              {guardando && <span className="loading loading-spinner loading-sm" aria-hidden />}
              Guardar
            </button>
            {/* Solo si eligió una: si no, la que sale de su nombre es la que ya tiene. */}
            {user.hamburguesa && (
              <button type="button" onClick={() => guardar(null)} disabled={guardando} className="btn btn-ghost">
                Volver a la de tu nombre
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

/**
 * Una elección entre pocas opciones, como botones pegados: la misma forma que la del
 * tema en Ajustes. Por dentro son radios, así que las flechas pasan de una a otra y el
 * lector de pantalla dice cuál está elegida.
 */
function Eleccion<T extends string | number>({
  titulo,
  nombre,
  valor,
  opciones,
  nivel = 99,
  onCambio,
}: {
  titulo: string
  nombre: string
  valor: T
  /** `desbloqueaEn`: el nivel que hay que tener; con menos, la opción se ve con candado. */
  opciones: { id: T; texto: string; desbloqueaEn?: number }[]
  nivel?: number
  onCambio: (valor: T) => void
}) {
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-sm font-semibold">{titulo}</legend>
      <div className="flex rounded-full bg-base-200 p-1">
        {opciones.map((opcion) => {
          const bloqueada = opcion.desbloqueaEn !== undefined && nivel < opcion.desbloqueaEn
          return (
            <label
              key={opcion.id}
              className={`relative flex min-h-11 flex-1 items-center justify-center gap-1 rounded-full px-2 text-center text-sm font-semibold transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-primary ${
                bloqueada
                  ? 'cursor-not-allowed text-base-content/70'
                  : valor === opcion.id
                    ? 'cursor-pointer bg-base-100 shadow-[var(--sombra-tarjeta)]'
                    : 'cursor-pointer text-base-content/70 hover:text-base-content'
              }`}
            >
              <input
                type="radio"
                name={nombre}
                value={opcion.id}
                checked={valor === opcion.id}
                disabled={bloqueada}
                onChange={() => onCambio(opcion.id)}
                className="sr-only"
              />
              {bloqueada && <IconLock size={14} />}
              {opcion.texto}
            </label>
          )
        })}
      </div>
      {opciones.some((o) => o.desbloqueaEn !== undefined && nivel < o.desbloqueaEn) && (
        <p className="text-sm text-base-content/70">
          {opciones
            .filter((o) => o.desbloqueaEn !== undefined && nivel < o.desbloqueaEn)
            .map((o) => `${o.texto}: se desbloquea en el nivel ${o.desbloqueaEn}`)
            .join('. ')}
        </p>
      )}
    </fieldset>
  )
}

/**
 * Los premios que da el nivel (#313): el huevo frito, el bacon y el fondo de rayos. Cada
 * uno se ve siempre; sin el nivel, con candado y la leyenda de cuándo se desbloquea. Es
 * elegir si se usa, no obligación: un premio ganado se puede dejar apagado.
 */
function ExtrasDeNivel({
  receta,
  nivel,
  onCambio,
}: {
  receta: Receta
  nivel: number
  onCambio: (extra: 'huevo' | 'bacon' | 'rayos', valor: boolean) => void
}) {
  const extras = [
    { id: 'huevo', texto: 'Huevo frito', nivel: NIVEL_DE.huevo },
    { id: 'bacon', texto: 'Bacon', nivel: NIVEL_DE.bacon },
    { id: 'rayos', texto: 'Fondo de rayos', nivel: NIVEL_DE.rayos },
  ] as const
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-sm font-semibold">Extras</legend>
      <div className="flex flex-col gap-2">
        {extras.map((extra) => {
          const bloqueado = nivel < extra.nivel
          return (
            <label
              key={extra.id}
              className={`flex min-h-11 items-center justify-between gap-3 rounded-field bg-base-200 px-3 text-sm font-semibold has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-primary ${
                bloqueado ? 'cursor-not-allowed' : 'cursor-pointer'
              }`}
            >
              <span className="flex items-center gap-2">
                {bloqueado && <IconLock size={14} />}
                {extra.texto}
              </span>
              {bloqueado ? (
                <span id={`desbloqueo-${extra.id}`} className="text-sm font-normal text-base-content/70">
                  Se desbloquea en el nivel {extra.nivel}
                </span>
              ) : null}
              <input
                type="checkbox"
                checked={!bloqueado && receta[extra.id]}
                disabled={bloqueado}
                aria-describedby={bloqueado ? `desbloqueo-${extra.id}` : undefined}
                onChange={(e) => onCambio(extra.id, e.target.checked)}
                className={`checkbox checkbox-primary ${bloqueado ? 'sr-only' : ''}`}
              />
            </label>
          )
        })}
      </div>
    </fieldset>
  )
}
