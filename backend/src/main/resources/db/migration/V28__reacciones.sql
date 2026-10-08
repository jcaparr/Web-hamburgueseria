-- Reaccionar a la reseña de otro (#186).
--
-- Una reacción por persona y por reseña, como en los mensajes de WhatsApp: elegir otra
-- cambia la que había, no suma una segunda. Con varias por persona, cinco emojis de la
-- misma persona se leerían como cinco personas.
--
-- El tipo se guarda por nombre y no como el emoji: el dibujo es cosa de la pantalla, y
-- cambiarlo algún día no tiene que tocar la base.
create table reacciones (
    id         bigserial   primary key,
    rating_id  bigint      not null references ratings (id) on delete cascade,
    user_id    bigint      not null references users (id) on delete cascade,
    tipo       varchar(20) not null,
    created_at timestamptz not null default now(),
    constraint uk_reaccion unique (rating_id, user_id),
    constraint ck_reaccion_tipo check (tipo in ('HAMBRE', 'FUEGO', 'APLAUSO', 'RISA', 'SORPRESA'))
);

-- Para contarlas de una página de reseñas de una vez. El unique de arriba ya empieza
-- por rating_id, así que sirve para eso; este es para buscar las de una persona.
create index idx_reacciones_usuario on reacciones (user_id);
