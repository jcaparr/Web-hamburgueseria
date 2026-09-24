package com.hamburguesas.repository;

import com.hamburguesas.model.SavedTour;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedTourRepository extends JpaRepository<SavedTour, Long> {

    /**
     * Los recorridos de esa persona, del más nuevo al más viejo.
     *
     * Con las paradas y sus locales traídos de una: la pantalla los muestra todos, y sin
     * esto cada tour dispara una consulta por parada.
     */
    @EntityGraph(attributePaths = {"stops", "stops.burgerJoint"})
    List<SavedTour> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /** Por id y dueño a la vez: nadie puede borrar el recorrido de otro. */
    Optional<SavedTour> findByIdAndUser_Id(Long id, Long userId);

    /** Para no proponer de nuevo un recorrido que esa persona ya guardó. */
    @Query("""
        select stop.burgerJoint.id
        from SavedTourStop stop
        where stop.tour.id = :tourId
        """)
    List<Long> idsDeLasParadas(@Param("tourId") Long tourId);

    /** Si este local es parte de algún recorrido guardado, la limpieza no lo borra. */
    @Query("""
        select count(stop) > 0
        from SavedTourStop stop
        where stop.burgerJoint.id = :burgerJointId
        """)
    boolean estaEnAlgunTour(@Param("burgerJointId") Long burgerJointId);

    long countByUser_Id(Long userId);
}
