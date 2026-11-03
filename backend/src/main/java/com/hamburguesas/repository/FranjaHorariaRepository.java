package com.hamburguesas.repository;

import com.hamburguesas.model.FranjaHoraria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FranjaHorariaRepository extends JpaRepository<FranjaHoraria, Long> {

    List<FranjaHoraria> findByBurgerJoint_IdOrderByDiaAscAbreAsc(Long burgerJointId);

    /**
     * Las franjas de todos los locales del mapa (#395), de una: el mapa muestra el horario
     * de hoy de cada uno y pedirlo de a uno serían mil consultas. Mismo criterio de locales
     * que BurgerJointRepository.paraElMapa.
     */
    @Query("""
        select new com.hamburguesas.dto.FranjaDeLocal(f.burgerJoint.id, f.dia, f.abre, f.cierra)
        from FranjaHoraria f
        where f.burgerJoint.latitude is not null and f.burgerJoint.longitude is not null
          and f.burgerJoint.fastFood = false
        order by f.burgerJoint.id, f.dia, f.abre
        """)
    List<com.hamburguesas.dto.FranjaDeLocal> paraElMapa();

    /** Para reemplazar el horario entero: Google lo devuelve completo cada vez. */
    @Modifying
    @Transactional
    @Query("delete from FranjaHoraria f where f.burgerJoint.id = :burgerJointId")
    void borrarLasDe(@Param("burgerJointId") Long burgerJointId);
}
