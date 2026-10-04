package com.hamburguesas.repository;

import com.hamburguesas.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<WishlistItem> findByUser_IdAndBurgerJoint_Id(Long userId, Long burgerJointId);
    boolean existsByUser_IdAndBurgerJoint_Id(Long userId, Long burgerJointId);

    /** De estos locales, cuáles tiene guardados esta persona: una consulta para toda la página. */
    @Query("""
        select w.burgerJoint.id
        from WishlistItem w
        where w.user.id = :userId and w.burgerJoint.id in :ids
        """)
    List<Long> guardadosEntre(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    /** Para no borrar un local que alguien tiene anotado para ir. */
    boolean existsByBurgerJoint_Id(Long burgerJointId);
}
