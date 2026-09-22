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
    Page<BurgerJoint> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Optional<BurgerJoint> findByPlaceId(String placeId);
}
