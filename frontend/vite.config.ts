import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // En producción Caddy sirve el sitio y el backend desde el mismo origen, así que
    // una ruta como /api/place-photos/xxx.jpg funciona sola. En desarrollo son dos
    // servidores distintos, y sin esto el navegador buscaba las fotos en el 5173 y no
    // las encontraba: se veían rotas en local aunque estuvieran bien guardadas.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
