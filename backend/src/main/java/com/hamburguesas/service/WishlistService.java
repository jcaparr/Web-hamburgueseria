package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.User;
import com.hamburguesas.model.WishlistItem;
import com.hamburguesas.repository.BurgerJointRepository;
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
    private final FichaDeLocal fichaDeLocal;

    @Transactional(readOnly = true)
    public List<BurgerJointDto> list(Long userId) {
        List<BurgerJoint> locales = wishlistRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
            .map(WishlistItem::getBurgerJoint)
            .toList();
        return fichaDeLocal.conDeseo(locales, true);
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
}
