import { useTitulo } from '../hooks/useTitulo'

/**
 * La pantalla de Logros. Por ahora vacía: el ícono de la barra ya lleva hasta acá, y la
 * grilla con lo que fuiste ganando llega en #316.
 */
export function Logros() {
  useTitulo('Logros')
  return (
    <div className="flex flex-col gap-3 p-4 md:mx-auto md:max-w-3xl md:p-0 md:pt-6">
      <h1 className="titulo-pagina">Logros</h1>
      <p className="text-sm text-base-content/70">Pronto vas a ver acá todo lo que vayas ganando.</p>
    </div>
  )
}
