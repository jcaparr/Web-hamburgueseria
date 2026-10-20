// Foto de perfil de Instagram (#292): isotipo + «Burgómetro» pegados, sobre el amarillo de la plantilla.
// Uso: node docs/perfil-ig/generar.mjs   (necesita Edge). Salida: foto-de-perfil-1080.png y foto-de-perfil-2160.png
import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const AQUI = path.dirname(fileURLToPath(import.meta.url))
const EDGE = process.env.EDGE ?? 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'

// El mismo dibujo de frontend/src/components/MarcaBurgometro.tsx, sin el calco.
const ISOTIPO = `<svg viewBox="-5 -5 140 118" xmlns="http://www.w3.org/2000/svg"><g stroke="#141110" stroke-width="3.5" stroke-linejoin="round"><rect x="12" y="88" width="106" height="16" rx="8" fill="#e39f58"/><rect x="7" y="66" width="116" height="20" rx="10" fill="#5d302a"/><path d="M14 62 H116 V71 H98 C98 81 86 81 86 71 H46 C46 80 34 80 34 71 H14 Z" fill="#e5812c" stroke-width="3"/><path d="M9 57 C9 24 36 3 65 3 C94 3 121 24 121 57 C121 61 119 63 115 63 H15 C11 63 9 61 9 57 Z" fill="#e39f58"/></g><g stroke="#fff" stroke-opacity=".4" stroke-linecap="round" fill="none"><path d="M19 42 C21 32 26 24 33 18" stroke-width="4"/><path d="M21 95 H30" stroke-width="3"/></g><g stroke="#141110" stroke-width="3.6" stroke-linecap="round"><line x1="99.24" y1="46.88" x2="106.85" y2="44.40"/><line x1="91.75" y1="33.91" x2="97.70" y2="28.56"/><line x1="79.64" y1="25.11" x2="82.90" y2="17.80"/><line x1="65" y1="22" x2="65" y2="14"/><line x1="50.36" y1="25.11" x2="47.10" y2="17.80"/><line x1="38.25" y1="33.91" x2="32.30" y2="28.56"/><line x1="30.76" y1="46.88" x2="23.15" y2="44.40"/></g><polygon points="94.7,31.2 69.01,62.46 60.99,53.54" fill="#e63240" stroke="#141110" stroke-width="2.5" stroke-linejoin="round"/><circle cx="65" cy="58" r="7.5" fill="#5d302a" stroke="#141110" stroke-width="3"/></svg>`

// Instagram recorta la foto en círculo: el bloque entero cabe dentro del círculo central (el texto queda a menos de 430 px del centro, radio 540).
const HTML = `<!doctype html><meta charset="utf-8"><title>Foto de perfil</title><style>
@font-face{font-family:Poppins;font-weight:800;src:url(fuentes/poppins-800.ttf)}
*{margin:0;box-sizing:border-box}
html,body{width:1080px;height:1080px;overflow:hidden}
body{background:#f4ca2c;position:relative;font-family:Poppins,system-ui,sans-serif;color:#2b1a10}
.fondo{position:absolute;right:-190px;bottom:-210px;width:620px;opacity:.9;transform:rotate(-14deg)}
.fondo svg{width:100%;display:block}
.fondo svg *{fill:none!important;stroke:#f8dc6a!important;stroke-width:7!important;stroke-linejoin:round!important}
.bloque{position:absolute;left:0;right:0;top:50%;transform:translateY(-50%);display:flex;flex-direction:column;align-items:center;gap:6px}
.bloque svg{width:430px;display:block;filter:drop-shadow(14px 22px 24px rgba(110,64,0,.35))}
.bloque span{font-weight:800;font-size:122px;letter-spacing:-3px;line-height:1}
</style>
<div class="bloque">${ISOTIPO}<span>Burgómetro</span></div>`

const f = path.join(AQUI, 'foto-de-perfil.html')
fs.writeFileSync(f, HTML)
for (const [nombre, escala] of [['1080', 1], ['2160', 2]]) {
  const r = spawnSync(EDGE, ['--headless=new', '--disable-gpu', '--hide-scrollbars', `--force-device-scale-factor=${escala}`, '--window-size=1080,1080', '--virtual-time-budget=4000', `--screenshot=${path.join(AQUI, 'foto-de-perfil-' + nombre + '.png')}`, 'file:///' + f.replaceAll('\\', '/')], { stdio: 'ignore' })
  console.log(nombre, r.status)
}
