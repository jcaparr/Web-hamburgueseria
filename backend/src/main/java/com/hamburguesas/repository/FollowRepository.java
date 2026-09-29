package com.hamburguesas.repository;

import com.hamburguesas.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollower_IdAndFollowed_Id(Long followerId, Long followedId);

    /** @return cuántas filas se borraron: 0 si no lo seguía. */
    long deleteByFollower_IdAndFollowed_Id(Long followerId, Long followedId);

    /** Cuántos lo siguen. */
    long countByFollowed_Id(Long userId);

    /** A cuántos sigue. */
    long countByFollower_Id(Long userId);

    /**
     * De esta lista, a cuáles sigue.
     *
     * Es una sola consulta para toda una pantalla de resultados. Preguntando de a uno
     * se llevaría una consulta por fila, que es la forma más fácil de que una búsqueda
     * de veinte personas cueste veintiuna idas a la base.
     */
    @Query("select f.followed.id from Follow f where f.follower.id = :seguidor "
        + "and f.followed.id in :candidatos")
    List<Long> idsQueSigueDeEntre(@Param("seguidor") Long seguidorId,
                                  @Param("candidatos") Collection<Long> candidatos);

    /** A quiénes sigue, para armarle el feed. */
    @Query("select f.followed.id from Follow f where f.follower.id = :seguidor")
    List<Long> idsQueSigue(@Param("seguidor") Long seguidorId);
}
