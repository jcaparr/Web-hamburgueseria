-- Guarda cuál de las fotos de Google es la que tenemos bajada.
--
-- Sin esto no hay forma de saber si la foto guardada sigue siendo la mejor: al
-- cambiar la regla de elección, la única alternativa era volver a bajarlas todas a
-- ciegas, y cada descarga cuesta una llamada. Con el nombre guardado se compara
-- primero y solo se baja la que de verdad cambió.
--
-- Queda en null para las que ya estaban: eso mismo las marca como "hay que
-- revisarlas una vez".
alter table burger_joints add column photo_name varchar(500);
