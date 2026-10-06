import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import type { BurgerJoint } from '../types'
import { BotonSobreFoto } from './BotonSobreFoto'
import { IconArrowLeft, IconHeart, IconShare } from './icons'
import { JointPhoto } from './JointPhoto'

/**
 * La foto del local a todo el ancho, con volver, compartir y guardar encima.
 *
 * La foto es lo que hace que alguien quiera ir: en la ficha vieja era una tira de 190
 * píxeles de alto al lado del texto, más chica que en la tarjeta de Explorar desde la
 * que se llegaba. Los botones van encima porque es donde se los busca en cualquier
 * aplicación de lugares, y así no le quitan lugar al nombre.
 */
export function PortadaDelLocal({
  local,
  onGuardar,
}: {
  local: BurgerJoint
  onGuardar: () => void
}) {
  const navigate = useNavigate()
  const location = useLocation()
  const [copiado, setCopiado] = useState(false)

  useEffect(() => {
    if (!copiado) return
    const reloj = setTimeout(() => setCopiado(false), 2500)
    return () => clearTimeout(reloj)
  }, [copiado])

  // Si se llegó desde la app, volver es lo mismo que el botón del navegador: Explorar
  // recuerda dónde estaba la lista. Si se entró directo por un enlace compartido no
  // hay a dónde volver dentro de la app, y se va al inicio en vez de salir de ella.
  function volver() {
    if (location.key !== 'default') navigate(-1)
    else navigate('/')
  }

  async function compartir() {
    const url = `${window.location.origin}/burger-joints/${local.id}`
    if (navigator.share) {
      try {
        await navigator.share({ title: local.name, text: `${local.name} en Burgómetro`, url })
      } catch {
        // Cerrar el menú de compartir también llega acá, y no es un error.
      }
      return
    }
    try {
      await navigator.clipboard.writeText(url)
      setCopiado(true)
    } catch {
      // Sin portapapeles no hay nada más que hacer: el enlace está en la barra.
    }
  }

  return (
    <div className="relative -mx-4 -mt-4 md:mx-0 md:mt-0">
      <JointPhoto
        prioritaria
        src={local.photoUrl}
        name={local.name}
        className="aspect-[4/3] w-full object-cover md:aspect-[21/9] md:rounded-box"
      />

      <div className="absolute inset-x-0 top-0 flex items-start justify-between p-3">
        <BotonSobreFoto onClick={volver} aria-label="Volver">
          <IconArrowLeft size={20} />
        </BotonSobreFoto>

        <div className="flex items-center gap-2">
          <BotonSobreFoto onClick={compartir} aria-label="Compartir">
            <IconShare size={19} />
          </BotonSobreFoto>
          <BotonSobreFoto
            onClick={onGuardar}
            aria-pressed={local.inWishlist}
            aria-label={local.inWishlist ? 'Quitar de guardadas' : 'Guardar'}
            className={local.inWishlist ? 'text-primary' : ''}
          >
            <IconHeart size={20} filled={local.inWishlist} />
          </BotonSobreFoto>
        </div>
      </div>

      <p
        role="status"
        className={`pointer-events-none absolute right-3 top-16 rounded-full bg-neutral px-3 py-1.5 text-xs font-semibold text-neutral-content shadow-md transition-opacity motion-reduce:transition-none ${
          copiado ? 'opacity-100' : 'opacity-0'
        }`}
      >
        {copiado ? 'Enlace copiado' : ''}
      </p>
    </div>
  )
}
