-- El podio de personas por semana y por mes (#318).
--
-- Cada cierre guarda quiénes salieron en los primeros puestos de ese período y cuánta XP
-- tenían. Es un resultado que no se recalcula después: si más tarde se borra una reseña, el
-- podio de esa semana sigue siendo el que fue, y los logros que dio también.
create table resultados_de_podio (
    periodo  varchar(10) not null,
    desde    date        not null,
    puesto   integer     not null,
    user_id  bigint      not null references users (id) on delete cascade,
    xp       integer     not null,
    primary key (periodo, desde, puesto)
);
