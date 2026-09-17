package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.User;
import com.hamburguesas.model.WishlistItem;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import com.hamburguesas.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final BurgerJointRepository burgerJointRepository;
    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;

    public List<BurgerJointDto> list(Long userId) {
        return wishlistRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
            .map(item -> toDto(item.getBurgerJoint()))
            .toList();
    }

    @Transactional
    public void add(Long userId, Long burgerJointId) {
        if (wishlistRepository.existsByUser_IdAndBurgerJoint_Id(userId, burgerJointId)) {
            throw new ConflictException("Already in your wishlist");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        BurgerJoint burgerJoint = burgerJointRepository.findById(burgerJointId)
            .orElseThrow(() -> new ResourceNotFoundException("Burger joint not found"));

        wishlistRepository.save(
            WishlistItem.builder().user(user).burgerJoint(burgerJoint).build()
        );
    }

    @Transactional
    public void remove(Long userId, Long burgerJointId) {
        WishlistItem item = wishlistRepository
            .findByUser_IdAndBurgerJoint_Id(userId, burgerJointId)
            .orElseThrow(() -> new ResourceNotFoundException("It wasn't in your wishlist"));
        wishlistRepository.delete(item);
    }

    private BurgerJointDto toDto(BurgerJoint b) {
        Double averageScore = ratingRepository.averageScoreByBurgerJoint(b.getId());
        long ratingsCount = ratingRepository.countByBurgerJoint_Id(b.getId());
        return new BurgerJointDto(
            b.getId(), b.getName(), b.getAddress(), b.getArea(), b.getPhotoUrl(),
            b.getLatitude(), b.getLongitude(), averageScore, ratingsCount, true
        );
    }
}
