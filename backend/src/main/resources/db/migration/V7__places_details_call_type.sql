-- Las fotos se bajaban solo para los locales que devolvían las búsquedas por barrio.
-- Los que no aparecen en ninguna —por ejemplo los que Google no clasifica como
-- hamburguesería, como Burger King— quedaban sin foto para siempre. Ahora se les
-- puede pedir la ficha directamente por place_id, y eso es otra llamada con su
-- propio límite gratuito, así que necesita su propio contador.
alter table places_api_usage drop constraint ck_places_usage_call_type;
alter table places_api_usage add constraint ck_places_usage_call_type
    check (call_type in ('SEARCH', 'PHOTO', 'DETAILS'));
