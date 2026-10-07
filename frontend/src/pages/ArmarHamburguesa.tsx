import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AvatarDeUsuario } from '../components/AvatarDeUsuario'
import { IconArrowLeft } from '../components/icons'
import { useAuth } from '../context/useAuth'
import { useTitulo } from '../hooks/useTitulo'
import { isSessionExpired } from '../utils/errors'
import { FONDOS, codigoDe, recetaPara, type Receta } from '../utils/recetas'

/**
 * Los nombres de los fondos, para el lector de pantalla: en pantalla son círculos de
 * color y nada más. Van por lo que se ve en una hamburguesería y no por el nombre del
 * color del tema, que en el oscuro es otro tono.
 */
const NOMBRES_DE_FONDOS = ['Kétchup', 'Mostaza', 'Pepino', 'Azul', 'Tinta']

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
  const [receta, setReceta] = useState<Receta>(() => recetaPara(user?.username ?? '', user?.hamburguesa))
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState(false)

  // RequireAuth ya lo garantiza; el compilador no lo ve.
  if (!user) return null

  function cambiar<K extends keyof Receta>(cosa: K, valor: Receta[K]) {
    setReceta((previa) => ({ ...previa, [cosa]: valor }))
    setError(false)
  }

  async function guardar(codigo: string | null) {
    setGuardando(true)
    setError(false)
    try {
      await cambiarHamburguesa(codigo)
      navigate('/profile')
    } catch (err) {
      if (isSessionExpired(err)) navigate('/login')
      else setError(true)
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
          <AvatarDeUsuario username={user.username} hamburguesa={codigoDe(receta)} size={168} />
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
            ]}
            onCambio={(valor) => cambiar('carnes', valor)}
          />

          {error && (
            <p role="alert" className="text-sm text-error">
              No pudimos guardar tu hamburguesa. Probá de nuevo.
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
  onCambio,
}: {
  titulo: string
  nombre: string
  valor: T
  opciones: { id: T; texto: string }[]
  onCambio: (valor: T) => void
}) {
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-sm font-semibold">{titulo}</legend>
      <div className="flex rounded-full bg-base-200 p-1">
        {opciones.map((opcion) => (
          <label
            key={opcion.id}
            className={`relative flex min-h-11 flex-1 cursor-pointer items-center justify-center rounded-full px-2 text-center text-sm font-semibold transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-primary ${
              valor === opcion.id
                ? 'bg-base-100 shadow-[var(--sombra-tarjeta)]'
                : 'text-base-content/70 hover:text-base-content'
            }`}
          >
            <input
              type="radio"
              name={nombre}
              value={opcion.id}
              checked={valor === opcion.id}
              onChange={() => onCambio(opcion.id)}
              className="sr-only"
            />
            {opcion.texto}
          </label>
        ))}
      </div>
    </fieldset>
  )
}
