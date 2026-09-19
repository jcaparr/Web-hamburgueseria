import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { BottomNav } from './components/BottomNav'
import { TopBar } from './components/TopBar'
import { AuthProvider } from './context/AuthContext'
import { BurgerJointDetail } from './pages/BurgerJointDetail'
import { Explore } from './pages/Explore'
import { ForgotPassword } from './pages/ForgotPassword'
import { LinkGoogle } from './pages/LinkGoogle'
import { Login } from './pages/Login'
import { Profile } from './pages/Profile'
import { Ranking } from './pages/Ranking'
import { Register } from './pages/Register'
import { Reviews } from './pages/Reviews'
import { VerifyEmail } from './pages/VerifyEmail'
import { Wishlist } from './pages/Wishlist'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="flex min-h-dvh flex-col bg-base-100 text-base-content">
          <TopBar />
          <main className="mx-auto w-full max-w-[480px] flex-1 pb-20 md:max-w-5xl md:px-6 md:pb-8 md:pt-6">
            <Routes>
              <Route path="/" element={<Explore />} />
              <Route path="/burger-joints/:id" element={<BurgerJointDetail />} />
              <Route path="/ranking" element={<Ranking />} />
              <Route path="/profile" element={<Profile />} />
              <Route path="/reviews" element={<Reviews />} />
              <Route path="/wishlist" element={<Wishlist />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/verify-email" element={<VerifyEmail />} />
              <Route path="/forgot-password" element={<ForgotPassword />} />
              <Route path="/link-google" element={<LinkGoogle />} />
            </Routes>
          </main>
          <BottomNav />
        </div>
      </BrowserRouter>
    </AuthProvider>
  )
}
