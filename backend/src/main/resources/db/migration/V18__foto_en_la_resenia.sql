-- La foto que alguien le saca a lo que comió.
--
-- Una por reseña y no varias: una reseña es una opinión sobre una hamburguesa, y con
-- una foto ya se ve de qué está hablando. Varias vuelven la pantalla una galería y
-- traen de arrastre un orden, una principal y un borrar de a una.
--
-- Guarda la ruta pública, igual que burger_joints.photo_url, y no los bytes: la base
-- no es lugar para archivos, y servirlos desde disco deja ponerles caché.
alter table ratings add column photo_url varchar(500);
