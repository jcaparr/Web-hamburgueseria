-- El resumen de reseñas es otra llamada a Google, con su propio tramo gratuito, así
-- que lleva su propio contador. El tipo se agregó al código y no acá, y la base lo
-- rechaza: la restricción solo admitía los tres anteriores.
--
-- No es un detalle de arranque. Falla la primera vez que se anota un resumen, o sea
-- en la mitad de la sincronización, y la excepción se lleva puesto lo que faltaba:
-- los locales nuevos no se buscan y las fotos no se completan. Lo ya borrado queda
-- borrado, porque eso se confirma a medida que pasa.
--
-- Los tests no lo vieron porque corren sobre H2, donde el esquema lo arma Hibernate
-- desde las entidades y esta restricción no existe. Solo está acá, en la migración,
-- que es lo que corre contra Postgres.
--
-- Se busca por lo que hace y no por cómo se llama, igual que en V7: las bases que
-- vienen de la época de ddl-auto=update tienen la restricción con el nombre que le
-- puso Hibernate, y borrar solo la nuestra dejaría viva la otra.
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
    check (call_type in ('SEARCH', 'PHOTO', 'DETAILS', 'RESUMEN'));
