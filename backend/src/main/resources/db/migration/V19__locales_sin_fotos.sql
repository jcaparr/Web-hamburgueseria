-- Los locales de los que Google no tiene ninguna foto.
--
-- No alcanza con mirar si photo_url está en nulo: de los 102 que hoy no tienen foto
-- guardada, la mitad sí las tiene en Google y lo que faltó fue ir a buscarlas. Confundir
-- las dos cosas escondería medio centenar de hamburgueserías reales por un trabajo
-- nuestro que quedó a medias.
--
-- Por eso se anota aparte, y solo cuando Google contesta que no tiene ninguna. Arranca
-- en false: mientras nadie haya preguntado, no se sabe.
alter table burger_joints
    add column sin_fotos_en_google boolean not null default false;

comment on column burger_joints.sin_fotos_en_google is
    'Google contestó que este local no tiene ninguna foto. Lo escribe la revisión de fotos.';
