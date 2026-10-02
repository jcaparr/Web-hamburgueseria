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

    /** Para reemplazar el horario entero: Google lo devuelve completo cada vez. */
    @Modifying
    @Transactional
    @Query("delete from FranjaHoraria f where f.burgerJoint.id = :burgerJointId")
    void borrarLasDe(@Param("burgerJointId") Long burgerJointId);
}
