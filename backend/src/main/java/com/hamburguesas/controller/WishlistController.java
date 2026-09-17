package com.hamburguesas.controller;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public List<BurgerJointDto> list() {
        return wishlistService.list(CurrentUser.requireId());
    }

    @PostMapping("/{burgerJointId}")
    public ResponseEntity<Void> add(@PathVariable Long burgerJointId) {
        wishlistService.add(CurrentUser.requireId(), burgerJointId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{burgerJointId}")
    public ResponseEntity<Void> remove(@PathVariable Long burgerJointId) {
        wishlistService.remove(CurrentUser.requireId(), burgerJointId);
        return ResponseEntity.noContent().build();
    }
}
