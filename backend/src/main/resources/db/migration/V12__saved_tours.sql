-- Los recorridos que alguien guardó desde la pantalla de tour.
--
-- Se guardan las paradas y no los filtros con que salió el recorrido: lo que importa
-- después es por dónde se pasó, no qué se le pidió al generador. Con las paradas alcanza
-- para mostrarlo en el perfil, para volver a abrirlo en Maps y para lo otro que hacen
-- falta, que es no proponer de nuevo un recorrido que ya se hizo.
create table if not exists tours (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    -- Armado solo con los barrios y la cantidad: nadie quiere ponerle nombre a esto.
    name varchar(160) not null,
    -- Lo estimado cuando se generó, para no tener que recalcularlo al listarlo. Si el
    -- cálculo cambia, lo guardado sigue diciendo lo que decía el día que se guardó.
    kilometers double precision not null,
    minutes integer not null,
    -- A_PIE o EN_AUTO, para volver a abrirlo en Maps como se hizo.
    travel_mode varchar(20) not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_tours_user on tours (user_id, created_at desc);

-- Una fila por parada, con su orden. Es lo que permite comparar dos recorridos: dos
-- tours son el mismo cuando pasan por las mismas hamburgueserías, sin importar en qué
-- orden se hayan caminado.
create table if not exists tour_stops (
    id bigserial primary key,
    tour_id bigint not null references tours(id) on delete cascade,
    burger_joint_id bigint not null references burger_joints(id) on delete cascade,
    position integer not null,
    -- Lo que había que caminar desde la parada anterior, como se calculó ese día.
    kilometers double precision not null,
    constraint uk_tour_stop_position unique (tour_id, position),
    constraint uk_tour_stop_joint unique (tour_id, burger_joint_id)
);

create index if not exists idx_tour_stops_joint on tour_stops (burger_joint_id);
