-- Dos reacciones cambian de nombre y de dibujo (#365): Fuego pasa a ser Maravilloso
-- (arcoíris) y Aplauso pasa a ser Me encanta (ojos de corazón).
--
-- Se mantienen las reacciones que ya había: quien puso 🔥 queda con Maravilloso y quien
-- puso 👏 queda con Me encanta. El orden importa: primero se suelta la restricción vieja
-- (no deja escribir los nombres nuevos), después se convierten las filas y al final se
-- pone la restricción con los nombres de ahora.
alter table reacciones drop constraint ck_reaccion_tipo;

update reacciones set tipo = 'MARAVILLOSO' where tipo = 'FUEGO';
update reacciones set tipo = 'ENCANTA'     where tipo = 'APLAUSO';

alter table reacciones
    add constraint ck_reaccion_tipo
        check (tipo in ('HAMBRE', 'MARAVILLOSO', 'ENCANTA', 'RISA', 'SORPRESA'));
