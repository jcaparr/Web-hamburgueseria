package com.hamburguesas.service;

import com.hamburguesas.auth.SessionIssuer;
import com.hamburguesas.dto.AuthResponse;
import com.hamburguesas.dto.ProfileStatsDto;
import com.hamburguesas.dto.ReseniaDePerfilDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final RatingRepository ratingRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public ProfileStatsDto stats(Long userId) {
        long ratingsCount = ratingRepository.countByUser_Id(userId);
        Double averageScore = ratingRepository.averageScoreByUser(userId);
        return new ProfileStatsDto(
            ratingsCount, followRepository.countByFollowed_Id(userId),
            followRepository.countByFollower_Id(userId), averageScore);
    }

    public List<ReseniaDePerfilDto> myRatings(Long userId) {
        return ratingRepository.findAllByUserOrderByCreatedAtDesc(userId);
    }

    /**
     * Guarda la hamburguesa que eligió para su avatar, o la borra para volver a la que
     * sale de su nombre.
     *
     * Devuelve quién es, igual que al entrar: el navegador reemplaza con eso lo que
     * sabía de la sesión, y el avatar cambia en toda la app sin volver a preguntar.
     *
     * @param receta las cinco cifras, ya validadas por {@link com.hamburguesas.dto.HamburguesaRequest}, o null
     */
    @Transactional
    public AuthResponse cambiarHamburguesa(Long userId, String receta) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("No encontramos tu cuenta"));
        user.setHamburguesa(receta);
        return SessionIssuer.quienEs(user);
    }
}
