-- El motor de logros (#314).
--
-- Un logro por cuenta y por código. El código es estable (los nombres viven en el
-- frontend); ganado_el se escribe una vez y no cambia; activo se apaga si deja de
-- cumplirse (solo en los propios) y se prende de nuevo si vuelve; visto_el queda nulo
-- hasta que se muestra el aviso y no se toca más, así perder y volver a ganar un logro no
-- repite el aviso. retroactivo marca lo que se dio en la carga inicial.
create table logros_de_usuarios (
    user_id      bigint                      not null references users (id) on delete cascade,
    logro        varchar(40)                 not null,
    ganado_el    timestamp(6) with time zone not null,
    activo       boolean                     not null default true,
    cambio_el    timestamp(6) with time zone not null,
    visto_el     timestamp(6) with time zone,
    retroactivo  boolean                     not null default false,
    detalle      varchar(100),
    primary key (user_id, logro)
);

-- Los avisos que faltan mostrar: es lo que mira la campana en cada pantalla.
create index ix_logros_sin_ver on logros_de_usuarios (user_id) where activo and visto_el is null;

-- Para Ansioso y para el podio: las reseñas de un local en orden de llegada.
create index ix_ratings_local_fecha on ratings (burger_joint_id, created_at, id);

-- Ansioso (#314): ser la primera persona en reseñar un local, con comentario. Queda fijo al
-- publicar: si la reseña se borra se pierde el logro, y nadie lo hereda, porque el local
-- ya tuvo su primera reseña.
alter table burger_joints add column tuvo_resenias boolean not null default false;
alter table ratings add column primera_con_comentario boolean not null default false;

-- Para lo anterior al lanzamiento cuenta la reseña más vieja de cada local.
update burger_joints b set tuvo_resenias = true
where exists (select 1 from ratings r where r.burger_joint_id = b.id);

update ratings r set primera_con_comentario = true
where r.comment is not null and btrim(r.comment) <> ''
  and not exists (select 1 from ratings o
                  where o.burger_joint_id = r.burger_joint_id
                    and (o.created_at < r.created_at or (o.created_at = r.created_at and o.id < r.id)));
