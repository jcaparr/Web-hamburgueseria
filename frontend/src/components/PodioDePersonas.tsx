import { useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'
import { usePedido } from '../hooks/usePedido'
import type { Podio, PuestoDelPodio } from '../types'
import { nivelDe } from '../utils/niveles'
import { AvatarDeUsuario } from './AvatarDeUsuario'
import { LoadError } from './LoadError'

const PERIODOS = [
  { id: 'semana', texto: 'Semana', vacio: 'Todavía nadie ganó XP esta semana. Reseñá algo y salí primero.' },
  { id: 'mes', texto: 'Mes', vacio: 'Todavía nadie ganó XP este mes. Reseñá algo y salí primero.' },
  { id: 'general', texto: 'General', vacio: 'Todavía no hay nadie en el podio.' },
] as const

/**
 * El podio de personas (#318): quién ganó más XP esta semana, este mes y en total. El
 * Ranking de la barra de abajo sigue siendo el de locales.
 *
 * Escalones: oro al centro (el más alto), plata a la izquierda y bronce a la derecha. Debajo,
 * la lista con avatar, nivel y XP del período. Tu fila va resaltada, aunque estés fuera del top.
 */
export function PodioDePersonas({ yoId }: { yoId: number }) {
  const [periodo, setPeriodo] = useState<(typeof PERIODOS)[number]['id']>('semana')
  const pedido = usePedido(`podio:${periodo}`, () => apiClient.get<Podio>('/podio', { params: { periodo } }).then((r) => r.data))
  const datos = pedido.datos
  const actual = PERIODOS.find((p) => p.id === periodo)!

  return (
    <div className="flex flex-col gap-4">
      <div role="radiogroup" aria-label="Período del podio" className="flex rounded-full bg-base-200 p-1">
        {PERIODOS.map((p) => (
          <label
            key={p.id}
            className={`relative flex min-h-11 flex-1 cursor-pointer items-center justify-center rounded-full text-sm font-semibold has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-primary ${
              periodo === p.id ? 'bg-base-100 shadow-[var(--sombra-tarjeta)]' : 'text-base-content/70'
            }`}
          >
            <input type="radio" name="periodo-del-podio" className="sr-only" checked={periodo === p.id} onChange={() => setPeriodo(p.id)} />
            {p.texto}
          </label>
        ))}
      </div>

      {pedido.error ? (
        <LoadError error={pedido.error} onRetry={pedido.reintentar} />
      ) : !datos ? (
        <p role="status" className="text-sm text-base-content/70">
          Cargando…
        </p>
      ) : datos.top.length === 0 ? (
        <p className="tarjeta p-4 text-sm text-base-content/80">{actual.vacio}</p>
      ) : (
        <>
          {!datos.yo && <p className="text-sm text-base-content/70">Todavía no sumaste XP en este período.</p>}
          <Escalones top={datos.top.slice(0, 3)} yoId={yoId} />
          <ListaDelPodio datos={datos} yoId={yoId} />
        </>
      )}
    </div>
  )
}

/** Oro al centro y más alto, plata a la izquierda, bronce a la derecha. */
function Escalones({ top, yoId }: { top: PuestoDelPodio[]; yoId: number }) {
  const orden = [top[1], top[0], top[2]]
  const alto = ['h-20', 'h-28', 'h-14']
  const color = ['bg-[#c9c3b5] text-[#3a2a1e]', 'bg-[#f2b705] text-[#3a2a1e]', 'bg-[#c98a3d] text-[#3a2a1e]']
  return (
    <ol className="flex items-end justify-center gap-2" aria-label="Los tres primeros">
      {orden.map((p, i) =>
        p ? (
          <li key={p.userId} className="flex w-1/3 max-w-32 flex-col items-center gap-1">
            <Link to={p.userId === yoId ? '/profile' : `/u/${p.username}`} className="flex min-w-0 max-w-full flex-col items-center gap-1">
              <AvatarDeUsuario username={p.username} hamburguesa={p.hamburguesa} xp={p.xp} size={i === 1 ? 56 : 48} />
              <span className="max-w-full truncate text-sm font-semibold">
                @{p.username}
                {p.userId === yoId && <span className="font-normal text-base-content/70"> (vos)</span>}
              </span>
            </Link>
            <span className="text-sm tabular-nums text-base-content/70">{p.xpDelPeriodo.toLocaleString('es-AR')} XP</span>
            <div className={`flex w-full items-start justify-center rounded-t-box pt-2 font-display text-2xl font-extrabold ${alto[i]} ${color[i]}`}>
              {p.puesto}
            </div>
          </li>
        ) : (
          <li key={`vacio-${i}`} className="w-1/3 max-w-32" aria-hidden />
        ),
      )}
    </ol>
  )
}

function ListaDelPodio({ datos, yoId }: { datos: Podio; yoId: number }) {
  const resto = datos.top.slice(3)
  const yoFuera = datos.yo && !datos.top.some((p) => p.userId === datos.yo!.userId) ? datos.yo : null
  if (resto.length === 0 && !yoFuera) return null
  return (
    <ul className="tarjeta px-3 py-1">
      {resto.map((p) => (
        <Fila key={p.userId} p={p} soyYo={p.userId === yoId} />
      ))}
      {yoFuera && (
        <>
          {resto.length > 0 && <li aria-hidden className="py-1 text-center text-base-content/50">⋯</li>}
          <Fila p={yoFuera} soyYo />
        </>
      )}
    </ul>
  )
}

function Fila({ p, soyYo }: { p: PuestoDelPodio; soyYo: boolean }) {
  return (
    <li className={`flex min-h-14 items-center gap-3 rounded-field px-1 py-2 ${soyYo ? 'bg-primary/10 font-semibold' : ''}`}>
      <span className="w-6 text-center text-sm tabular-nums text-base-content/70">{p.puesto}</span>
      <Link to={soyYo ? '/profile' : `/u/${p.username}`} className="flex min-w-0 flex-1 items-center gap-3">
        <AvatarDeUsuario username={p.username} hamburguesa={p.hamburguesa} xp={p.xp} size={40} />
        <span className="flex min-w-0 flex-col">
          <span className="truncate text-sm font-semibold">
            @{p.username}
            {soyYo && <span className="font-normal text-base-content/70"> (vos)</span>}
          </span>
          <span className="text-sm text-base-content/70">Nivel {nivelDe(p.xp)}</span>
        </span>
      </Link>
      <span className="text-sm font-semibold tabular-nums">{p.xpDelPeriodo.toLocaleString('es-AR')} XP</span>
    </li>
  )
}
