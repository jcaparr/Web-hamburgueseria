-- Hasta qué resumen de "Tu mes en Burgómetro" ya vio la persona (#405).
--
-- Guarda el último mes cuyo pop up ya se mostró ("2026-10"), así no se repite en otro
-- dispositivo ni al volver a entrar. Nulo = todavía no vio ninguno. El resumen de diciembre
-- (el recuento anual) se anota como "2026-12".
alter table users add column resumen_visto varchar(7);
