/**
 * Las pestañas de un perfil (#264): las mismas en el propio y en el de otro. Aparte de
 * sus componentes para que el recargado en caliente de Vite siga andando.
 */
export type Pestania = 'resenias' | 'guardadas' | 'recorridos'

export const PESTANIAS: { id: Pestania; texto: string }[] = [
  { id: 'resenias', texto: 'Reseñas' },
  { id: 'guardadas', texto: 'Guardadas' },
  { id: 'recorridos', texto: 'Recorridos' },
]

/** La pestaña que pide la dirección (`?ver=`), o Reseñas si no pide una que exista. */
export function pestaniaDe(valor: string | null): Pestania {
  return PESTANIAS.find((p) => p.id === valor)?.id ?? 'resenias'
}

/** El id del panel que controlan las pestañas: el contenido va en un `role="tabpanel"` con este id. */
export const PANEL_DEL_PERFIL = 'contenido-del-perfil'
