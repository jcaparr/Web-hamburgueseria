-- La portada de Ácido se cambió el 10 de octubre por la de la cocina abierta (#226), antes
-- de que las direcciones de las fotos llevaran una marca de versión (#229). Quedó con la
-- dirección de siempre, y quien ya había visto la foto anterior la sigue viendo: el
-- navegador la guarda 30 días.
--
-- Una marca cualquiera alcanza para que sea una dirección nueva. Las portadas que se
-- cambien de ahora en más la traen sola.
update burger_joints
set photo_url = photo_url || '?v=cocina'
where place_id = 'ChIJRcGmcSm1vJURQaSeqM3qx9A'
  and photo_url is not null
  and photo_url not like '%?%';
