-- La dirección linda de cada local: burgometro.com.ar/<slug> en vez de /burger-joints/<id> (#305).
--
-- Queda nulo hasta que la app se lo arma al arrancar (SlugsDeLocales): armarlo acá pediría
-- sacar acentos en SQL, y eso necesita la extensión unaccent, que no está garantizada.
-- El índice único deja varios nulos, así que no estorba mientras tanto.
alter table burger_joints add column slug varchar(160);
create unique index uq_burger_joints_slug on burger_joints (slug);
