-- El barrio preparado para buscar: sin acentos y en minúsculas (#335).
--
-- Igual que nombre_para_buscar (V21): permite que "mostaza belgrano" encuentre la
-- sucursal de Belgrano sin depender de la extensión unaccent de Postgres.
alter table burger_joints
    add column area_para_buscar varchar(255);

update burger_joints
set area_para_buscar = lower(btrim(regexp_replace(translate(
    area,
    'áéíóúüñÁÉÍÓÚÜÑàèìòùÀÈÌÒÙâêîôûÂÊÎÔÛäëïöÄËÏÖçÇ',
    'aeiouunAEIOUUNaeiouAEIOUaeiouAEIOUaeioAEIOcC'), '\s+', ' ', 'g')))
where area is not null;

comment on column burger_joints.area_para_buscar is
    'El barrio sin acentos y en minúsculas. Lo mantiene la aplicación al guardar.';
