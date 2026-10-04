import { useEffect, useRef } from 'react'
import { BrowserRouter, Route, Routes, useLocation, useNavigationType } from 'react-router-dom'
import { BottomNav } from './components/BottomNav'
import { RequireAuth } from './components/RequireAuth'
import { TopBar } from './components/TopBar'
import { AuthProvider } from './context/AuthContext'
import { BurgerJointDetail } from './pages/BurgerJointDetail'
import { BuscarGente } from './pages/BuscarGente'
import { Explore } from './pages/Explore'
import { Feed } from './pages/Feed'
import { ForgotPassword } from './pages/ForgotPassword'
import { Login } from './pages/Login'
import { NoEncontrada } from './pages/NoEncontrada'
import { PerfilPublico } from './pages/PerfilPublico'
import { Profile } from './pages/Profile'
import { Ranking } from './pages/Ranking'
import { Register } from './pages/Register'
import { Reviews } from './pages/Reviews'
import { Tour } from './pages/Tour'
import { VerifyEmail } from './pages/VerifyEmail'
import { Wishlist } from './pages/Wishlist'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="flex min-h-dvh flex-col bg-base-100 text-base-content">
          <a
            href="#contenido"
            className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-field focus:bg-neutral focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-secondary"
          >
            Saltar al contenido
          </a>
          <AlCambiarDePagina />
          <TopBar />
          <main
            id="contenido"
            tabIndex={-1}
            className="mx-auto w-full max-w-[480px] flex-1 pb-20 focus:outline-none md:max-w-5xl md:px-6 md:pb-8 md:pt-6"
          >
            <Routes>
              <Route path="/" element={<Explore />} />
              <Route path="/burger-joints/:id" element={<BurgerJointDetail />} />
              <Route path="/ranking" element={<Ranking />} />
              <Route path="/tour" element={<Tour />} />
              {/* Bajo /u/ para que un nombre de usuario no pueda chocar nunca con una
                  pantalla de la app: alguien que se llame "tour" no rompe nada. */}
              <Route path="/u/:username" element={<RequireAuth><PerfilPublico /></RequireAuth>} />
              <Route path="/buscar" element={<RequireAuth><BuscarGente /></RequireAuth>} />
              <Route path="/feed" element={<RequireAuth><Feed /></RequireAuth>} />
              <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />
              <Route path="/reviews" element={<RequireAuth><Reviews /></RequireAuth>} />
              <Route path="/wishlist" element={<RequireAuth><Wishlist /></RequireAuth>} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/verify-email" element={<VerifyEmail />} />
              <Route path="/forgot-password" element={<ForgotPassword />} />
              <Route path="*" element={<NoEncontrada />} />
            </Routes>
          </main>
          <BottomNav />
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
