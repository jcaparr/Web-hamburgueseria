-- Preguntar qué fotos tiene un local pasa a contarse aparte (#199).
--
-- Antes era DETAILS: la ficha con el nombre, que Google cobra como Pro. Ahora se pide
-- sin el nombre, que la deja en un tramo gratuito, y se cuenta como LISTA_DE_FOTOS. Los
-- meses anteriores quedan como DETAILS, así que la restricción los sigue aceptando.
-- Se busca la restricción por lo que hace, igual que en V7, V22 y V24.
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
    check (call_type in ('SEARCH', 'PHOTO', 'DETAILS', 'RESUMEN', 'HORARIO', 'LISTA_DE_FOTOS'));
