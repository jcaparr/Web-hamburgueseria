// Arma las láminas del carrusel de Instagram (#272) en HTML y las renderiza a PNG (1080×1350)
// con Edge en modo headless. Uso: node docs/post-ig/generar.mjs
// Las capturas (capturas/*.png) salen de la web local; los textos están en LAMINAS.
import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const AQUI = path.dirname(fileURLToPath(import.meta.url))
const EDGE = process.env.EDGE ?? 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
const SALIDA = path.join(AQUI, 'laminas')
fs.mkdirSync(SALIDA, { recursive: true })

// El isotipo es el mismo dibujo de frontend/src/components/MarcaBurgometro.tsx, sin el calco.
const ISOTIPO = `<svg viewBox="-5 -5 140 118" xmlns="http://www.w3.org/2000/svg"><g stroke="#141110" stroke-width="3.5" stroke-linejoin="round"><rect x="12" y="88" width="106" height="16" rx="8" fill="#e39f58"/><rect x="7" y="66" width="116" height="20" rx="10" fill="#5d302a"/><path d="M14 62 H116 V71 H98 C98 81 86 81 86 71 H46 C46 80 34 80 34 71 H14 Z" fill="#e5812c" stroke-width="3"/><path d="M9 57 C9 24 36 3 65 3 C94 3 121 24 121 57 C121 61 119 63 115 63 H15 C11 63 9 61 9 57 Z" fill="#e39f58"/></g><g stroke="#fff" stroke-opacity=".4" stroke-linecap="round" fill="none"><path d="M19 42 C21 32 26 24 33 18" stroke-width="4"/><path d="M21 95 H30" stroke-width="3"/></g><g stroke="#141110" stroke-width="3.6" stroke-linecap="round"><line x1="99.24" y1="46.88" x2="106.85" y2="44.40"/><line x1="91.75" y1="33.91" x2="97.70" y2="28.56"/><line x1="79.64" y1="25.11" x2="82.90" y2="17.80"/><line x1="65" y1="22" x2="65" y2="14"/><line x1="50.36" y1="25.11" x2="47.10" y2="17.80"/><line x1="38.25" y1="33.91" x2="32.30" y2="28.56"/><line x1="30.76" y1="46.88" x2="23.15" y2="44.40"/></g><polygon points="94.7,31.2 69.01,62.46 60.99,53.54" fill="#e63240" stroke="#141110" stroke-width="2.5" stroke-linejoin="round"/><circle cx="65" cy="58" r="7.5" fill="#5d302a" stroke="#141110" stroke-width="3"/></svg>`

const CSS = `
@font-face{font-family:Poppins;font-weight:400;src:url(../../fuentes/poppins-400.ttf)}
@font-face{font-family:Poppins;font-weight:500;src:url(../../fuentes/poppins-500.ttf)}
@font-face{font-family:Poppins;font-weight:600;src:url(../../fuentes/poppins-600.ttf)}
@font-face{font-family:Poppins;font-weight:800;src:url(../../fuentes/poppins-800.ttf)}
*{box-sizing:border-box;margin:0;padding:0}
html,body{width:1080px;height:1350px;overflow:hidden}
body{position:relative;background:#f2b705;color:#2b1a10;font-family:Poppins,system-ui,sans-serif}
.marca{position:absolute;right:-150px;top:-120px;width:640px;opacity:.13;transform:rotate(-14deg)}
.marca svg{width:100%;display:block}
.marca svg *{fill:#8a5a00!important;stroke:none!important}
h1{position:absolute;left:58px;top:92px;width:960px;font-weight:800;font-size:98px;line-height:1.04;letter-spacing:-2px}
h1.m{font-size:92px}
.sub{position:absolute;left:62px;font-weight:500;font-size:35px;line-height:1.3;width:410px;color:#3a2a1e}
.rayas{position:absolute;width:60px;height:80px}
.rayas i{position:absolute;height:7px;border-radius:4px;background:#b23a2e;transform-origin:left center}
.sticker{position:absolute;left:112px;width:340px;height:340px;border-radius:50%;background:#3a2a1e;color:#f7efd8;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;font-weight:800;font-size:60px;line-height:1.05;transform:rotate(-9deg);box-shadow:0 18px 30px rgba(43,26,16,.35);z-index:4}
.sticker u{display:block;width:230px;height:9px;border-radius:6px;background:#f2b705;margin-top:10px;text-decoration:none;transform:rotate(-4deg)}
.burst{position:absolute;z-index:4}
.burst i{position:absolute;width:9px;border-radius:5px;background:#2b1a10;transform-origin:center}
.logo{position:absolute;left:70px;bottom:58px;display:flex;align-items:center;gap:20px;font-weight:800;font-size:46px;letter-spacing:-1px;z-index:5}
.logo svg{width:74px;height:auto}
.phone{position:absolute;width:490px;height:1100px;left:528px;top:380px;padding:13px;border-radius:80px;background:linear-gradient(160deg,#3b3b3e,#161617 60%);box-shadow:0 0 0 3px #5a5a5e inset,0 50px 70px rgba(43,26,16,.38);transform:rotate(7deg)}
.phone:before,.phone:after{content:"";position:absolute;width:7px;background:#2a2a2c;border-radius:4px}
.phone:before{left:-7px;top:190px;height:90px}
.phone:after{right:-7px;top:260px;height:130px}
.pantalla{position:relative;width:100%;height:100%;border-radius:68px;overflow:hidden;background:#faf3e3}
.estado{height:62px;display:flex;justify-content:space-between;align-items:flex-end;padding:0 44px 6px;font-weight:600;font-size:23px;color:#2b1a10}
.estado b{display:block;font-weight:600;letter-spacing:1px}
.estado span{display:flex;gap:9px;align-items:center}
.estado svg{height:20px;width:auto}
.isla{position:absolute;left:50%;top:14px;width:150px;height:42px;margin-left:-75px;border-radius:24px;background:#0b0b0c;z-index:3}
.pantalla>img{display:block;width:100%}
`
const ICONOS_ESTADO = `<span><svg viewBox="0 0 28 20"><rect x="0" y="12" width="4" height="8" rx="1" fill="currentColor"/><rect x="8" y="8" width="4" height="12" rx="1" fill="currentColor"/><rect x="16" y="4" width="4" height="16" rx="1" fill="currentColor"/><rect x="24" y="0" width="4" height="20" rx="1" fill="currentColor"/></svg><svg viewBox="0 0 44 20"><rect x="1" y="2" width="38" height="16" rx="5" fill="none" stroke="currentColor" stroke-width="2"/><rect x="4" y="5" width="28" height="10" rx="3" fill="currentColor"/></svg></span>`

// Trazos cortos rojos junto al subtítulo, como los de la plantilla.
function rayas(x, y, ang) {
  let s = ''
  for (let i = 0; i < 3; i++) s += `<i style="left:0;top:${20 + i * 22}px;width:${40 + (i % 2) * 8}px;transform:rotate(${ang + (i - 1) * 26}deg)"></i>`
  return `<div class="rayas" style="left:${x}px;top:${y}px">${s}</div>`
}
// Cuatro trazos oscuros que rodean el sello.
function destello(cx, cy, r) {
  let s = ''
  for (let i = 0; i < 4; i++) {
    const a = (-62 + i * 24) * Math.PI / 180
    s += `<i style="left:${Math.cos(a) * r}px;top:${Math.sin(a) * r}px;height:${46 - i * 5}px;transform:rotate(${-62 + i * 24 + 90}deg)"></i>`
  }
  return `<div class="burst" style="left:${cx}px;top:${cy}px">${s}</div>`
}

function celu({ img, barra = '#faf3e3', pantallaHtml }) {
  const dentro = pantallaHtml ?? `<img src="../../capturas/${img}">`
  return `<div class="phone"><div class="pantalla" style="background:${barra}"><div class="isla"></div><div class="estado" style="background:${barra}"><b>9:41</b>${ICONOS_ESTADO}</div>${dentro}</div></div>`
}

const NUM = 'display:grid;place-items:center;width:44px;height:44px;border-radius:50%;background:#3a2a1e;color:#f7efd8;font:800 26px Poppins;position:absolute'

// El menú de Chrome es una ilustración (no sale de la web): los tres puntitos, el menú y el paso clave resaltado.
const MENU_CHROME = `<div style="position:absolute;inset:62px 0 0 0;background:#fff;font-family:Roboto,Arial,sans-serif;color:#202124">
  <div style="display:flex;align-items:center;gap:14px;padding:14px 18px;background:#f1f3f4">
    <div style="flex:1;background:#fff;border-radius:30px;padding:16px 22px;font-size:25px">burgometro.com.ar</div>
    <div style="position:relative;width:52px;height:52px;border-radius:50%;background:#fde7e4;display:grid;place-items:center;box-shadow:0 0 0 4px #b23a2e"><svg width="8" height="30" viewBox="0 0 8 30"><circle cx="4" cy="4" r="3.6" fill="#202124"/><circle cx="4" cy="15" r="3.6" fill="#202124"/><circle cx="4" cy="26" r="3.6" fill="#202124"/></svg>
      <b style="${NUM};left:-34px;top:-20px">1</b></div>
  </div>
  <div style="position:absolute;right:16px;top:100px;width:440px;background:#fff;border-radius:22px;box-shadow:0 10px 40px rgba(0,0,0,.35);padding:14px 0;font-size:25px">
    <div style="padding:17px 26px">Nueva pestaña</div>
    <div style="padding:17px 26px">Nueva pestaña de incógnito</div>
    <div style="padding:17px 26px">Historial</div>
    <div style="border-top:2px solid #e8eaed;margin:8px 0"></div>
    <div style="position:relative;margin:4px 12px;padding:16px 14px;border-radius:16px;background:#fde7e4;box-shadow:0 0 0 4px #b23a2e;font-weight:600;line-height:1.2">Agregar a la pantalla de inicio
      <b style="${NUM};right:-8px;top:-26px;z-index:2">2</b></div>
    <div style="padding:17px 26px">Compartir…</div>
    <div style="padding:17px 26px">Configuración</div>
  </div>
  <div style="position:absolute;left:0;right:0;top:650px;height:240px;background:#fff;border-radius:30px;box-shadow:0 -8px 30px rgba(0,0,0,.25);padding:28px 30px;font-size:26px">
    <div style="font-weight:600;font-size:29px;margin-bottom:18px">Agregar a la pantalla de inicio</div>
    <div style="display:flex;gap:14px;align-items:center"><div style="width:62px;height:62px;border-radius:16px;background:#f2b705;display:grid;place-items:center;padding:7px">${ISOTIPO}</div><div style="flex:1;font-size:24px">Burgómetro</div>
    <div style="padding:16px 20px;border-radius:30px;background:#b23a2e;color:#fff;font-weight:600;font-size:23px;box-shadow:0 0 0 4px #fde7e4,0 0 0 8px #b23a2e;position:relative">Crear acceso directo<b style="${NUM};right:-14px;top:-40px">3</b></div></div>
  </div></div>`

const LAMINAS = [
  { id: '01-portada', h: 'Todo lo que podés hacer en Burgómetro', m: true, sub: '+1.200 hamburgueserías en Buenos Aires y alrededores, en un solo lugar.', subTop: 470, sticker: ['deslizá', '→'], stTop: 790, phone: { img: 'explorar.png' } },
  { id: '02-sugerir-local', h: '¿Falta tu hamburguesería?', sub: 'Sumala a nuestro mapa y ayudanos a que más personas descubran tu lugar.', subTop: 400, sticker: ['pasanos', 'el dato'], stTop: 700, phone: { img: 'sugerir-local.png', barra: '#9b958b' } },
  { id: '03-instalar', h: 'Tenela<br>en tu celu', sub: 'En el menú del navegador (los 3 puntitos), elegí «Agregar a la pantalla de inicio». En iPhone, tocá Compartir.', subSize: 31, subTop: 400, sticker: ['3 toques', 'y entrás'], stTop: 700, phone: { html: MENU_CHROME, barra: '#f1f3f4' } },
  { id: '04-recorrido', h: 'Armá tu<br>recorrido', sub: 'Elegí paradas y distancia, a pie o en auto. Te armamos la ruta.', subTop: 400, sticker: ['a pie', 'o en auto'], stTop: 700, phone: { img: 'tour.png' } },
  { id: '05-buscar-gente', h: 'Buscá gente y seguila', sub: 'Encontrá a otros fanáticos de las burgers y mirá qué recomiendan.', subTop: 400, sticker: ['mirá qué', 'comen'], stTop: 700, phone: { img: 'buscar-gente.png' } },
  { id: '06-avatar', h: 'Armá tu hamburguesa', sub: 'Elegí fondo, pan y queso. Es tu cara en Burgómetro.', subTop: 400, sticker: ['a tu', 'gusto'], stTop: 700, phone: { img: 'avatar.png' } },
  { id: '07-guardadas', h: 'Guardá las<br>que te debés', sub: 'Tocá el corazón y encontralas después en Guardadas.', subTop: 400, sticker: ['para', 'después'], stTop: 700, phone: { img: 'guardadas.png' } },
]

function html(l) {
  const phone = celu({ img: l.phone.img, barra: l.phone.barra, pantallaHtml: l.phone.html })
  return `<!doctype html><meta charset="utf-8"><title>${l.id}</title><style>${CSS}</style>
<div class="marca">${ISOTIPO}</div>
<h1 class="${l.m ? 'm' : ''}">${l.h}</h1>
<p class="sub" style="top:${l.subTop}px;${l.subSize ? `font-size:${l.subSize}px;width:380px` : ''}">${l.sub}</p>
${rayas(486, l.subTop - 12, -60)}
${phone}
<div class="sticker" style="top:${l.stTop}px">${l.sticker[0]}<br>${l.sticker[1]}<u></u></div>
${destello(282, l.stTop + 170, 215)}
<div class="logo">${ISOTIPO}<span>Burgómetro</span></div>`
}

// Cierre: sin celu, el logo grande y la dirección.
const CIERRE = `<!doctype html><meta charset="utf-8"><title>08-cierre</title><style>${CSS}
.grande{position:absolute;left:0;right:0;top:360px;text-align:center}
.grande svg{width:430px;display:block;margin:0 auto}
.grande .n{font-weight:800;font-size:128px;letter-spacing:-3px;margin-top:24px}
.url{position:absolute;left:0;right:0;top:1030px;text-align:center}
.url span{display:inline-block;background:#3a2a1e;color:#f7efd8;border-radius:60px;padding:26px 58px;font-weight:800;font-size:54px}
</style>
<div class="marca" style="right:-190px;top:-150px;width:700px">${ISOTIPO}</div>
<h1 style="text-align:center;left:0;right:0;width:auto;font-size:84px;top:110px">Descubrí. Calificá. Recorré.</h1>
<div class="grande">${ISOTIPO}<div class="n">Burgómetro</div></div>
<div class="url"><span>burgometro.com.ar</span></div>
<p class="sub" style="left:0;right:0;width:auto;text-align:center;top:1170px;font-size:36px">Entrá desde el link de la bio.</p>`

const todas = [...LAMINAS.map(l => [l.id, html(l)]), ['08-cierre', CIERRE]]
const dirHtml = path.join(SALIDA, 'html')
fs.mkdirSync(dirHtml, { recursive: true })
for (const [id, h] of todas) {
  const f = path.join(dirHtml, id + '.html')
  fs.writeFileSync(f, h)
  const r = spawnSync(EDGE, ['--headless=new', '--disable-gpu', '--hide-scrollbars', '--force-device-scale-factor=1', '--window-size=1080,1350', '--virtual-time-budget=4000', `--screenshot=${path.join(SALIDA, id + '.png')}`, 'file:///' + f.replace(/\\/g, '/')], { stdio: 'ignore' })
  console.log(id, r.status)
}
