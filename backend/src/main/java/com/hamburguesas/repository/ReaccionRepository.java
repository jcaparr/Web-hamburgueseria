package com.hamburguesas.repository;

import com.hamburguesas.model.Reaccion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReaccionRepository extends JpaRepository<Reaccion, Long> {

    Optional<Reaccion> findByRating_IdAndUser_Id(Long ratingId, Long userId);

    /**
     * Las reacciones a las reseñas de esta persona, de lo más nuevo a lo más viejo, para
     * su buzón de notificaciones (#210). Sin las suyas —uno no reacciona a lo propio,
     * pero la base no lo impide— ni las de gente con la que hay un bloqueo.
     */
    @Query("""
        select r from Reaccion r join fetch r.user join fetch r.rating resenia join fetch resenia.burgerJoint
        where resenia.user.id = :yo and r.user.id <> :yo and r.user.id not in :ocultos
        order by r.createdAt desc, r.id desc
        """)
    List<Reaccion> aLasReseniasDe(@Param("yo") Long yo, @Param("ocultos") Collection<Long> ocultos,
                                  Pageable pagina);

    /** Cuántas de esas llegaron después de una fecha: lo que cuenta la campana. */
    @Query("""
        select count(r) from Reaccion r
        where r.rating.user.id = :yo and r.user.id <> :yo and r.user.id not in :ocultos
          and r.createdAt > :desde
        """)
    long nuevasALasReseniasDe(@Param("yo") Long yo, @Param("ocultos") Collection<Long> ocultos,
                              @Param("desde") Instant desde);

    void deleteByRating_IdAndUser_Id(Long ratingId, Long userId);

    /**
     * Cuántas hay de cada tipo en cada una de estas reseñas, en una sola consulta.
     *
     * Cada fila es el id de la reseña, el tipo y la cantidad. Las reseñas sin ninguna
     * no aparecen: quien llama las completa.
     */
    @Query("""
        select r.rating.id, r.tipo, count(r) from Reaccion r
        where r.rating.id in :ids
        group by r.rating.id, r.tipo
        """)
    List<Object[]> contarEn(@Param("ids") Collection<Long> ids);

    /** Las que puso una persona en estas reseñas: el id de la reseña y el tipo. */
    @Query("""
        select r.rating.id, r.tipo from Reaccion r
        where r.user.id = :userId and r.rating.id in :ids
        """)
    List<Object[]> deUnoEn(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}
