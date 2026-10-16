-- Las cuentas nuevas arrancan mostrando sus guardadas y recorridos (#270).
--
-- Solo cambia el valor por defecto de la columna: las cuentas que ya existen quedan como
-- están. Quien tenía las listas ocultas no aceptó mostrarlas, y prenderlas por él sería
-- publicar algo suyo sin preguntar. El que quiera, las prende desde su perfil.
--
-- Si el código vuelve atrás, la columna queda con este valor y el código viejo siempre
-- manda el valor al crear la cuenta (arranca en falso): no pasa nada.
alter table users alter column muestra_listas set default true;

comment on column users.muestra_listas is
    'Si muestra sus guardadas y recorridos en su perfil público. Las cuentas nuevas arrancan en verdadero.';
