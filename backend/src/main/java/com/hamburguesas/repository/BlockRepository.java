package com.hamburguesas.repository;

import com.hamburguesas.model.Block;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Long> {

    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    /** @return cuántas filas se borraron: 0 si no lo tenía bloqueado. */
    long deleteByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    /** Los que bloqueó, para poder desbloquearlos: es el único lugar donde los ve. */
    @EntityGraph(attributePaths = "blocked")
    List<Block> findByBlocker_IdOrderByCreatedAtDesc(Long blockerId);

    /**
     * Con quiénes no se puede cruzar, sin importar quién bloqueó a quién.
     *
     * Una sola consulta que devuelve al otro de cada par: si la fila la puso él, el
     * bloqueado; si se la pusieron, el que lo bloqueó. Es lo que el feed y la búsqueda
     * necesitan para dejarlos afuera, y preguntarlo en dos consultas separadas sería
     * pedir dos veces lo mismo.
     */
    @Query("""
        select case when b.blocker.id = :usuario then b.blocked.id else b.blocker.id end
        from Block b
        where b.blocker.id = :usuario or b.blocked.id = :usuario
        """)
    List<Long> idsQueNoPuedeVer(@Param("usuario") Long usuarioId);

    /** Si hay un bloqueo entre los dos, lo haya puesto cualquiera. */
    @Query("""
        select count(b) > 0 from Block b
        where (b.blocker.id = :uno and b.blocked.id = :otro)
           or (b.blocker.id = :otro and b.blocked.id = :uno)
        """)
    boolean hayBloqueoEntre(@Param("uno") Long uno, @Param("otro") Long otro);
}
