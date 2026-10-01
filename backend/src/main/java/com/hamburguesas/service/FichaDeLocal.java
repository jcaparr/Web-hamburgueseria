package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Un local tal como lo ve la pantalla: con su nota, cuántos lo puntuaron y si está en
 * la lista de deseos de quien mira.
 *
 * Estaba armado a mano en cuatro servicios —Explorar, la lista de deseos, el recorrido
 * y los recorridos guardados—, campo por campo. Agregar un dato al local pedía tocar
 * los cuatro, y alcanzaba con olvidarse de uno para que la misma hamburguesería se
 * viera distinta según la pantalla.
 */
@Component
@RequiredArgsConstructor
public class FichaDeLocal {

    private final RatingRepository ratingRepository;
    private final WishlistRepository wishlistRepository;

    /** Para quien está mirando: sin sesión, nunca está en la lista de deseos. */
    public BurgerJointDto para(BurgerJoint local, Long userId) {
        boolean enDeseos = userId != null
            && wishlistRepository.existsByUser_IdAndBurgerJoint_Id(userId, local.getId());
        return conDeseo(local, enDeseos);
    }

    /** Cuando ya se sabe si está en la lista, y preguntarlo de nuevo sería una consulta de más. */
    public BurgerJointDto conDeseo(BurgerJoint local, boolean enDeseos) {
        return new BurgerJointDto(
            local.getId(), local.getPlaceId(), local.getName(), local.getAddress(),
            local.getArea(), local.getPhotoUrl(), local.getLatitude(), local.getLongitude(),
            ratingRepository.averageScoreByBurgerJoint(local.getId()),
            ratingRepository.countByBurgerJoint_Id(local.getId()),
            enDeseos);
    }
}
