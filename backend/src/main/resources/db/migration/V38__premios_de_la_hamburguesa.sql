-- Los premios de la hamburguesa que se desbloquean con el nivel (#313).
--
-- El código pasa de cinco cifras a cinco u ocho: la cifra de las carnes llega hasta 4 (la
-- cuarta carne, nivel 10) y, si se usa algún premio, tres cifras más al final que valen 0 o 1:
-- huevo frito (nivel 3), bacon (5) y fondo de rayos (15).
--
-- Los códigos de cinco cifras que ya existen siguen siendo válidos, así que no hay que
-- tocar ninguna fila. Y si el código vuelve atrás pero la base no, el código viejo
-- sigue leyendo los de cinco cifras; los de ocho los rechaza y dibuja la del nombre.
alter table users drop constraint users_hamburguesa_valida;
alter table users alter column hamburguesa type varchar(8);
alter table users add constraint users_hamburguesa_valida
    check (hamburguesa ~ '^[0-4][0-2][0-2][0-3][1-4]([01]{3})?$');

comment on column users.hamburguesa is
    'La hamburguesa del avatar: fondo, pan, queso, verdura y carnes (1 a 4), y opcionalmente huevo, bacon y rayos. Nulo: la que sale del nombre.';
