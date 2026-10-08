package com.hamburguesas.service;

import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.fotos.FotosDeResenias;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cada uno puede borrar su reseña, con la foto, y solo la suya (#180). */
class BorrarReseniaTest {

    private static final Long YO = 1L;
    private static final Long OTRO = 2L;
    private static final Long LOCAL = 5L;

    private RatingRepository ratingRepository;
    private FotosDeResenias fotos;
    private RatingService service;

    @BeforeEach
    void setUp() {
        ratingRepository = mock(RatingRepository.class);
        fotos = mock(FotosDeResenias.class);
        when(ratingRepository.findByUser_IdAndBurgerJoint_Id(any(), any())).thenReturn(Optional.empty());
        service = new RatingService(ratingRepository, mock(BurgerJointRepository.class),
            mock(UserRepository.class), fotos, mock(FollowRepository.class), mock(Bloqueos.class),
            mock(Reacciones.class));
    }

    @AfterEach
    void sinTransaccion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private Rating reseniaDe(Long usuario, String foto) {
        Rating resenia = Rating.builder().id(9L).score(4).comment("buena").photoUrl(foto)
            .user(User.builder().id(usuario).username("u" + usuario).build())
            .burgerJoint(BurgerJoint.builder().id(LOCAL).name("Un local").build())
            .build();
        when(ratingRepository.findByUser_IdAndBurgerJoint_Id(usuario, LOCAL)).thenReturn(Optional.of(resenia));
        return resenia;
    }

    @Test
    void borraLaPropiaYElArchivoDeSuFoto() {
        Rating mia = reseniaDe(YO, "/api/rating-photos/mia.jpg");

        service.borrar(YO, LOCAL);

        verify(ratingRepository).delete(mia);
        verify(fotos).borrar("/api/rating-photos/mia.jpg");
    }

    @Test
    void unaViejaSinFotoSeBorraSinTocarArchivos() {
        Rating mia = reseniaDe(YO, null);

        service.borrar(YO, LOCAL);

        verify(ratingRepository).delete(mia);
        verify(fotos, never()).borrar(anyString());
    }

    /** Se busca por quien pide: la de otro en el mismo local no aparece por ningún lado. */
    @Test
    void laDeOtroNoSePuedeBorrar() {
        reseniaDe(OTRO, "/api/rating-photos/ajena.jpg");

        assertThatThrownBy(() -> service.borrar(YO, LOCAL)).isInstanceOf(ResourceNotFoundException.class);

        verify(ratingRepository, never()).delete(any());
        verify(fotos, never()).borrar(anyString());
    }

    @Test
    void siNoHayReseniaDa404() {
        assertThatThrownBy(() -> service.borrar(YO, LOCAL)).isInstanceOf(ResourceNotFoundException.class);

        verify(ratingRepository, never()).delete(any());
    }

    /** Si la base no llega a confirmar, la reseña sigue y tiene que seguir con su foto. */
    @Test
    void laFotoSeBorraRecienCuandoLaBaseConfirma() {
        reseniaDe(YO, "/api/rating-photos/mia.jpg");
        TransactionSynchronizationManager.initSynchronization();

        service.borrar(YO, LOCAL);
        verify(fotos, never()).borrar(anyString());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(fotos).borrar("/api/rating-photos/mia.jpg");
    }
}
