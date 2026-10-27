-- La experiencia (XP) de cada cuenta, de donde sale su nivel (#310).
--
-- Solo las reseñas dan XP: 100 la que tiene un comentario de verdad (80 letras o más y
-- 10 palabras o más), 70 la que tiene solo la foto, y 30 más de bienvenida a quien tiene
-- al menos una. Es la misma regla de Niveles.java; si una cambia, cambia la otra.
--
-- La columna es una copia derivada: la recalcula la app cada vez que cambian las reseñas
-- de la persona, así que borrarla y recalcular siempre deja el mismo número.
alter table users add column xp integer not null default 0;
alter table users add constraint users_xp_no_negativa check (xp >= 0);

-- Lo que ya merecían las cuentas con las reseñas que existen hoy.
update users u
set xp = coalesce((
    select sum(case
                   when length(regexp_replace(r.comment, '[^[:alpha:]]', '', 'g')) >= 80
                        and array_length(regexp_split_to_array(regexp_replace(r.comment, '^\s+|\s+$', '', 'g'), '\s+'), 1) >= 10
                       then 100
                   else 70
               end) + 30
    from ratings r
    where r.user_id = u.id
), 0);
