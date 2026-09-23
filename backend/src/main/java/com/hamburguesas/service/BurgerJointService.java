package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BurgerJointService {

    private final BurgerJointRepository burgerJointRepository;
    private final RatingRepository ratingRepository;
    private final WishlistRepository wishlistRepository;

    public Page<BurgerJointDto> search(String query, boolean conCadenas, Long userId, Pageable pageable) {
        boolean buscaPorNombre = query != null && !query.isBlank();

        Page<BurgerJoint> page;
        if (conCadenas) {
            page = buscaPorNombre
                ? burgerJointRepository.findByNameContainingIgnoreCase(query, pageable)
                : burgerJointRepository.findAll(pageable);
        } else {
            page = buscaPorNombre
                ? burgerJointRepository.findByNameContainingIgnoreCaseAndFastFoodFalse(query, pageable)
                : burgerJointRepository.findByFastFoodFalse(pageable);
        }

        return page.map(b -> toDto(b, userId));
    }

    public BurgerJointDto get(Long id, Long userId) {
        BurgerJoint burgerJoint = burgerJointRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Burger joint not found"));
        return toDto(burgerJoint, userId);
    }

    private BurgerJointDto toDto(BurgerJoint b, Long userId) {
        Double averageScore = ratingRepository.averageScoreByBurgerJoint(b.getId());
        long ratingsCount = ratingRepository.countByBurgerJoint_Id(b.getId());
        boolean inWishlist = userId != null &&
            wishlistRepository.existsByUser_IdAndBurgerJoint_Id(userId, b.getId());

        return new BurgerJointDto(
            b.getId(), b.getPlaceId(), b.getName(), b.getAddress(), b.getArea(), b.getPhotoUrl(),
            b.getLatitude(), b.getLongitude(),
            averageScore, ratingsCount, inWishlist
        );
    }
}
