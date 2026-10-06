// Aplica el tema elegido en Perfil > Ajustes antes de que se pinte la página, para que
// quien eligió el oscuro no vea un destello claro al entrar. Sin elección no hace nada:
// manda el tema del sistema.
//
// Va en un archivo y no escrito dentro de index.html porque la política de seguridad del
// servidor solo deja correr scripts propios servidos como archivo. La lógica es la misma
// que la de src/hooks/useTema.ts, que es quien la cambia después.
(function () {
  try {
    var tema = localStorage.getItem('tema')
    var nombres = { claro: 'burger', oscuro: 'burger-noche' }
    var barras = { claro: '#faf3e3', oscuro: '#17110d' }
    if (!nombres[tema]) return
    document.documentElement.setAttribute('data-theme', nombres[tema])
    var metas = document.querySelectorAll('meta[name="theme-color"]')
    for (var i = 0; i < metas.length; i++) metas[i].setAttribute('content', barras[tema])
  } catch {
    // Sin acceso al almacenamiento (navegación privada, permisos): queda el del sistema.
  }
})()
