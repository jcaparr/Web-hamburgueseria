-- El horario de apertura de cada local, tal como lo tiene Google.
--
-- Va en una tabla aparte y no en una columna de burger_joints porque un local tiene
-- varias franjas por semana, a veces dos el mismo día (mediodía y noche), y porque
-- Explorar lista los locales de a veinte sin necesitar el horario de ninguno.
--
-- Cada franja es un tramo abierto que empieza un día de la semana:
--   dia     0 es domingo y 6 es sábado, como los numera Google
--   abre    minutos desde la medianoche de ese día
--   cierra  minutos desde la misma medianoche, así que puede pasar de 1440: un viernes
--           de 19 a 1 de la mañana es abre 1140 y cierra 1500. Partirlo en dos franjas
--           haría que el sábado figure "abierto de 0 a 1", que no es lo que se lee.
create table franjas_horarias (
    id bigserial primary key,
    burger_joint_id bigint not null references burger_joints(id) on delete cascade,
    dia integer not null check (dia between 0 and 6),
    abre integer not null check (abre between 0 and 1439),
    cierra integer not null,
    constraint ck_franjas_cierra_despues_de_abrir check (cierra > abre)
);

create index ix_franjas_horarias_local on franjas_horarias (burger_joint_id);

-- Cuándo se le preguntó a Google por última vez. Nulo es "nunca", que no es lo mismo
-- que "no tiene horario": esos quedan con la fecha puesta y ninguna franja.
alter table burger_joints add column horario_consultado_el timestamp(6) with time zone;

comment on column burger_joints.horario_consultado_el is
    'Última vez que se le pidió el horario a Google. Nulo: nunca se preguntó.';

-- El horario se paga aparte —es un campo Enterprise, con su propio tramo gratuito—, así
-- que lleva su propio contador. Se busca la restricción por lo que hace, igual que en
-- V7 y V22.
do $$
declare
    restriccion record;
begin
    for restriccion in
        select conname
        from pg_constraint
        where conrelid = 'places_api_usage'::regclass
          and contype = 'c'
          and pg_get_constraintdef(oid) ilike '%call_type%'
    loop
        execute format('alter table places_api_usage drop constraint %I', restriccion.conname);
    end loop;
end $$;

alter table places_api_usage add constraint ck_places_usage_call_type
    check (call_type in ('SEARCH', 'PHOTO', 'DETAILS', 'RESUMEN', 'HORARIO'));
