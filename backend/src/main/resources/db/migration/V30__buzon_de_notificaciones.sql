-- Cuándo abrió cada uno el buzón de notificaciones por última vez (#210).
--
-- El buzón no tiene tabla propia: se arma con los seguimientos y las reacciones que ya
-- están guardados, cada uno con su fecha. Lo único que falta saber es desde cuándo son
-- nuevos, y eso es esta columna. Nulo: nunca lo abrió, y todo cuenta como nuevo.
--
-- Las cuentas que ya existen quedan en nulo a propósito: los seguimientos y las
-- reacciones de antes del buzón nunca se avisaron, y la primera vez que cada uno lo abra
-- le llegan como nuevos. Al lanzarlo eran 20 en total, y la cuenta con más tenía 8.
alter table users add column notificaciones_vistas_el timestamp(6) with time zone;

comment on column users.notificaciones_vistas_el is
    'Última vez que abrió el buzón de notificaciones. Nulo: nunca lo abrió.';
