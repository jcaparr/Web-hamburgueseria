-- Los fondos del avatar pasan de cinco a diez (#370).
--
-- La primera cifra del código de la hamburguesa llega ahora hasta 9. Los códigos que ya
-- existen siguen siendo válidos, así que no hay que tocar ninguna fila. Si el código vuelve
-- atrás pero la base no, el código viejo rechaza los fondos nuevos y dibuja la hamburguesa
-- del nombre.
alter table users drop constraint users_hamburguesa_valida;
alter table users add constraint users_hamburguesa_valida
    check (hamburguesa ~ '^[0-9][0-2][0-2][0-3][1-4]([01]{3})?$');

comment on column users.hamburguesa is
    'La hamburguesa del avatar: fondo (0 a 9), pan, queso, verdura y carnes (1 a 4), y opcionalmente huevo, bacon y rayos. Nulo: la que sale del nombre.';
