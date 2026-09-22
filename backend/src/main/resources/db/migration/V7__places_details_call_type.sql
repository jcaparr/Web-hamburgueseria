-- Las fotos se bajaban solo para los locales que devolvían las búsquedas por barrio.
-- Los que no aparecen en ninguna —por ejemplo los que Google no clasifica como
-- hamburguesería, como Burger King— quedaban sin foto para siempre. Ahora se les
-- puede pedir la ficha directamente por place_id, y eso es otra llamada con su
-- propio límite gratuito, así que necesita su propio contador.
--
-- La restricción vieja se busca por lo que hace y no por cómo se llama. Las bases
-- que venían de la época de ddl-auto=update quedaron marcadas desde V1 sin
-- ejecutarlo: sus tablas las creó Hibernate, que le puso otro nombre
-- (places_api_usage_call_type_check). Borrar solo el nombre de V1 dejaba viva la
-- otra, que rechaza DETAILS, y encima fallaba justo en las bases más viejas, que son
-- las que ya están en producción.
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
    check (call_type in ('SEARCH', 'PHOTO', 'DETAILS'));
