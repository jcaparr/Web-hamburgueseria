-- Cuándo se fue y tres notas más por reseña (#336).
--
-- visit_date: el día de la visita, que puede ser anterior al día en que se cargó la
-- reseña. Las que ya existen quedan en null y se muestran con la fecha de carga.
--
-- score_local, score_atencion y score_precio: opcionales, del 1 al 5. La nota que
-- cuenta para el promedio, el ranking y las medallas sigue siendo "score": estas son
-- solo para ver el desglose de cada local.
alter table ratings add column visit_date date;
alter table ratings add column score_local smallint;
alter table ratings add column score_atencion smallint;
alter table ratings add column score_precio smallint;

alter table ratings add constraint ck_ratings_score_local check (score_local between 1 and 5);
alter table ratings add constraint ck_ratings_score_atencion check (score_atencion between 1 and 5);
alter table ratings add constraint ck_ratings_score_precio check (score_precio between 1 and 5);
