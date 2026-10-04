package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.dto.NotaDeLocalDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Un local tal como lo ve la pantalla: con su nota, cuántos lo puntuaron y si está en
 * la lista de deseos de quien mira.
 *
 * Estaba armado a mano en cuatro servicios —Explorar, la lista de deseos, el recorrido
 * y los recorridos guardados—, campo por campo. Agregar un dato al local pedía tocar
 * los cuatro, y alcanzaba con olvidarse de uno para que la misma hamburguesería se
 * viera distinta según la pantalla.
 *
 * Arma varios de una vez con dos consultas en total —las notas de todos agrupadas, y
 * cuáles están en la lista de deseos— y no dos o tres por local: una página de Explorar
 * son veinte locales, y eran entre cuarenta y sesenta consultas además de la de la
 * página (#101).
 */
@Component
@RequiredArgsConstructor
public class FichaDeLocal {

    private final RatingRepository ratingRepository;
    private final WishlistRepository wishlistRepository;

    /** Para quien está mirando: sin sesión, nunca está en la lista de deseos. */
    public BurgerJointDto para(BurgerJoint local, Long userId) {
        return para(List.of(local), userId).get(0);
    }

    /** Varios locales para quien está mirando, en el mismo orden en que vinieron. */
    public List<BurgerJointDto> para(List<BurgerJoint> locales, Long userId) {
        if (locales.isEmpty()) {
            return List.of();
        }
        List<Long> ids = idsDe(locales);
        Set<Long> guardados = userId == null
            ? Set.of()
            : new HashSet<>(wishlistRepository.guardadosEntre(userId, ids));
        return armar(locales, ids, guardados::contains);
    }

    /** Cuando ya se sabe si están en la lista, y preguntarlo de nuevo sería una consulta de más. */
    public List<BurgerJointDto> conDeseo(List<BurgerJoint> locales, boolean enDeseos) {
        if (locales.isEmpty()) {
            return List.of();
        }
        return armar(locales, idsDe(locales), id -> enDeseos);
    }

    private List<BurgerJointDto> armar(List<BurgerJoint> locales, List<Long> ids,
                                       Predicate<Long> estaEnDeseos) {
        Map<Long, NotaDeLocalDto> notas = ratingRepository.notasDe(ids).stream()
            .collect(Collectors.toMap(NotaDeLocalDto::burgerJointId, nota -> nota));

        return locales.stream()
            .map(local -> {
                NotaDeLocalDto nota = notas.get(local.getId());
                return new BurgerJointDto(
                    local.getId(), local.getPlaceId(), local.getName(), local.getAddress(),
                    local.getArea(), local.getPhotoUrl(), local.getLatitude(), local.getLongitude(),
                    nota == null ? null : nota.promedio(),
                    nota == null ? 0L : nota.cuantas(),
                    estaEnDeseos.test(local.getId()));
            })
            .toList();
    }

    private static List<Long> idsDe(List<BurgerJoint> locales) {
        return locales.stream().map(BurgerJoint::getId).distinct().toList();
    }
}
