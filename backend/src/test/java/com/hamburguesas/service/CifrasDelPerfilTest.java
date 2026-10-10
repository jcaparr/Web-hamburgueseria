package com.hamburguesas.service;

import com.hamburguesas.dto.ProfileStatsDto;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Las cifras de tu perfil: reseñas, promedio, seguidores y a cuántos seguís (#232). */
class CifrasDelPerfilTest {

    @Test
    void traeAQuienesSigueAdemasDeLosSeguidores() {
        RatingRepository ratings = mock(RatingRepository.class);
        FollowRepository follows = mock(FollowRepository.class);
        when(ratings.countByUser_Id(7L)).thenReturn(2L);
        when(ratings.averageScoreByUser(7L)).thenReturn(4.5);
        when(follows.countByFollowed_Id(7L)).thenReturn(5L);
        when(follows.countByFollower_Id(7L)).thenReturn(7L);

        ProfileStatsDto cifras = new ProfileService(ratings, follows, mock(UserRepository.class)).stats(7L);

        assertThat(cifras.seguidores()).isEqualTo(5);
        assertThat(cifras.siguiendo()).isEqualTo(7);
        assertThat(cifras.ratingsCount()).isEqualTo(2);
        assertThat(cifras.averageScore()).isEqualTo(4.5);
    }
}
