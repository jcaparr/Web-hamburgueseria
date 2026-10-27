-- Tareas que corren una sola vez al arrancar, y cuándo se hicieron (#344).
--
-- La primera es el recálculo de la XP de todas las cuentas con la regla de 5 palabras.
-- La fila se escribe al terminar: si la tarea se corta a la mitad, el próximo arranque la
-- retoma (es idempotente).
create table tareas_unicas (
    nombre   varchar(80) primary key,
    hecha_el timestamp(6) with time zone not null
);
