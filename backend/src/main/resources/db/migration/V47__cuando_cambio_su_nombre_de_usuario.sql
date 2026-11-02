-- Cuándo cambió por última vez su nombre de usuario (#380).
--
-- El nombre se puede cambiar una vez cada 30 días. Nulo = nunca lo cambió, y entonces
-- puede hacerlo ya: a las cuentas que existen (con el nombre sacado del email) no se
-- les cierra la puerta. Registrarse o elegir el nombre al entrar con Google no cuenta
-- como un cambio.
alter table users add column username_changed_at timestamp(6) with time zone;
