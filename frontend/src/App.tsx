import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { BottomNav } from './components/BottomNav'
import { AuthProvider } from './context/AuthContext'
import { BurgerJointDetail } from './pages/BurgerJointDetail'
import { Explore } from './pages/Explore'
import { Login } from './pages/Login'
import { Ranking } from './pages/Ranking'
import { Register } from './pages/Register'
import { Wishlist } from './pages/Wishlist'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="flex flex-1 flex-col">
          <main className="flex-1 overflow-y-auto pb-4">
            <Routes>
              <Route path="/" element={<Explore />} />
              <Route path="/burger-joints/:id" element={<BurgerJointDetail />} />
              <Route path="/ranking" element={<Ranking />} />
              <Route path="/wishlist" element={<Wishlist />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
            </Routes>
          </main>
          <BottomNav />
        </div>
      </BrowserRouter>
    </AuthProvider>
  )
}
