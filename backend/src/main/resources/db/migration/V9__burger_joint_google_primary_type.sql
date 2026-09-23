-- El rubro principal que Google le pone al local. Se guarda para poder revisar qué
-- entró a la base y por qué: la búsqueda pide hamburgueserías, pero Google le cuelga
-- ese tipo a negocios que no dan de comer, y sin este dato había que volver a
-- preguntarle a Google local por local para darse cuenta.
alter table burger_joints add column if not exists google_primary_type varchar(80);
