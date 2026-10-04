-- Con qué prueba entró cada local como hamburguesería: el rubro de Google, el nombre,
-- una sucursal de una cadena o lo que dice su resumen de reseñas.
--
-- Hace falta porque un local guardado tiene menos datos que cuando lo devolvió la
-- búsqueda —el rubro principal y no la lista entera, y no el resumen— y la limpieza lo
-- volvía a evaluar como si fuera nuevo. Los que entraron por un rubro secundario o por
-- su resumen caían siempre en "sin pruebas", y cada limpieza le volvía a pedir a Google
-- el resumen, que es el tramo más caro de la API (#97).
--
-- Nulo es "todavía no se anotó": los que ya estaban la anotan la primera vez que la
-- limpieza los mira.
alter table burger_joints add column prueba_de_hamburguesas varchar(40);

comment on column burger_joints.prueba_de_hamburguesas is
    'Con qué prueba entró como hamburguesería (un Veredicto.Prueba). Nulo: todavía no se anotó.';
