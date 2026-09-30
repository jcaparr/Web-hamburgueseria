-- Cómo reconocer la foto que elegimos, que el nombre no permite.
--
-- Google devuelve el nombre de una foto como "places/ChIJ.../photos/Aa-ngM...", y esa
-- segunda parte cambia en cada pedido: pidiendo la misma ficha dos veces con tres
-- segundos de diferencia, ninguno de los diez nombres coincidió. Es un vale para
-- descargar, no un identificador.
--
-- Sin algo estable, la comparación "¿es la misma foto que ya tengo?" nunca daba
-- verdadero y cada revisión se bajaba las cuatrocientas de nuevo, que es el tramo
-- gratuito de un mes entero. Lo que sí se repite igual es el tamaño y quién la subió.
alter table burger_joints
    add column photo_fingerprint varchar(200);

comment on column burger_joints.photo_fingerprint is
    'Ancho x alto | autor de la foto elegida. Sirve para no volver a bajar la misma.';
