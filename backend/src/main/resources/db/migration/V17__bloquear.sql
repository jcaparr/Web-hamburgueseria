-- Quién no quiere saber nada con quién.
--
-- Hasta acá, alguien molesto no tenía freno: podía seguirte, y sus reseñas te
-- aparecían en el feed igual. La única salida era irse de la app, que es una salida
-- que no sirve.
--
-- Se guarda en una sola dirección —quién bloqueó a quién— pero se aplica en las dos:
-- después de un bloqueo ninguno de los dos ve al otro. Que el bloqueado siguiera
-- viendo a quien lo bloqueó dejaría justo la parte que importa sin resolver, porque lo
-- que se quiere evitar es que le siga prestando atención.
create table if not exists blocks (
    id bigserial primary key,
    blocker_id bigint not null references users(id) on delete cascade,
    blocked_id bigint not null references users(id) on delete cascade,
    created_at timestamptz not null default now(),
    constraint uk_block unique (blocker_id, blocked_id),
    constraint ck_block_no_self check (blocker_id <> blocked_id)
);

-- Para resolver "a quién no puedo ver" en una consulta, que es lo que el feed y la
-- búsqueda preguntan en cada pantalla. El unique de arriba cubre la otra dirección.
create index if not exists idx_blocks_blocked on blocks (blocked_id);
