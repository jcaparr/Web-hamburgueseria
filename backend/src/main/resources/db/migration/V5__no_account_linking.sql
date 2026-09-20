-- Cada cuenta pertenece a un solo método de ingreso: o email y contraseña, o Google.
-- La V4 había dejado lugar para vincular ambos; se decidió no hacerlo, porque un
-- modelo donde las cuentas convergen es más difícil de razonar (y de auditar) que uno
-- donde cada cuenta tiene un solo camino de entrada.
--
-- Va como migración nueva y no editando la V4 porque esa ya corrió sobre una base con
-- datos: reescribir una migración aplicada es cómo se rompen los entornos.

delete from verification_codes where purpose = 'GOOGLE_LINK';

alter table verification_codes drop constraint ck_verification_codes_purpose;
alter table verification_codes add constraint ck_verification_codes_purpose
    check (purpose in ('EMAIL_VERIFICATION', 'PASSWORD_RESET'));

-- Las cuentas que alcanzaron a vincularse tienen las dos credenciales y no entrarían
-- en la regla nueva. Se les deja la contraseña y se les quita Google: la cuenta se
-- creó con contraseña, Google llegó después, y quitar lo agregado es menos sorpresa
-- que quitar lo original. Quien quiera entrar con Google se crea una cuenta nueva.
update users set google_sub = null
    where password_hash is not null and google_sub is not null;

-- Ahora que no hay vinculación, una cuenta tiene exactamente una credencial, nunca dos.
alter table users drop constraint ck_users_has_credential;
alter table users add constraint ck_users_has_credential
    check ((password_hash is not null) <> (google_sub is not null));
