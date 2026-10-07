-- La hamburguesa que cada uno eligió para su avatar (#151).
--
-- Son cinco cifras, una por cosa que se elige y en este orden: el fondo (0 a 4), el pan
-- (0 a 2), el queso (0 a 2), lo verde (0 a 3) y cuántas carnes (1 a 3). Cinco cifras y
-- no una columna por cosa porque la base no hace nada con ellas: las guarda y las
-- devuelve con el nombre, y el dibujo lo arma el navegador.
--
-- Nulo es "no eligió ninguna": se ve la que sale de su nombre de usuario.
alter table users add column hamburguesa varchar(5);

alter table users add constraint users_hamburguesa_valida
    check (hamburguesa ~ '^[0-4][0-2][0-2][0-3][1-3]$');

comment on column users.hamburguesa is
    'La hamburguesa del avatar: fondo, pan, queso, verdura y carnes, una cifra cada uno. Nulo: la que sale del nombre.';
