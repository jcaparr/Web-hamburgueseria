import { useEffect, useRef } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useLocation, useNavigationType } from 'react-router-dom'
import { AvisoDeLogro } from './components/AvisoDeLogro'
import { AvisoDeXp } from './components/AvisoDeXp'
import { InvitacionARegistrarse } from './components/InvitacionARegistrarse'
import { BottomNav } from './components/BottomNav'
import { PieDeLaPagina } from './components/PieDeLaPagina'
import { RequireAuth } from './components/RequireAuth'
import { TopBar } from './components/TopBar'
import { AuthProvider } from './context/AuthContext'
import { ArmarHamburguesa } from './pages/ArmarHamburguesa'
import { BurgerJointDetail } from './pages/BurgerJointDetail'
import { FichaPorSlug } from './pages/FichaPorSlug'
import { Explore } from './pages/Explore'
import { Feed } from './pages/Feed'
import { GenteDeUnPerfil } from './pages/GenteDeUnPerfil'
import { ForgotPassword } from './pages/ForgotPassword'
import { Login } from './pages/Login'
import { Logros } from './pages/Logros'
import { NoEncontrada } from './pages/NoEncontrada'
import { PaginaDeCadena } from './pages/PaginaDeCadena'
import { PerfilPublico } from './pages/PerfilPublico'
import { Privacidad } from './pages/Privacidad'
import { Profile } from './pages/Profile'
import { Ranking } from './pages/Ranking'
import { Register } from './pages/Register'
import { ReseniasDeUnPerfil } from './pages/ReseniasDeUnPerfil'
import { Reviews } from './pages/Reviews'
import { Terminos } from './pages/Terminos'
import { Tour } from './pages/Tour'
import { VerifyEmail } from './pages/VerifyEmail'
import { Wishlist } from './pages/Wishlist'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="flex min-h-dvh flex-col bg-base-200 text-base-content">
          <a
            href="#contenido"
            className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-field focus:bg-neutral focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-secondary"
          >
            Saltar al contenido
          </a>
          <AlCambiarDePagina />
          <AvisoDeXp />
          <AvisoDeLogro />
          <TopBar />
          {/* En el teléfono, lo de abajo deja lugar para la barra de navegación, que flota
              encima: 59 px de barra, lo que ocupa la barra de gestos del teléfono si tiene
              y unos 13 de aire. Con pb-28 fijo sobraban 43 px vacíos al final de cada
              página (#174).

              En escritorio llega hasta 1200 px: con 1024 un monitor ancho quedaba con la
              mitad vacía (#244). Las páginas para leer ponen su propio ancho, más angosto. */}
          <main
            id="contenido"
            tabIndex={-1}
            className="mx-auto w-full max-w-[480px] flex-1 focus:outline-none md:max-w-[75rem] md:px-6 md:pb-8 md:pt-6"
          >
            <Routes>
              <Route path="/" element={<Explore />} />
              <Route path="/burger-joints/:id" element={<BurgerJointDetail />} />
              <Route path="/cadenas/:marca" element={<PaginaDeCadena />} />
              <Route path="/ranking" element={<Ranking />} />
              <Route path="/tour" element={<Tour />} />
              {/* Bajo /u/ para que un nombre de usuario no pueda chocar nunca con una
                  pantalla de la app: alguien que se llame "tour" no rompe nada. */}
              <Route path="/u/:username" element={<RequireAuth><PerfilPublico /></RequireAuth>} />
              <Route path="/u/:username/resenias" element={<RequireAuth><ReseniasDeUnPerfil /></RequireAuth>} />
              <Route path="/u/:username/seguidores" element={<RequireAuth><GenteDeUnPerfil lista="seguidores" /></RequireAuth>} />
              <Route path="/u/:username/siguiendo" element={<RequireAuth><GenteDeUnPerfil lista="siguiendo" /></RequireAuth>} />
              {/* Buscar gente vive en el Feed: /buscar queda para los enlaces viejos. */}
              <Route path="/buscar" element={<Navigate to="/feed" replace />} />
              <Route path="/feed" element={<RequireAuth><Feed /></RequireAuth>} />
              <Route path="/logros" element={<RequireAuth><Logros /></RequireAuth>} />
              <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />
              <Route path="/profile/hamburguesa" element={<RequireAuth><ArmarHamburguesa /></RequireAuth>} />
              <Route path="/reviews" element={<RequireAuth><Reviews /></RequireAuth>} />
              <Route path="/wishlist" element={<RequireAuth><Wishlist /></RequireAuth>} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/verify-email" element={<VerifyEmail />} />
              <Route path="/forgot-password" element={<ForgotPassword />} />
              <Route path="/terminos" element={<Terminos />} />
              <Route path="/privacidad" element={<Privacidad />} />
              {/* La dirección linda de un local, burgometro.com.ar/<slug> (#305). Última de
                  las de un tramo: las pantallas de arriba ganan, y SlugsDeLocales no le da
                  a ningún local el nombre de una. */}
              <Route path="/:slug" element={<FichaPorSlug />} />
              <Route path="*" element={<NoEncontrada />} />
            </Routes>
          </main>
          <PieDeLaPagina />
          <BottomNav />
          <InvitacionARegistrarse />
        </div>
      </BrowserRouter>
    </AuthProvider>
  )
}

/**
 * Al pasar a otra pantalla: arranca arriba y el foco va al contenido.
 *
 * En una app de una sola página, cambiar de pantalla no recarga nada. La pantalla
 * nueva aparecía con el scroll que traía la anterior —desde la mitad del Ranking,
 * Explorar se abría por la mitad—, y el foco se quedaba en el enlace que se tocó, que
 * muchas veces ya no existe: quien usa lector de pantalla no se enteraba del cambio.
 *
 * Arriba solo cuando se llega por un enlace, no con "atrás": al volver, cada pantalla
 * decide dónde estaba, y Explorar vuelve a la tarjeta que se había tocado.
 *
 * Y solo cuando cambia la ruta, no los filtros: elegir un barrio en Explorar o una
 * pestaña del Ranking no es ir a otro lado.
 */
function AlCambiarDePagina() {
  const { pathname } = useLocation()
  const comoLlego = useNavigationType()
  const anterior = useRef(pathname)

  useEffect(() => {
    if (anterior.current === pathname) return
    anterior.current = pathname
    if (comoLlego !== 'POP') window.scrollTo(0, 0)
    document.getElementById('contenido')?.focus({ preventScroll: true })
  }, [pathname, comoLlego])

  return null
}
