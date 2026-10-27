-- El logro que cada uno elige mostrar en su perfil (#319): el código del logro, o nulo.
-- Si el logro se pierde, el recálculo lo limpia.
alter table users add column logro_exhibido varchar(40);
