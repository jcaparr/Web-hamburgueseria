import { createReadStream, statSync } from 'node:fs'
import { join, normalize, sep } from 'node:path'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig, loadEnv, type Plugin } from 'vite'

/**
 * En desarrollo, sirve /tiles/<archivo>.pmtiles desde la carpeta MAPA_TILES_DIR del
 * .env.local (#395), igual que Caddy en producción. Hace falta soportar pedidos Range:
 * el navegador lee el archivo a pedacitos, no lo baja entero.
 *
 * Sin MAPA_TILES_DIR no hace nada, y el .pmtiles se puede dejar en public/tiles/.
 */
function tilesDeDesarrollo(carpeta: string | undefined): Plugin {
  return {
    name: 'tiles-de-desarrollo',
    apply: 'serve',
    configureServer(server) {
      if (!carpeta) return
      const raiz = normalize(carpeta)
      server.middlewares.use('/tiles', (req, res, next) => {
        const nombre = decodeURIComponent((req.url ?? '').split('?')[0]).replace(/^\/+/, '')
        const ruta = normalize(join(raiz, nombre))
        // Que un nombre con ".." no saque nada de afuera de la carpeta.
        if (!nombre.endsWith('.pmtiles') || !ruta.startsWith(raiz + sep)) return next()

        let tamanio: number
        try {
          tamanio = statSync(ruta).size
        } catch {
          return next()
        }

        const rango = /^bytes=(\d*)-(\d*)$/.exec(req.headers.range ?? '')
        res.setHeader('Accept-Ranges', 'bytes')
        res.setHeader('Content-Type', 'application/octet-stream')
        if (!rango) {
          res.setHeader('Content-Length', tamanio)
          createReadStream(ruta).pipe(res)
          return
        }
        const desde = rango[1] === '' ? Math.max(tamanio - Number(rango[2]), 0) : Number(rango[1])
        const hasta = rango[1] === '' || rango[2] === '' ? tamanio - 1 : Math.min(Number(rango[2]), tamanio - 1)
        if (desde > hasta || desde >= tamanio) {
          res.statusCode = 416
          res.setHeader('Content-Range', `bytes */${tamanio}`)
          res.end()
          return
        }
        res.statusCode = 206
        res.setHeader('Content-Range', `bytes ${desde}-${hasta}/${tamanio}`)
        res.setHeader('Content-Length', hasta - desde + 1)
        createReadStream(ruta, { start: desde, end: hasta }).pipe(res)
      })
    },
  }
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react(), tailwindcss(), tilesDeDesarrollo(env.MAPA_TILES_DIR)],
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
  }
})
