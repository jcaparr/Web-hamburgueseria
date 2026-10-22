/** La dirección de un local: la linda si ya la tiene (#305), y si no la de siempre, con el id. */
export function rutaDelLocal(local: { id: number; slug: string | null }): string {
  return local.slug ? `/${local.slug}` : `/burger-joints/${local.id}`
}
