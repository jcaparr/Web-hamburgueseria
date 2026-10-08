-- Hasta cuatro fotos por reseña (#185).
--
-- V18 decía que con una alcanzaba. Los testers pidieron más: la hamburguesa por fuera
-- y por dentro, las papas, el lugar. Cuatro alcanza para eso sin que una reseña se
-- vuelva un álbum, y deja acotado lo que crecen el disco y la copia de afuera.
--
-- Van en su propia tabla, en orden. La primera es la portada.
create table rating_photos (
    rating_id bigint       not null references ratings (id) on delete cascade,
    orden     integer      not null,
    url       varchar(500) not null,
    primary key (rating_id, orden)
);

-- Las reseñas que ya tenían foto la pasan a ser su primera.
insert into rating_photos (rating_id, orden, url)
select id, 0, photo_url
from ratings
where photo_url is not null;

-- photo_url queda, siempre igual a la primera de rating_photos. Si un deploy sale mal y
-- se vuelve al código anterior, ese código sigue encontrando la foto donde la buscaba:
-- volver atrás el código no vuelve atrás la base.
comment on column ratings.photo_url is
    'La portada: la primera de rating_photos. Se mantiene para que el código anterior siga andando.';
