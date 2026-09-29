-- El nombre con que una persona se busca y se muestra en la app.
--
-- Hasta ahora una cuenta se identificaba por el email, que no se puede mostrar, o por
-- el nombre, que se repite: con dos "Juan" no hay forma de decir a cuál seguir. Este es
-- el único dato público y único a la vez, y por eso es el que se busca.
--
-- Se guarda siempre en minúsculas y solo con letras, números y guión bajo. Podría
-- aceptarse cualquier cosa y comparar sin distinguir mayúsculas, pero entonces "Juan" y
-- "juan" serían dos personas distintas para el ojo y la misma para la base, que es
-- exactamente como se hacen las suplantaciones.
alter table users add column username varchar(20);

-- A las cuentas que ya existen hay que inventarles uno, porque la columna pasa a ser
-- obligatoria. Sale de la parte del email anterior a la arroba, que es lo más parecido
-- a un nombre elegido que tenemos; el que quiera otro lo va a poder cambiar.
--
-- El bucle busca el primer número libre en vez de calcular el sufijo de una: con pocas
-- cuentas cuesta nada, y a cambio no hay forma de que dos terminen con el mismo nombre,
-- ni siquiera si un email ya venía con números al final.
do $$
declare
    cuenta record;
    base text;
    candidato text;
    n int;
begin
    for cuenta in select id, email from users order by id loop
        base := regexp_replace(lower(split_part(cuenta.email, '@', 1)), '[^a-z0-9_]', '', 'g');
        -- Cinco caracteres de margen para el sufijo, dentro de los 20 que entran.
        base := left(base, 15);
        if length(base) < 3 then
            base := 'usuario';
        end if;

        candidato := base;
        n := 1;
        while exists (select 1 from users where username = candidato) loop
            n := n + 1;
            candidato := base || n;
        end loop;

        update users set username = candidato where id = cuenta.id;
    end loop;
end $$;

alter table users alter column username set not null;
alter table users add constraint uk_users_username unique (username);
