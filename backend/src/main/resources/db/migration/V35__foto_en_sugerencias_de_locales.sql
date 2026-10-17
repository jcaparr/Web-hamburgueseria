-- La foto opcional que acompaña una sugerencia de local (#283).
--
-- Guarda solo el nombre del archivo: la foto vive en una carpeta aparte que la web no
-- sirve, así que quien sugiere no puede publicarla. Juan la mira y, si la aprueba, la
-- sube al local al agregarlo (skill agregar-local).
alter table sugerencias_de_locales add column foto_archivo varchar(100);
