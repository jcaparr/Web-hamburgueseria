/**
 * Las estadísticas de visitas: cuánta gente entra y qué pantallas mira.
 *
 * Las cuenta Umami, que corre en nuestro propio servidor: nadie de afuera se entera de
 * quién entra, el mismo criterio por el que las tipografías se sirven desde acá. No usa
 * cookies ni guarda la IP. El script y el envío pasan por /e/ en nuestro dominio, así
 * que la política de seguridad no tiene que abrirle la puerta a ningún tercero.
 *
 * Solo se carga si el build trae el id del sitio (VITE_UMAMI_WEBSITE_ID): en desarrollo
 * no hay, y no se cuenta nada.
 */
export function contarVisitas() {
  const sitio = import.meta.env.VITE_UMAMI_WEBSITE_ID
  if (!sitio) return

  const script = document.createElement('script')
  script.src = '/e/script.js'
  script.defer = true
  script.dataset.websiteId = sitio
  // Lo que va después del "?" puede ser lo que alguien escribió en el buscador, y lo
  // que va después del "#" no es de nadie más. Se cuenta la página y nada más.
  script.dataset.excludeSearch = 'true'
  script.dataset.excludeHash = 'true'
  // Quien pidió "No rastrear" en el navegador no se cuenta.
  script.dataset.doNotTrack = 'true'
  document.head.appendChild(script)
}
