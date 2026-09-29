-- Cuándo se tocó una reseña por última vez.
--
-- Una reseña se edita en su lugar: hay una sola por persona y por hamburguesería, así
-- que cambiar de opinión pisa la de antes y la fecha original queda igual. En el feed
-- eso importa, porque sin esta columna no habría forma de decir que lo que se está
-- leyendo ya no es lo que se escribió ese día.
--
-- Queda en null mientras nadie la haya tocado. Podría arrancar igual a created_at y
-- comparar, pero entonces "nunca editada" y "editada en el mismo segundo" serían lo
-- mismo, y null dice exactamente lo que pasó: no se editó.
alter table ratings add column updated_at timestamp(6) with time zone;

-- El feed es siempre "las últimas primero", cortando por dónde iba la pantalla. El id
-- va en el índice porque dos reseñas pueden compartir el instante: sin él, el corte
-- entre una página y la siguiente podría repetir o saltear una.
create index if not exists idx_ratings_feed on ratings (created_at desc, id desc);

-- Y lo mismo restringido a un grupo de personas, que es la pestaña "Siguiendo".
create index if not exists idx_ratings_feed_por_usuario
    on ratings (user_id, created_at desc, id desc);
