-- Cuántas fotos tiene el local en Google, que hasta ahora se preguntaba y se tiraba.
--
-- La respuesta de Google trae el array entero —hasta diez, que es su tope— y nosotros nos
-- quedábamos con las tres mejores y perdíamos el resto. De un local solo sabíamos si
-- tenía alguna o ninguna, y "ninguna" y "tres" no son lo mismo: diez es un local que la
-- gente fotografía, tres es uno por el que nadie pasó.
--
-- Nulo quiere decir "todavía no se preguntó", que no es cero. La diferencia importa: cero
-- es motivo para borrarlo y nulo no es motivo de nada.
alter table burger_joints add column fotos_en_google integer;

comment on column burger_joints.fotos_en_google is
    'Cuántas fotos devolvió Google la última vez que se preguntó. Nulo: nunca se preguntó.';
