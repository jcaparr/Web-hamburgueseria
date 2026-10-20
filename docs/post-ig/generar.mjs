// Arma las láminas del carrusel de Instagram (#272) en HTML y las renderiza a PNG (1080×1350)
// con Edge en modo headless. Uso: node docs/post-ig/generar.mjs
// Las capturas (capturas/*.png) salen de la web local; los textos están en LAMINAS.
import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const AQUI = path.dirname(fileURLToPath(import.meta.url))
const EDGE = process.env.EDGE ?? 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
// 4:5 es el formato del carrusel del feed; 9:16 es para historias (con ~250 px de zona segura arriba y abajo).
// El celu se dibuja siempre a 490×1100 y se escala (S) alrededor de su centro (cx, cy).
const FORMATOS = [
  { dir: 'laminas', alto: 1350, S: 0.86, cx: 778, cy: 820, h1: 80, sub: 0, st: 0, logo: 58, c: { h1: 110, g: 360, u: 1030, s: 1170 }, hist: { top: 650, w: 150 }, sello: 165 },
  { dir: 'laminas-9x16', alto: 1920, S: 0.92, cx: 765, cy: 1100, h1: 270, sub: 245, st: 360, logo: 360, c: { h1: 330, g: 600, u: 1280, s: 1450 }, hist: { top: 870, w: 185 }, sello: 240 },
]

// El isotipo es el mismo dibujo de frontend/src/components/MarcaBurgometro.tsx, sin el calco.
const ISOTIPO = `<svg viewBox="-5 -5 140 118" xmlns="http://www.w3.org/2000/svg"><g stroke="#141110" stroke-width="3.5" stroke-linejoin="round"><rect x="12" y="88" width="106" height="16" rx="8" fill="#e39f58"/><rect x="7" y="66" width="116" height="20" rx="10" fill="#5d302a"/><path d="M14 62 H116 V71 H98 C98 81 86 81 86 71 H46 C46 80 34 80 34 71 H14 Z" fill="#e5812c" stroke-width="3"/><path d="M9 57 C9 24 36 3 65 3 C94 3 121 24 121 57 C121 61 119 63 115 63 H15 C11 63 9 61 9 57 Z" fill="#e39f58"/></g><g stroke="#fff" stroke-opacity=".4" stroke-linecap="round" fill="none"><path d="M19 42 C21 32 26 24 33 18" stroke-width="4"/><path d="M21 95 H30" stroke-width="3"/></g><g stroke="#141110" stroke-width="3.6" stroke-linecap="round"><line x1="99.24" y1="46.88" x2="106.85" y2="44.40"/><line x1="91.75" y1="33.91" x2="97.70" y2="28.56"/><line x1="79.64" y1="25.11" x2="82.90" y2="17.80"/><line x1="65" y1="22" x2="65" y2="14"/><line x1="50.36" y1="25.11" x2="47.10" y2="17.80"/><line x1="38.25" y1="33.91" x2="32.30" y2="28.56"/><line x1="30.76" y1="46.88" x2="23.15" y2="44.40"/></g><polygon points="94.7,31.2 69.01,62.46 60.99,53.54" fill="#e63240" stroke="#141110" stroke-width="2.5" stroke-linejoin="round"/><circle cx="65" cy="58" r="7.5" fill="#5d302a" stroke="#141110" stroke-width="3"/></svg>`

const CSS = `
@font-face{font-family:Poppins;font-weight:400;src:url(../../fuentes/poppins-400.ttf)}
@font-face{font-family:Poppins;font-weight:500;src:url(../../fuentes/poppins-500.ttf)}
@font-face{font-family:Poppins;font-weight:600;src:url(../../fuentes/poppins-600.ttf)}
@font-face{font-family:Poppins;font-weight:800;src:url(../../fuentes/poppins-800.ttf)}
*{box-sizing:border-box;margin:0;padding:0}
html,body{width:1080px;overflow:hidden}
body{position:relative;background:#f4ca2c;color:#2b1a10;font-family:Poppins,system-ui,sans-serif}
.marca{position:absolute;right:-250px;top:-170px;width:780px;opacity:.9;transform:rotate(-14deg)}
.marca svg{width:100%;display:block}
.marca svg *{fill:none!important;stroke:#f8dc6a!important;stroke-width:7!important;stroke-linejoin:round!important}
h1{position:absolute;left:58px;top:92px;width:960px;font-weight:800;font-size:98px;line-height:1.04;letter-spacing:-2px}
h1.m{font-size:84px}
.sub{position:absolute;left:62px;font-weight:500;font-size:35px;line-height:1.3;width:410px;color:#3a2a1e}
.rayas{position:absolute;width:60px;height:80px}
.rayas i{position:absolute;height:7px;border-radius:4px;background:#b23a2e;transform-origin:left center}
.sticker{position:absolute;left:112px;width:340px;height:340px;border-radius:50%;background:#3a2a1e;color:#f7efd8;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;font-weight:800;font-size:60px;line-height:1.05;transform:rotate(-9deg);box-shadow:0 18px 30px rgba(43,26,16,.35);z-index:4}
.sello-chico{position:absolute;z-index:5}
.sticker u{display:block;width:230px;height:9px;border-radius:6px;background:#f2b705;margin-top:10px;text-decoration:none;transform:rotate(-4deg)}
.burst{position:absolute;z-index:4}
.burst i{position:absolute;width:9px;border-radius:5px;background:#2b1a10;transform-origin:center}
.logo{position:absolute;left:70px;bottom:58px;display:flex;align-items:center;gap:20px;font-weight:800;font-size:46px;letter-spacing:-1px;z-index:5}
.logo svg{width:74px;height:auto}
.phone{position:absolute;width:490px;height:1100px;padding:13px;border-radius:80px;background:linear-gradient(160deg,#3b3b3e,#161617 60%);box-shadow:0 0 0 3px #5a5a5e inset,26px 46px 64px rgba(110,64,0,.38);}
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
.nota-r{position:absolute;border:6px solid #b23a2e;border-radius:18px;box-shadow:0 0 0 4px #fff7e0;z-index:5}
.nota-c{position:absolute;background:#3a2a1e;color:#f7efd8;font:600 30px/1.1 Poppins;padding:9px 18px 9px 12px;display:flex;align-items:center;border-radius:15px;white-space:nowrap;z-index:6;box-shadow:0 6px 14px rgba(43,26,16,.3)}
.num{display:inline-grid;place-items:center;width:38px;height:38px;border-radius:50%;background:#b23a2e;color:#fff;box-shadow:0 0 0 3px #f7efd8;font:800 24px Poppins;margin-right:12px;flex:none}
.nota-c:after{content:"";position:absolute;left:var(--tx,30px);border:10px solid transparent}
.nota-c.arriba:after{top:-19px;border-bottom-color:#3a2a1e}
.nota-c.abajo:after{bottom:-19px;border-top-color:#3a2a1e}
.historia{position:absolute;left:78px;border:10px solid #fff7e0;border-radius:34px;overflow:hidden;box-shadow:0 24px 40px rgba(43,26,16,.4);transform:rotate(-5deg);z-index:4;background:#241a13}
.historia img{display:block;width:100%}
.paso::before{content:attr(data-paso);position:absolute;left:-22px;top:-26px;width:42px;height:42px;border-radius:50%;background:#b23a2e;color:#fff;box-shadow:0 0 0 3px #fff;display:grid;place-items:center;font:800 26px Poppins;z-index:8}
#pasos{position:absolute;left:0;top:0;width:1080px;z-index:6}
.pc{position:absolute;left:58px;width:430px;display:flex;align-items:center;gap:16px;background:#3a2a1e;color:#f7efd8;border-radius:24px;padding:14px 22px 14px 14px;font:600 34px/1.15 Poppins;box-shadow:0 18px 30px rgba(43,26,16,.35)}
.pc .num{width:54px;height:54px;font-size:32px;margin:0;box-shadow:0 0 0 3px #f7efd8}
#flechas{position:absolute;left:0;top:0;width:1080px;z-index:7;pointer-events:none}
#flechasBajo{position:absolute;left:0;top:0;width:1080px;pointer-events:none}
#flechas,#flechasBajo{display:none}
.extra{position:absolute;left:120px}
.cap{display:inline-flex;background:#3a2a1e;color:#f7efd8;border-radius:18px;padding:10px 20px;font:600 30px/1.1 Poppins;margin-bottom:22px}
.nota-c.izq:after{left:-19px;top:calc(50% - 10px);border-right-color:#3a2a1e}
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

const FC = 464 / 375
function notas(lista = []) {
  return lista.map((n, k) => {
    const [x, y, w, h] = n.ring
    return `<div class="nota-r paso" data-paso="${k + 1}" data-txt="${n.chip.t}" style="left:${x * FC - 6}px;top:${62 + y * FC - 6}px;width:${w * FC + 12}px;height:${h * FC + 12}px"></div>`
  }).join('')
}

function celu({ img, barra = '#faf3e3', pantallaHtml, notas: lista }, F) {
  const dentro = pantallaHtml ?? `<img src="../../capturas/${img}">`
  return `<div class="phone" style="left:${F.cx - 245}px;top:${F.cy - 550}px;transform:rotate(7deg) scale(${F.S})"><div class="pantalla" style="background:${barra}"><div class="isla"></div><div class="estado" style="background:${barra}"><b>9:41</b>${ICONOS_ESTADO}</div>${dentro}${notas(lista)}</div></div>`
}

const NUM = 'display:grid;place-items:center;width:44px;height:44px;border-radius:50%;background:#b23a2e;color:#fff;box-shadow:0 0 0 4px #fff;font:800 26px Poppins;position:absolute'

// El menú de Chrome es una ilustración (no sale de la web): los tres puntitos, el menú y el paso clave resaltado.
const MENU_CHROME = (() => {
  // Chrome en Android, tema claro. Los nombres son los reales (verificados por Juan en su celu).
  const fila = (t, extra = '') => `<div style="padding:19px 26px;display:flex;gap:18px;align-items:center;${extra}"><i style="width:30px;height:30px;border-radius:8px;border:3px solid #80868b;flex:none"></i>${t}</div>`
  const marca = (n, pos) => `<b style="${NUM};${pos};z-index:3">${n}</b>`
  return `<div style="position:absolute;inset:62px 0 0 0;background:#f8f9fa;font-family:Roboto,Arial,sans-serif;color:#202124">
  <div style="display:flex;align-items:center;gap:14px;padding:14px 18px;background:#f1f3f4">
    <div style="flex:1;background:#fff;border-radius:30px;padding:16px 22px;font-size:25px;color:#5f6368">burgometro.com.ar</div>
    <div class="paso" data-paso="1" data-txt="Tocá los 3 puntitos" style="position:relative;width:56px;height:56px;border-radius:50%;background:#fde7e4;display:grid;place-items:center;box-shadow:0 0 0 4px #fff,0 0 0 8px #b23a2e"><svg width="8" height="30" viewBox="0 0 8 30"><circle cx="4" cy="4" r="3.6" fill="#202124"/><circle cx="4" cy="15" r="3.6" fill="#202124"/><circle cx="4" cy="26" r="3.6" fill="#202124"/></svg>
      </div>
  </div>
  <div style="position:absolute;right:12px;top:176px;width:430px;background:#fff;border-radius:24px;box-shadow:0 10px 40px rgba(60,64,67,.4);padding:12px 0;font-size:25px">
    ${fila('Traducir…')}
    ${fila('Mostrar el Modo de lectura')}
    <div class="paso" data-paso="2" data-txt="Elegí «Instalar y crear acceso directo»" style="position:relative;margin:4px 10px;border-radius:16px;background:#fde7e4;font-weight:600;line-height:1.15;box-shadow:0 0 0 4px #fff,0 0 0 8px #b23a2e">${fila('Instalar y crear acceso directo', 'padding:16px 16px')}</div>
    ${fila('Sitio para computadoras')}
  </div>
  <div style="position:absolute;left:10px;right:10px;top:640px;background:#fff;border-radius:28px;padding:24px 22px 22px;box-shadow:0 -4px 30px rgba(60,64,67,.35)">
    <div style="text-align:center;font-size:27px;margin-bottom:16px">Instalar y crear acceso directo</div>
    <div class="paso" data-paso="3" data-txt="Tocá «Instalar»" style="position:relative;display:flex;align-items:center;gap:16px;background:#fde7e4;border-radius:18px;padding:14px 16px;font-size:27px;font-weight:600;box-shadow:0 0 0 4px #fff,0 0 0 8px #b23a2e">
      <div style="width:64px;height:64px;border-radius:16px;background:#f2b705;display:grid;place-items:center;padding:7px;flex:none">${ISOTIPO}</div><div style="flex:1">Instalar</div><div style="font-size:22px;font-weight:600;color:#b23a2e">1.ª opción</div>
</div>
    <div style="display:flex;align-items:center;gap:16px;margin-top:16px;padding:12px 16px;font-size:23px;color:#5f6368">
      <div style="width:64px;height:64px;border-radius:16px;background:#f2b705;display:grid;place-items:center;padding:7px;flex:none;opacity:.6">${ISOTIPO}</div><div style="flex:1"><div style="font-size:26px">Crear acceso directo</div>Los accesos directos se abren en Chrome</div></div>
  </div>
  </div>`
})()

// Safari en iPhone: la barra de abajo con los tres puntitos, el menú con «Compartir» y, aparte,
// el pedazo de la hoja de compartir donde está «Agregar a Inicio» (hay que bajar en la lista).
const MENU_SAFARI = (() => {
  const fila = (t, extra = '') => `<div style="padding:17px 22px;display:flex;gap:18px;align-items:center;font-size:26px;${extra}"><i style="width:28px;height:28px;border-radius:7px;border:3px solid #6e6e73;flex:none"></i>${t}</div>`
  const marca = (n, pos) => `<b style="${NUM};${pos};z-index:3">${n}</b>`
  return `<div style="position:absolute;inset:62px 0 0 0;font-family:Roboto,Arial,sans-serif;color:#1c1c1e">
  <img src="../../capturas/explorar.png" style="position:absolute;left:0;top:0;width:100%"><div style="position:absolute;inset:0;background:rgba(40,30,20,.22)"></div>
  <div style="position:absolute;right:14px;bottom:176px;width:340px;background:rgba(250,250,252,.97);border-radius:30px;box-shadow:0 12px 40px rgba(0,0,0,.35);padding:10px 0">
    <div class="paso" data-paso="2" data-txt="Elegí «Compartir»" style="position:relative;margin:4px 10px;border-radius:18px;background:#fde7e4;font-weight:600;box-shadow:0 0 0 4px #fff,0 0 0 8px #b23a2e">${fila('Compartir')}</div>
    ${fila('Agregar a Marcadores')}
    ${fila('Agregar marcador a…')}
    <div style="border-top:2px solid #d8d8dc;margin:6px 22px"></div>
    ${fila('Nueva pestaña')}
    ${fila('Nueva pestaña privada')}
  </div>
  <div style="position:absolute;left:18px;right:18px;bottom:30px;height:86px;display:flex;gap:14px;align-items:center">
    <div style="width:86px;height:86px;border-radius:50%;background:#fff;box-shadow:0 4px 14px rgba(0,0,0,.2);display:grid;place-items:center;font-size:40px">‹</div>
    <div style="flex:1;height:86px;border-radius:43px;background:#fff;box-shadow:0 4px 14px rgba(0,0,0,.2);display:flex;align-items:center;justify-content:center;font-size:25px;color:#3a3a3c">burgometro.com.ar</div>
    <div class="paso" data-paso="1" data-txt="Tocá los 3 puntitos de abajo" style="position:relative;width:86px;height:86px;border-radius:50%;background:#fde7e4;box-shadow:0 0 0 4px #fff,0 0 0 8px #b23a2e;display:grid;place-items:center;"><svg width="44" height="12" viewBox="0 0 44 12"><circle cx="6" cy="6" r="5" fill="#1c1c1e"/><circle cx="22" cy="6" r="5" fill="#1c1c1e"/><circle cx="38" cy="6" r="5" fill="#1c1c1e"/></svg></div>
  </div>
  </div>`
})()

// La hoja de compartir de Safari, recortada a lo que importa; va en la lámina del iPhone como la historia en la 9.
const HOJA_COMPARTIR = `<div style="width:430px;font-family:Roboto,Arial,sans-serif;color:#1c1c1e;background:#f2f2f7;border-radius:34px;padding:18px;box-sizing:border-box">
  <div style="background:#fff;border-radius:24px;padding:6px 0">
    <div style="padding:20px 24px;display:flex;gap:18px;align-items:center;font-size:27px"><i style="width:30px;height:30px;border-radius:7px;border:3px solid #6e6e73;flex:none"></i>Buscar en la página</div>
    <div class="paso" data-paso="3" data-txt="Bajá y elegí «Agregar a Inicio»" style="position:relative;margin:4px 10px;padding:18px 16px;border-radius:18px;background:#fde7e4;display:flex;gap:18px;align-items:center;font-size:27px;font-weight:600;box-shadow:0 0 0 4px #fff,0 0 0 8px #b23a2e"><i style="width:32px;height:32px;border-radius:8px;border:3px solid #1c1c1e;flex:none;display:grid;place-items:center;font-style:normal;font-size:26px;line-height:1">+</i>Agregar a Inicio</div>
    <div style="padding:20px 24px;display:flex;gap:18px;align-items:center;font-size:27px"><i style="width:30px;height:30px;border-radius:50%;border:3px solid #6e6e73;flex:none"></i>Marcado</div>
  </div></div>`

const LAMINAS = [
  { id: '01-portada', h: 'Todo lo que podés hacer en Burgómetro', m: true, sub: '+1.200 hamburgueserías en Buenos Aires y alrededores, en un solo lugar.', subTop: 470, sticker: ['deslizá', '→'], stTop: 790, phone: { img: 'explorar.png', notas: [
    { ring: [16, 122, 343, 46], chip: { t: 'Buscá acá' } },
    { ring: [16, 178, 169, 44], chip: { t: 'Filtrá por barrio' } },
  ] } },
  { id: '02-sugerir-local', h: '¿Falta tu hamburguesería?', sub: '<b>En Explorar:</b> si buscás tu local y no aparece, sumalo. Pasanos el dato y lo agregamos.', subTop: 400, sticker: ['pasanos', 'el dato'], stTop: 700, phone: { img: 'sugerir-local.png', barra: '#9b958b', notas: [
    { ring: [20, 338, 335, 138], chip: { t: 'Completá los datos' } },
    { ring: [20, 743, 335, 48], chip: { t: 'Enviá la sugerencia' } },
  ] } },
  { id: '03a-instalar-android', h: 'Instalala<br>en Android', sub: 'Queda con el ícono y el nombre de Burgómetro.', subSize: 35, subW: 440, subTop: 400, sticker: ['como', 'una app'], stTop: 700, phone: { html: MENU_CHROME, barra: '#f1f3f4' } },
  { id: '03b-instalar-iphone', h: 'Instalala<br>en iPhone', sub: 'Queda con el ícono de Burgómetro en tu inicio.', subSize: 35, subW: 440, subTop: 400, hoja: true, phone: { html: MENU_SAFARI, barra: '#f5f0e1' } },
  { id: '04-recorrido', h: 'Armá tu<br>recorrido', sub: '<b>En Tour.</b> Elegí paradas y distancia. Te armamos la ruta.', subTop: 400, sticker: ['a pie', 'o en auto'], stTop: 700, phone: { img: 'tour.png', notas: [
    { ring: [32, 94, 311, 52], chip: { t: 'Elegí a pie o en auto' } },
    { ring: [32, 612, 311, 44], chip: { t: 'Tocá «Armar tour»' } },
  ] } },
  { id: '05-buscar-gente', h: 'Buscá gente y seguila', sub: '<b>En Feed.</b> Encontrá a otros fanáticos y mirá qué recomiendan.', subTop: 400, sticker: ['mirá qué', 'comen'], stTop: 700, phone: { img: 'buscar-gente.png', notas: [
    { ring: [16, 79, 343, 52], chip: { t: 'Buscá por usuario' } },
    { ring: [258, 391, 89, 35], chip: { t: 'Tocá «Seguir»' } },
  ] } },
  { id: '06-avatar', h: 'Armá tu hamburguesa', sub: '<b>En Perfil.</b> Es tu cara en Burgómetro.', subTop: 400, sticker: ['a tu', 'gusto'], stTop: 700, phone: { img: 'avatar.png', notas: [
    { ring: [12, 486, 351, 276], chip: { t: 'Elegí fondo, pan y queso' } },
  ] } },
  { id: '07-guardadas', h: 'Guardá las<br>que te debés', sub: '<b>En Perfil.</b> Tocá el corazón y encontralas en Guardadas.', subTop: 400, sticker: ['para', 'después'], stTop: 700, phone: { img: 'guardadas.png', notas: [
    { ring: [303, 142, 44, 44], chip: { t: 'Tocá el corazón' } },
  ] } },
  { id: '09-compartir-resenia', h: 'Compartí<br>tu reseña', sub: '<b>En Perfil.</b> Un toque y te armamos la historia.', subTop: 400, historia: 'historia-generada.png', phone: { img: 'resenia-compartir.png', notas: [
    { ring: [32, 541, 190, 44], chip: { t: 'Tocá «Compartir en historias»' } },
  ] } },
]

const SCRIPT = `<script>
const run = () => {
  const P = document.getElementById('pasos')
  const sb = document.querySelector('.sub').getBoundingClientRect()
  const els = [...document.querySelectorAll('[data-paso]')].sort((a, b) => a.dataset.paso - b.dataset.paso)
  let prev = sb.bottom + 40, first = null
  const xx = document.getElementById('extra')
  if (xx && window.ARRIBA) { xx.style.top = (sb.bottom + 24) + 'px'; prev = xx.getBoundingClientRect().bottom + 24 }
  els.forEach(e => {
    const c = document.createElement('div'); c.className = 'pc'; c.innerHTML = '<span class="num">' + e.dataset.paso + '</span><span>' + e.dataset.txt + '</span>'; P.appendChild(c)
    const t = e.getBoundingClientRect(), h = c.offsetHeight
    const top = Math.min(window.APILAR ? prev : Math.max(prev, t.top + t.height / 2 - h / 2), Math.max(prev, window.TOPE - h))
    if (first === null) first = top
    c.style.top = top + 'px'; prev = top + h + 18
  })
  const x = document.getElementById('extra')
  if (x) x.style.top = (window.ARRIBA ? Math.max(sb.bottom + 30, first - x.offsetHeight - 24) : prev - 6) + 'px'
  const svg = document.getElementById('flechas'), bajo = document.getElementById('flechasBajo')
  svg.setAttribute('height', document.documentElement.clientHeight)
  bajo.setAttribute('height', document.documentElement.clientHeight)
  let d = '', db = ''
  els.forEach((e, i) => {
    const c = P.children[i].getBoundingClientRect(), t = e.getBoundingClientRect()
    let sx = c.right + 6, sy = c.top + c.height / 2, ex = t.left - 12, ey = t.top + t.height / 2, izq = false
    if (ex < sx) { izq = true; sx = c.left + 40; sy = c.bottom + 6 }
    const k = Math.max((ex - sx) * 0.5, 20)
    const largo = window.APILAR && i === 0
    const path = largo ? 'M' + sx + ' ' + sy + ' C' + (sx + 30) + ' ' + (sy + 520) + ' ' + (ex - 330) + ' ' + (ey + 70) + ' ' + ex + ' ' + ey : izq ? 'M' + sx + ' ' + sy + ' C' + sx + ' ' + (sy + 90) + ' ' + (ex - 90) + ' ' + ey + ' ' + ex + ' ' + ey : 'M' + sx + ' ' + sy + ' C' + (sx + k) + ' ' + sy + ' ' + (ex - k) + ' ' + ey + ' ' + ex + ' ' + ey
    const pts = [[ex, ey], [ex - 25, ey - 15], [ex - 25, ey + 15]].map(p => p.join(',')).join(' ')
    let g = '<path d="' + path + '" fill="none" stroke="#fff7e0" stroke-width="12" stroke-linecap="round"/><polygon points="' + pts + '" fill="#fff7e0" stroke="#fff7e0" stroke-width="8" stroke-linejoin="round"/>'
    g += '<path d="' + path + '" fill="none" stroke="#2b1a10" stroke-width="5.5" stroke-linecap="round"/><polygon points="' + pts + '" fill="#2b1a10"/>'
    // en la lámina del iPhone la flecha 1 es larguísima: va por detrás del celu y el número rojo del aro la completa
    d += g
  })
  bajo.innerHTML = db
  svg.innerHTML = d
}
;(document.fonts ? document.fonts.ready : Promise.resolve()).then(() => setTimeout(run, 400))
</script>`

function html(l, F) {
  const esPortada = l.id === '01-portada'
  const phone = celu({ img: l.phone.img, barra: l.phone.barra, pantallaHtml: l.phone.html, notas: esPortada ? [] : l.phone.notas }, F)
  const subTop = l.subTop + F.sub, stTop = l.stTop + F.st
  const extra = l.hoja
    ? `<div id="extra" class="extra"><div style="zoom:${F.alto > 1400 ? 1 : 0.7}">${HOJA_COMPARTIR}</div></div>`
    : l.historia
      ? `<div id="extra" class="extra"><div class="cap">Así queda tu historia</div><div class="historia" style="position:relative;left:8px;width:${F.hist.w}px"><img src="../../capturas/${l.historia}"></div></div>`
      : ''
  return `<!doctype html><meta charset="utf-8"><title>${l.id}</title><style>${CSS}html,body{height:${F.alto}px}.logo{bottom:${F.logo}px}</style>
<div class="marca">${ISOTIPO}</div>
<h1 class="${l.m ? 'm' : ''}" style="top:${F.h1}px">${l.h}</h1>
<p class="sub" style="top:${subTop}px;${l.subSize ? `font-size:${l.subSize}px;width:${l.subW ?? 380}px` : ''}">${l.sub}</p>
${rayas(486, subTop - 12, -60)}
<svg id="flechasBajo" xmlns="http://www.w3.org/2000/svg"></svg>
${phone}
${esPortada ? `<div class="sticker" style="top:${stTop}px">${l.sticker[0]}<br>${l.sticker[1]}<u></u></div>${destello(282, stTop + 170, 215)}` : `<div id="pasos"></div>${extra}${l.sticker ? `<div class="sello-chico" style="left:70px;top:${F.alto - F.logo - 94 - F.sello - 34}px"><div class="sticker" style="position:absolute;left:0;top:0;transform-origin:top left;transform:rotate(-9deg) scale(${F.sello / 340})">${l.sticker[0]}<br>${l.sticker[1]}<u></u></div>${destello(F.sello * 0.62, F.sello * 0.1, F.sello * 0.52)}</div>` : ''}`}
<svg id="flechas" xmlns="http://www.w3.org/2000/svg"></svg>
<div class="logo">${ISOTIPO}<span>Burgómetro</span></div><script>window.APILAR=true;window.ARRIBA=${!!l.historia};window.TOPE=${F.alto - F.logo - 130}</script>${SCRIPT}`
}

// Cierre: sin celu, el logo grande y la dirección.
const cierre = (F) => `<!doctype html><meta charset="utf-8"><title>08-cierre</title><style>${CSS}html,body{height:${F.alto}px}
.grande{position:absolute;left:0;right:0;top:${F.c.g}px;text-align:center}
.grande svg{width:430px;display:block;margin:0 auto}
.grande .n{font-weight:800;font-size:128px;letter-spacing:-3px;margin-top:24px}
.url{position:absolute;left:0;right:0;top:${F.c.u}px;text-align:center}
.url span{display:inline-block;background:#3a2a1e;color:#f7efd8;border-radius:60px;padding:26px 58px;font-weight:800;font-size:54px}
</style>
<div class="marca" style="right:-190px;top:-150px;width:700px">${ISOTIPO}</div>
<h1 style="text-align:center;left:0;right:0;width:auto;font-size:84px;top:${F.c.h1}px">Descubrí. Calificá. Recorré.</h1>
<div class="grande">${ISOTIPO}<div class="n">Burgómetro</div></div>
<div class="url"><span>burgometro.com.ar</span></div>
<p class="sub" style="left:0;right:0;width:auto;text-align:center;top:${F.c.s}px;font-size:36px">Entrá desde el link de la bio.</p>`

for (const F of FORMATOS) {
  const salida = path.join(AQUI, F.dir)
  const dirHtml = path.join(salida, 'html')
  fs.mkdirSync(dirHtml, { recursive: true })
  const todas = [...LAMINAS.map(l => [l.id, html(l, F)]), ['08-cierre', cierre(F)]]
  for (const [id, h] of todas) {
    const f = path.join(dirHtml, id + '.html')
    fs.writeFileSync(f, h)
    const r = spawnSync(EDGE, ['--headless=new', '--disable-gpu', '--hide-scrollbars', '--force-device-scale-factor=1', `--window-size=1080,${F.alto}`, '--virtual-time-budget=4000', `--screenshot=${path.join(salida, id + '.png')}`, 'file:///' + f.replaceAll('\\', '/')], { stdio: 'ignore' })
    console.log(F.dir, id, r.status)
  }
}
