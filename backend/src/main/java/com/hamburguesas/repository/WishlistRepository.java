package com.hamburguesas.repository;

import com.hamburguesas.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<WishlistItem> findByUser_IdAndBurgerJoint_Id(Long userId, Long burgerJointId);
    boolean existsByUser_IdAndBurgerJoint_Id(Long userId, Long burgerJointId);

    /** Para no borrar un local que alguien tiene anotado para ir. */
    boolean existsByBurgerJoint_Id(Long burgerJointId);
}
