package com.hamburguesas.repository;

import com.hamburguesas.model.BurgerJoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BurgerJointRepository extends JpaRepository<BurgerJoint, Long> {

    /** Los que todavía no tienen foto, para completarlas en la próxima sincronización. */
    List<BurgerJoint> findByPhotoUrlIsNull();

    /** Los que ya tienen foto, para prestársela a otras sucursales de la misma cadena. */
    List<BurgerJoint> findByPhotoUrlIsNotNull();

    /**
     * Los que tienen foto pero de los que no sabemos cuál es: son los que se bajaron
     * antes de empezar a guardar el nombre, con la regla de elección vieja. Se revisan
     * una vez y después quedan con su nombre anotado.
     */
    List<BurgerJoint> findByPhotoUrlIsNotNullAndPhotoNameIsNull();
    Page<BurgerJoint> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<BurgerJoint> findByPlaceId(String placeId);
}
