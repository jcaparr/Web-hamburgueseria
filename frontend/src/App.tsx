import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { BottomNav } from './components/BottomNav'
import { AuthProvider } from './context/AuthContext'
import { DetalleHamburgueseria } from './pages/DetalleHamburgueseria'
import { Explorar } from './pages/Explorar'
import { Login } from './pages/Login'
import { Ranking } from './pages/Ranking'
import { Registro } from './pages/Registro'
import { Wishlist } from './pages/Wishlist'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="flex flex-1 flex-col">
          <main className="flex-1 overflow-y-auto pb-4">
            <Routes>
              <Route path="/" element={<Explorar />} />
              <Route path="/hamburguesuerias/:id" element={<DetalleHamburgueseria />} />
              <Route path="/ranking" element={<Ranking />} />
              <Route path="/deseados" element={<Wishlist />} />
              <Route path="/login" element={<Login />} />
              <Route path="/registro" element={<Registro />} />
            </Routes>
          </main>
          <BottomNav />
        </div>
      </BrowserRouter>
    </AuthProvider>
  )
}
