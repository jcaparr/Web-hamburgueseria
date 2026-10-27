/**
 * El catálogo de logros (#316): para cada código, su nombre, grupo, qué es, cómo se dibuja
 * y cómo se cuenta lo que falta. Los códigos son los de `Logro.java` y no cambian nunca;
 * los nombres y los textos viven acá.
 */

export type GrupoDeLogros = 'Niveles' | 'Seguidores' | 'Barrios' | 'Ranking' | 'Actividad' | 'Especiales' | 'Secretos'

export const GRUPOS: GrupoDeLogros[] = ['Niveles', 'Seguidores', 'Barrios', 'Ranking', 'Actividad', 'Especiales', 'Secretos']

const ORO = '#f2b705'
const KET = '#b23a2e'
const SOC = '#3b6e78'
const BARC = '#9e5c0e'
const ACT = '#52753a'
const REA = '#8f1d14'
const CREMA = '#f3e8cf'
const BRONCE = '#c98a3d'
const PLATA = '#c9c3b5'
const PODIO = '#7a4526'

/** Cómo se dice cuánto falta en los logros que se miden con un número. */
type Medida = (actual: number, meta: number) => string

const nivel: Medida = (a, m) => `Nivel ${a} de ${m}`
const de = (cosa: string): Medida => (a, m) => `${a.toLocaleString('es-AR')} de ${m.toLocaleString('es-AR')} ${cosa}`

export interface DatosDeLogro {
  codigo: string
  nombre: string
  grupo: GrupoDeLogros
  /** Qué hay que hacer para ganarlo. */
  texto: string
  /** El dibujo de adentro de la chapita, en una grilla de 48. */
  icono: string
  /** El mismo dibujo con menos trazos, para las chapitas chicas. */
  iconoChico?: string
  relleno: string
  borde: string
  /** El chip con el número de los de serie. */
  numero?: string
  secreto?: boolean
  medida?: Medida
}

function logro(
  codigo: string,
  nombre: string,
  grupo: GrupoDeLogros,
  relleno: string,
  borde: string,
  numero: string | undefined,
  texto: string,
  icono: string,
  extra: { secreto?: boolean; medida?: Medida; iconoChico?: string } = {},
): DatosDeLogro {
  return { codigo, nombre, grupo, relleno, borde, numero, texto, icono, ...extra }
}

const IC = {
  paladar: 'M14 17h20l-2.5 23h-15zM12 17h24M24 17V8l7-2M17 25h14M18.5 32h11',
  top3sem: 'M6 40V29h10v11M16 40V20h12v20M28 40V33h10v7M4 40h40M22 8l1.5 3 3.3.5-2.4 2.3.6 3.2-3-1.6-3 1.6.6-3.2-2.4-2.3 3.3-.5z',
  primeroMes: 'M16 9h16v9a8 8 0 0 1-16 0zM16 12h-5a5 5 0 0 0 5 8M32 12h5a5 5 0 0 1-5 8M24 26v8M17 40h14M20 34h8',
  racha2: 'M24 42c-7 0-12-5-12-11 0-6 5-9 6-16 3 2 5 5 5 8 2-2 3-5 3-8 5 4 10 9 10 15 0 7-5 12-12 12zM24 38a5 5 0 0 1-5-5c0-3 3-5 5-8 2 3 5 5 5 8a5 5 0 0 1-5 5z',
  pionero: 'M24 24a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM19 31l-4 11 9-4 9 4-4-11M22 13l2-2v10',
  menu: 'M7 35h34M11 35a13 13 0 0 1 26 0M24 19v-3M21 14h6M6 41h36',
  slider: 'M15 25a9 7 0 0 1 18 0zM14 29h20M16 33h16v1a3 3 0 0 1-3 3H19a3 3 0 0 1-3-3z',
  triple: 'M12 15a12 7 0 0 1 24 0zM11 20h26M11 26h26M11 32h26M18 20v3M30 26v3M22 32v3M13 37h22',
  bro: 'M9 25a15 11 0 0 1 30 0zM14 17h7v3a3.5 3.5 0 0 1-7 0zM27 17h7v3a3.5 3.5 0 0 1-7 0zM21 18h6M8 30h32M11 35h26v1a3 3 0 0 1-3 3H14a3 3 0 0 1-3-3z',
  lover: 'M24 40S10 31 10 21a7.5 7.5 0 0 1 14-3.5A7.5 7.5 0 0 1 38 21c0 10-14 19-14 19zM17 23h14M18 28h12',
  smasher: 'M24 25V9M20 9h8M14 25h20l-2 6H16zM9 37h30M7 33l-3-2M41 33l3-2',
  hamburguesero: 'M15 17l2-9 7 5 7-5 2 9zM11 29a13 8 0 0 1 26 0zM10 33h28M12 37h24',
  nano: 'M22 23a5 5 0 1 0 0-10 5 5 0 0 0 0 10zM13 37a9 9 0 0 1 18 0M36 9l1.3 2.7 3 .4-2.2 2 .6 3-2.7-1.5-2.7 1.5.6-3-2.2-2 3-.4z',
  micro: 'M17 22a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9zM9 36a8 8 0 0 1 16 0M31 22a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9zM25 31a8 8 0 0 1 14 5',
  influ: 'M9 20v8h5l14 8V12l-14 8zM14 28l2 8h4l-2-8M33 19a6 6 0 0 1 0 10M37 15a11 11 0 0 1 0 18',
  giga: 'M24 7l5 10 11 1.6-8 7.8 2 11-10-5.3-10 5.3 2-11-8-7.8L19 17z',
  chico: 'M16 17a8 6 0 0 1 16 0zM15 21h18M16 25h16v1a2 2 0 0 1-2 2H18a2 2 0 0 1-2-2zM12 41a12 10 0 0 1 24 0M19 8l2-3 3 2 3-2 2 3',
  esquina: 'M24 10v30M24 12h12l3 3-3 3H24M24 22H12l-3 3 3 3h12M18 40h12',
  barrio: 'M14 26s-6-5-6-10a6 6 0 0 1 12 0c0 5-6 10-6 10zM34 38s-6-5-6-10a6 6 0 0 1 12 0c0 5-6 10-6 10zM15 31c3 5 7 3 10 4',
  conquista: 'M6 39c6-10 30-10 36 0M24 32V9M24 10l12 4-12 4',
  duenio: 'M8 40V25h8v15M16 40V15h10v25M26 40V21h8v19M34 40V29h6v11M5 40h38M16 12l1-6 3 3 1-5 2 5 3-3 1 6',
  timido: 'M9 13h30v17H22l-7 6v-6H9zM17 21.5h.01M24 21.5h.01M31 21.5h.01',
  picasso: 'M24 8C14 8 7 14 7 22s7 16 15 16c3 0 3-3 1-5-2-3 1-6 4-5 6 2 14-1 14-8 0-7-7-12-17-12zM15 20h.01M21 14h.01M29 14h.01M34 21h.01',
  planif: 'M13 11h22v29H13zM20 8h8v5h-8zM17 21l2 2 4-4M17 30l2 2 4-4M27 21h4M27 30h4',
  ansioso: 'M24 41a13 13 0 1 0 0-26 13 13 0 0 0 0 26zM24 28v-7M20 8h8M24 8v7M35 16l3-3',
  tour: 'M8 13l10-4 12 4 10-4v26l-10 4-12-4-10 4zM18 9v26M30 13v26',
  guia: 'M15 8v32M15 10h19l-4 5 4 5H15M11 40h8',
  album: 'M14 11h25v19M9 16h25v21H9zM11 34l7-7 5 5 3-3 7 6M26 23h.01',
  stalker: 'M8 31a6 6 0 1 0 12 0 6 6 0 1 0-12 0zM28 31a6 6 0 1 0 12 0 6 6 0 1 0-12 0zM20 30h8M9 26l4-13h5l2 13M39 26l-4-13h-5l-2 13',
  promesa: 'M24 39S11 31 11 21.5a6.8 6.8 0 0 1 13-3 6.8 6.8 0 0 1 13 3C37 31 24 39 24 39zM18.5 24.5l4 4 7.5-7.5',
  autor: 'M15 27a6 6 0 0 1 2-11.7A7.5 7.5 0 0 1 31 15.3a6 6 0 0 1 2 11.7v9H15zM15 31h18',
  intendente: 'M8 39h32M11 39V25M19 39V25M29 39V25M37 39V25M7 25l17-10 17 10zM24 15V8l6 2-6 2',
  combo: 'M20.8 14.0a3.2 3.2 0 1 0 6.4 0 3.2 3.2 0 1 0-6.4 0zM30.3 20.9a3.2 3.2 0 1 0 6.4 0 3.2 3.2 0 1 0-6.4 0zM26.7 32.1a3.2 3.2 0 1 0 6.4 0 3.2 3.2 0 1 0-6.4 0zM14.9 32.1a3.2 3.2 0 1 0 6.4 0 3.2 3.2 0 1 0-6.4 0zM11.3 20.9a3.2 3.2 0 1 0 6.4 0 3.2 3.2 0 1 0-6.4 0z',
  boca: 'M8 12h20a3 3 0 0 1 3 3v9a3 3 0 0 1-3 3H17l-6 5v-5H8a3 3 0 0 1-3-3v-9a3 3 0 0 1 3-3zM35 19h5a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3h-2v4l-5-4h-8',
  comediante: 'M19 10a5 5 0 0 1 10 0v11a5 5 0 0 1-10 0zM13 20a11 11 0 0 0 22 0M24 31v7M18 39h12',
  bajonero: 'M12 17c0-4 5-6 12-6s12 2 12 6v10a12 12 0 0 1-24 0zM12 17l-2-8 6 4M36 17l2-8-6 4M15 21a4 4 0 1 0 8 0 4 4 0 1 0-8 0zM25 21a4 4 0 1 0 8 0 4 4 0 1 0-8 0zM22.5 27l1.5 2.5 1.5-2.5z',
  patriota: 'M9 12h30v27H9zM9 19h30M16 8v7M32 8v7M16 25.5h5.5V29H16v4h5.5M26.5 25.5H32V33h-5.5zM26.5 29H32',
  hambre: 'M15 15h4M29 15h4M11 22c3 9 23 9 26 0zM31 29s-3 4-3 6.5a3 3 0 0 0 6 0C34 33 31 29 31 29z',
}

const IC_CHICO: Record<string, string> = {
  triple: 'M12 18a12 8 0 0 1 24 0zM11 25h26M12 33h24',
  duenio: 'M9 40V24h9v16M18 40V12h12v28M30 40V20h9v20M5 40h38',
  stalker: 'M7 28a7 7 0 1 0 14 0 7 7 0 1 0-14 0zM27 28a7 7 0 1 0 14 0 7 7 0 1 0-14 0zM21 27h6',
  planif: 'M13 11h22v29H13zM20 8h8v5h-8zM18 26l4 4 8-8',
  album: 'M9 13h30v24H9zM11 34l8-8 6 6 4-4 8 7M30 20h.01',
  nano: 'M24 23a6 6 0 1 0 0-12 6 6 0 0 0 0 12zM13 38a11 11 0 0 1 22 0',
}

/** Lo que dice el aviso al ganarlo, en pasado y hablándole a quien lo ganó. */
export const AVISOS: Record<string, string> = {
  NIVEL_1: 'Te damos la bienvenida a Burgómetro. Armá tu hamburguesa y empezá a reseñar.',
  NIVEL_3: 'Llegaste al nivel 3. Ya podés ponerle huevo frito a tu hamburguesa.',
  NIVEL_5: 'Llegaste al nivel 5. Ya podés ponerle bacon a tu hamburguesa.',
  NIVEL_10: 'Llegaste al nivel 10. Ya podés sumarle la cuarta carne.',
  NIVEL_15: 'Llegaste al nivel 15. Ya podés usar el fondo de rayos.',
  NIVEL_20: 'Llegaste al nivel 20, el máximo. Tu avatar lleva el marco punteado.',
  SEGUIDORES_10: 'Ya tenés 10 seguidores.',
  SEGUIDORES_50: 'Ya tenés 50 seguidores.',
  SEGUIDORES_100: 'Ya tenés 100 seguidores.',
  SEGUIDORES_500: 'Ya tenés 500 seguidores.',
  SEGUIDORES_1000: 'Ya tenés 1.000 seguidores.',
  BARRIO_ESQUINA: 'Reseñaste 3 locales del mismo barrio.',
  BARRIOS_5: 'Reseñaste locales en 5 barrios distintos.',
  BARRIOS_15: 'Reseñaste locales en 15 barrios distintos.',
  BARRIOS_25: 'Reseñaste locales en 25 barrios distintos.',
  INTENDENTE: 'Sos quien más reseñó en un barrio.',
  PODIO_SEMANAL: 'Terminaste entre las 3 personas con más XP de la semana.',
  PRIMERO_DEL_MES: 'Terminaste primero en el ranking del mes.',
  RACHA_DE_ORO: 'Terminaste primero en el ranking del mes 2 meses seguidos.',
  PRIMERA_RESENIA: 'Publicaste tu primera reseña.',
  PICASSO: 'Armaste tu hamburguesa.',
  ANSIOSO: 'Fuiste la primera persona en reseñar un local.',
  HAMBRE_AJENA: 'Una reseña tuya juntó 10 reacciones.',
  PLANIFICADOR: 'Guardaste 10 hamburgueserías.',
  STALKER: 'Reaccionaste a 30 reseñas distintas.',
  PRIMER_TOUR: 'Guardaste tu primer recorrido.',
  GUIA: 'Guardaste 5 recorridos.',
  ALBUM: 'Publicaste una reseña con 3 fotos.',
  PROMESA: 'Reseñaste 5 locales que tenías guardados antes de ir.',
  DE_AUTOR: 'Reseñaste 10 locales que no son cadenas.',
  PIONERO: 'Fuiste una de las primeras 10 personas en crear su cuenta.',
  MENU_COMPLETO: 'Ganaste todos los logros de Burgómetro.',
  COMBO: 'Una reseña tuya recibió los 5 tipos de reacción.',
  BOCA_EN_BOCA: '3 personas que te siguen reseñaron un local después que vos.',
  COMEDIANTE: 'Tus reseñas juntaron 10 reacciones de risa.',
  BAJONERO: 'Publicaste una reseña entre la 1 y las 7 de la mañana.',
  PATRIOTA: 'Publicaste una reseña el 28 de mayo, Día de la Hamburguesa.',
  PALADAR: "Reseñaste Burger King, McDonald's y Wendy's. Cadena tras cadena.",
}

/** Los que cambian la hamburguesa al llegar a su nivel: el aviso muestra la capa cayendo sobre la tuya. */
export const DESBLOQUEOS: Record<string, { capa: 'HUEVO' | 'BACON' | 'CARNE-4'; premio: 'huevo' | 'bacon' | 'carnes'; boton: string }> = {
  NIVEL_3: { capa: 'HUEVO', premio: 'huevo', boton: 'Probar el huevo' },
  NIVEL_5: { capa: 'BACON', premio: 'bacon', boton: 'Probar el bacon' },
  NIVEL_10: { capa: 'CARNE-4', premio: 'carnes', boton: 'Probar la cuarta carne' },
}

/** Todos, en el orden en que se muestran dentro de cada grupo. */
export const CATALOGO: DatosDeLogro[] = [
  logro('NIVEL_1', 'Slider', 'Niveles', KET, ORO, '1', 'Creaste tu cuenta de Burgómetro.', IC.slider),
  logro('NIVEL_3', 'Triplequé', 'Niveles', KET, ORO, '3', 'Llegá al nivel 3. Desbloquea el huevo frito.', IC.triple, { medida: nivel, iconoChico: IC_CHICO.triple }),
  logro('NIVEL_5', 'Burger bro', 'Niveles', KET, ORO, '5', 'Llegá al nivel 5. Desbloquea el bacon.', IC.bro, { medida: nivel }),
  logro('NIVEL_10', 'Burger lover', 'Niveles', KET, ORO, '10', 'Llegá al nivel 10. Desbloquea la cuarta carne.', IC.lover, { medida: nivel }),
  logro('NIVEL_15', 'Smasher', 'Niveles', KET, ORO, '15', 'Llegá al nivel 15. Desbloquea un fondo especial.', IC.smasher, { medida: nivel }),
  logro('NIVEL_20', 'Hamburguesero', 'Niveles', KET, ORO, '20', 'Llegá al nivel 20, el máximo. Tu avatar lleva el marco punteado.', IC.hamburguesero, { medida: nivel }),

  logro('SEGUIDORES_10', 'Nano influencer', 'Seguidores', SOC, BRONCE, '10', 'Juntá 10 seguidores.', IC.nano, { medida: de('seguidores'), iconoChico: IC_CHICO.nano }),
  logro('SEGUIDORES_50', 'Micro influencer', 'Seguidores', SOC, PLATA, '50', 'Juntá 50 seguidores.', IC.micro, { medida: de('seguidores') }),
  logro('SEGUIDORES_100', 'Influencer', 'Seguidores', SOC, ORO, '100', 'Juntá 100 seguidores.', IC.influ, { medida: de('seguidores') }),
  logro('SEGUIDORES_500', 'Giga influencer', 'Seguidores', SOC, ORO, '500', 'Juntá 500 seguidores.', IC.giga, { medida: de('seguidores') }),
  logro('SEGUIDORES_1000', 'Fenómeno hamburguesero', 'Seguidores', SOC, KET, '1.000', 'Juntá 1.000 seguidores.', IC.chico, { medida: de('seguidores') }),

  logro('BARRIO_ESQUINA', 'Cliente de la esquina', 'Barrios', BARC, CREMA, '3', 'Reseñá 3 locales del mismo barrio.', IC.esquina, { medida: de('en el mismo barrio') }),
  logro('BARRIOS_5', 'Barrio por barrio', 'Barrios', BARC, CREMA, '5', 'Reseñá locales en 5 barrios distintos.', IC.barrio, { medida: de('barrios') }),
  logro('BARRIOS_15', 'Conquistador de barrios', 'Barrios', BARC, CREMA, '15', 'Reseñá locales en 15 barrios distintos.', IC.conquista, { medida: de('barrios') }),
  logro('BARRIOS_25', 'Dueño de la ciudad', 'Barrios', BARC, ORO, '25', 'Reseñá locales en 25 barrios distintos.', IC.duenio, { medida: de('barrios'), iconoChico: IC_CHICO.duenio }),
  logro('INTENDENTE', 'Intendente de barrio', 'Barrios', BARC, ORO, undefined, 'Sé quien más reseñó en un barrio (mínimo 2 reseñas). Si otra persona te pasa, lo perdés.', IC.intendente),

  logro('PODIO_SEMANAL', 'Podio semanal', 'Ranking', PODIO, PLATA, '3', 'Terminá entre las 3 personas con más XP de la semana.', IC.top3sem),
  logro('PRIMERO_DEL_MES', 'Número uno del mes', 'Ranking', PODIO, ORO, '1', 'Terminá primero en el ranking del mes, el de más XP ganada en ese mes.', IC.primeroMes),
  logro('RACHA_DE_ORO', 'Racha de oro', 'Ranking', KET, ORO, '2', 'Terminá primero en el ranking del mes 2 meses seguidos.', IC.racha2),

  logro('PRIMERA_RESENIA', 'Tímido', 'Actividad', ACT, CREMA, undefined, 'Publicá tu primera reseña.', IC.timido),
  logro('PICASSO', 'Picasso', 'Actividad', ACT, CREMA, undefined, 'Armá tu hamburguesa.', IC.picasso),
  logro('ANSIOSO', 'Ansioso', 'Actividad', ACT, CREMA, undefined, 'Sé la primera persona en reseñar un local, y con comentario.', IC.ansioso),
  logro('HAMBRE_AJENA', 'Hambre ajena', 'Actividad', REA, ORO, undefined, 'Una reseña tuya junta 10 reacciones.', IC.hambre, { medida: de('reacciones') }),
  logro('PLANIFICADOR', 'Planificador', 'Actividad', ACT, CREMA, undefined, 'Guardá 10 hamburgueserías con el corazón.', IC.planif, { medida: de('guardadas'), iconoChico: IC_CHICO.planif }),
  logro('STALKER', 'Stalker', 'Actividad', REA, CREMA, undefined, 'Reaccioná a 30 reseñas distintas.', IC.stalker, { medida: de('reseñas'), iconoChico: IC_CHICO.stalker }),
  logro('PRIMER_TOUR', 'Primer tour', 'Actividad', ACT, CREMA, undefined, 'Guardá tu primer recorrido.', IC.tour),
  logro('GUIA', 'Guía turístico', 'Actividad', ACT, ORO, undefined, 'Guardá 5 recorridos.', IC.guia, { medida: de('recorridos') }),
  logro('ALBUM', 'Álbum de medallones', 'Actividad', ACT, CREMA, undefined, 'Publicá una reseña con 3 fotos.', IC.album, { iconoChico: IC_CHICO.album }),
  logro('PROMESA', 'Promesa cumplida', 'Actividad', ACT, CREMA, '5', 'Reseñá 5 locales que tenías guardados antes de ir.', IC.promesa, { medida: de('locales') }),
  logro('DE_AUTOR', 'De autor', 'Actividad', BARC, CREMA, '10', 'Reseñá 10 locales que no son cadenas de comida rápida.', IC.autor, { medida: de('locales') }),

  logro('PIONERO', 'Pionero', 'Especiales', '#3a2a1e', ORO, '10', 'Fuiste una de las primeras 10 personas en crear su cuenta de Burgómetro.', IC.pionero),
  logro('MENU_COMPLETO', 'Menú completo', 'Especiales', REA, ORO, undefined, 'Ganá todos los demás logros, los secretos también.', IC.menu, { medida: de('logros') }),

  logro('COMBO', 'Combo completo', 'Secretos', REA, ORO, undefined, 'Una reseña tuya recibió los 5 tipos de reacción.', IC.combo, { secreto: true }),
  logro('BOCA_EN_BOCA', 'Boca en boca', 'Secretos', SOC, CREMA, '3', '3 personas que te siguen reseñaron un local después que vos.', IC.boca, { secreto: true }),
  logro('COMEDIANTE', 'Comediante', 'Secretos', REA, CREMA, '10', 'Tus reseñas juntaron 10 reacciones de risa.', IC.comediante, { secreto: true }),
  logro('BAJONERO', 'Bajonero noctámbulo', 'Secretos', ACT, CREMA, undefined, 'Publicaste una reseña entre la 1 y las 7 de la mañana.', IC.bajonero, { secreto: true }),
  logro('PATRIOTA', 'Patriota', 'Secretos', ACT, CREMA, undefined, 'Publicaste una reseña el 28 de mayo, Día de la Hamburguesa.', IC.patriota, { secreto: true }),
  logro('PALADAR', 'Paladar de plástico', 'Secretos', PODIO, CREMA, undefined, "Reseñaste Burger King, McDonald's y Wendy's. Ni el paladar se salva de la comida rápida.", IC.paladar, { secreto: true }),
]

export const POR_CODIGO: Record<string, DatosDeLogro> = Object.fromEntries(CATALOGO.map((l) => [l.codigo, l]))

/** Los que cuenta el título, "Logros (N de 32)": los secretos no. */
export const VISIBLES = CATALOGO.filter((l) => !l.secreto).length

/** El nombre de un logro; el Intendente lleva el barrio que lo hizo intendente. */
export function nombreDe(codigo: string, detalle?: string | null): string {
  const datos = POR_CODIGO[codigo]
  if (!datos) return codigo
  return codigo === 'INTENDENTE' && detalle ? `Intendente de ${detalle}` : datos.nombre
}

/** El borde dentado de la chapita: grande para las de 41 px o más y con menos dientes para las chicas. */
export const CHAPITA_GRANDE =
  'M24.00 3.00Q27.91 -1.94 30.19 3.93Q35.38 0.36 35.83 6.65Q41.84 4.77 40.42 10.91Q46.72 10.88 43.55 16.33Q49.58 18.16 44.94 22.43Q50.16 25.96 44.47 28.67Q48.42 33.58 42.19 34.50Q44.51 40.36 38.28 39.39Q38.78 45.68 33.11 42.92Q31.73 49.07 27.13 44.77Q24.00 50.23 20.87 44.77Q16.27 49.07 14.89 42.92Q9.22 45.68 9.72 39.39Q3.49 40.36 5.81 34.50Q-0.42 33.58 3.53 28.67Q-2.16 25.96 3.06 22.43Q-1.58 18.16 4.45 16.33Q1.28 10.88 7.58 10.91Q6.16 4.77 12.17 6.65Q12.62 0.36 17.81 3.93Q20.09 -1.94 24.00 3.00Z'
export const CHAPITA_CHICA =
  'M24.00 3.50Q29.60 -2.36 32.34 5.27Q39.84 2.20 39.23 10.28Q47.34 10.53 43.50 17.67Q50.80 21.18 44.39 26.14Q49.63 32.33 41.75 34.25Q44.03 42.03 36.05 40.58Q34.96 48.62 28.26 44.05Q24.00 50.95 19.74 44.05Q13.04 48.62 11.95 40.58Q3.97 42.03 6.25 34.25Q-1.63 32.33 3.61 26.14Q-2.80 21.18 4.50 17.67Q0.66 10.53 8.77 10.28Q8.16 2.20 15.66 5.27Q18.40 -2.36 24.00 3.50Z'
