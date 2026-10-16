-- Si cada persona muestra sus guardadas y sus recorridos en su perfil público (#264).
--
-- Arranca apagado para todos, también para las cuentas que ya existen: hasta ahora las
-- guardadas eran solo de quien las guardaba, y nadie aceptó mostrarlas. Cada uno lo
-- prende desde los ajustes de su perfil.
--
-- Si el código vuelve atrás, la columna queda y nadie la lee: las listas vuelven a ser
-- privadas, que es lo seguro.
alter table users add column muestra_listas boolean not null default false;

comment on column users.muestra_listas is
    'Si muestra sus guardadas y recorridos en su perfil público. Arranca en falso.';
