-- Con qué versión de la regla de elección se eligió la foto que tenemos guardada.
-- Cuando la regla mejora, este número dice cuáles hay que volver a mirar: sin él, una
-- mejora solo alcanzaría a los locales nuevos y los que ya tienen foto se quedarían
-- con la elección vieja para siempre.
alter table burger_joints add column if not exists photo_rule integer;

-- Las que ya están se eligieron con reglas anteriores. Las que tienen anotado cuál es
-- su foto pasaron por la regla 2 (prefiere las del local y las apaisadas); las demás
-- vienen de la regla 1, que se quedaba con la primera.
update burger_joints set photo_rule = 2 where photo_url is not null and photo_name is not null;
update burger_joints set photo_rule = 1 where photo_url is not null and photo_name is null;
