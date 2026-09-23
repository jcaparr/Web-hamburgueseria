package com.hamburguesas.repository;

import com.hamburguesas.model.BurgerJoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BurgerJointRepository extends JpaRepository<BurgerJoint, Long> {

    /** Los que todavía no tienen foto, para completarlas en la próxima sincronización. */
    List<BurgerJoint> findByPhotoUrlIsNull();

    /** Los que ya tienen foto, para prestársela a otras sucursales de la misma cadena. */
    List<BurgerJoint> findByPhotoUrlIsNotNull();

    /**
     * Los que tienen una foto elegida con una regla anterior a la de ahora. Se revisan
     * una vez, quedan anotados con la regla nueva y no se vuelven a mirar.
     */
    @Query("""
        select b from BurgerJoint b
        where b.photoUrl is not null
          and (b.photoRule is null or b.photoRule < :regla)
        """)
    List<BurgerJoint> conFotoElegidaConUnaReglaVieja(@Param("regla") int regla);
    Page<BurgerJoint> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<BurgerJoint> findByPlaceId(String placeId);
}
