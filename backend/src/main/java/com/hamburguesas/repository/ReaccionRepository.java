package com.hamburguesas.repository;

import com.hamburguesas.model.Reaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReaccionRepository extends JpaRepository<Reaccion, Long> {

    Optional<Reaccion> findByRating_IdAndUser_Id(Long ratingId, Long userId);

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
