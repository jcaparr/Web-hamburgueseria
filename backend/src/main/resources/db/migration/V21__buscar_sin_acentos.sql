-- El nombre preparado para buscar: sin acentos y en minúsculas.
--
-- Buscar "Atiko" no encontraba "Átiko - Agronomia", ni "Nunez" a los de Núñez. Nadie
-- escribe los acentos en un buscador, y la mitad de los barrios de la Ciudad los tienen.
--
-- Va en una columna y no resuelto en la consulta porque Postgres necesitaría la
-- extensión unaccent, que no está garantizada en todos los hostings y haría que la app
-- dependa de haberla instalado a mano en el servidor.
alter table burger_joints
    add column nombre_para_buscar varchar(255);

-- El relleno inicial. De acá en adelante lo mantiene la aplicación al guardar, que es
-- donde vive la misma regla para los locales nuevos.
update burger_joints
set nombre_para_buscar = lower(translate(
    name,
    'áéíóúüñÁÉÍÓÚÜÑàèìòùÀÈÌÒÙâêîôûÂÊÎÔÛäëïöÄËÏÖçÇ',
    'aeiouunAEIOUUNaeiouAEIOUaeiouAEIOUaeioAEIOcC'));

create index idx_burger_joints_nombre_para_buscar on burger_joints (nombre_para_buscar);

comment on column burger_joints.nombre_para_buscar is
    'El nombre sin acentos y en minúsculas. Lo mantiene la aplicación al guardar.';
