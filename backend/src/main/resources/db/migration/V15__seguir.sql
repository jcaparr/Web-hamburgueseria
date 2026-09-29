-- Quién sigue a quién.
--
-- Es una sola dirección y no una amistad: seguir a alguien no necesita su permiso,
-- igual que interesarse por dónde come no lo necesita. La amistad mutua obligaría a
-- una solicitud, a aceptarla o rechazarla, y a avisarle a alguien que llegó — tres
-- cosas que no hacen falta para lo único que se quiere, que es ver sus reseñas.
create table if not exists follows (
    id bigserial primary key,
    follower_id bigint not null references users(id) on delete cascade,
    followed_id bigint not null references users(id) on delete cascade,
    created_at timestamptz not null default now(),
    -- Seguir dos veces es seguir una vez.
    constraint uk_follow unique (follower_id, followed_id),
    -- Nadie se sigue a sí mismo. El servicio ya lo impide, pero si alguna vez se
    -- escapa por otro camino conviene que la base lo frene antes de guardarlo.
    constraint ck_follow_no_self check (follower_id <> followed_id)
);

-- Para contar seguidores. El unique de arriba ya sirve para la otra dirección.
create index if not exists idx_follows_followed on follows (followed_id);

-- Buscar gente es preguntar por un pedazo de nombre. El índice único de username no
-- sirve para eso: ordena por la colación del idioma, no por bytes, y entonces un
-- "empieza con" no lo puede usar. Este sí.
create index if not exists idx_users_username_prefijo on users (username text_pattern_ops);
