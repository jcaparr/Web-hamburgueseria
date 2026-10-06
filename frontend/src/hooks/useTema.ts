import { useState } from 'react'

/** Automático sigue al sistema; claro y oscuro lo fijan. */
export type Tema = 'auto' | 'claro' | 'oscuro'

const CLAVE = 'tema'
const NOMBRES = { claro: 'burger', oscuro: 'burger-noche' } as const
/** El color de la barra del navegador en el teléfono: el fondo de la página de cada tema. */
const BARRAS = { claro: '#faf3e3', oscuro: '#17110d' } as const

function leer(): Tema {
  try {
    const guardado = localStorage.getItem(CLAVE)
    return guardado === 'claro' || guardado === 'oscuro' ? guardado : 'auto'
  } catch {
    return 'auto'
  }
}

/**
 * Pone el tema en la raíz de la página. Lo mismo hace public/tema.js antes de que React
 * arranque, para que la página no aparezca un instante con el tema equivocado.
 */
function aplicar(tema: Tema) {
  const raiz = document.documentElement
  if (tema === 'auto') raiz.removeAttribute('data-theme')
  else raiz.setAttribute('data-theme', NOMBRES[tema])

  // Hay dos etiquetas, una por esquema del sistema. Con el tema fijado, las dos dicen el
  // color elegido; en automático, cada una vuelve a ser la de su esquema.
  for (const meta of document.querySelectorAll('meta[name="theme-color"]')) {
    const oscura = meta.getAttribute('media')?.includes('dark') ?? false
    meta.setAttribute('content', tema === 'auto' ? BARRAS[oscura ? 'oscuro' : 'claro'] : BARRAS[tema])
  }
}

/** El tema elegido y cómo cambiarlo. Se recuerda en este navegador. */
export function useTema(): [Tema, (tema: Tema) => void] {
  const [tema, setTema] = useState<Tema>(leer)

  function elegir(nuevo: Tema) {
    try {
      if (nuevo === 'auto') localStorage.removeItem(CLAVE)
      else localStorage.setItem(CLAVE, nuevo)
    } catch {
      // Sin almacenamiento el tema dura lo que dure la pestaña, que igual sirve.
    }
    aplicar(nuevo)
    setTema(nuevo)
  }

  return [tema, elegir]
}
