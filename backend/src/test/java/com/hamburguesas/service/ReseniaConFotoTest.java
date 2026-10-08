package com.hamburguesas.service;

import com.hamburguesas.dto.RatingRequest;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.fotos.FotosDeResenias;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Toda reseña lleva una foto: qué pasa cuando no la lleva, y qué cuando ya la tenía. */
class ReseniaConFotoTest {

    private static final Long YO = 1L;
    private static final Long LOCAL = 5L;

    private RatingRepository ratingRepository;
    private FotosDeResenias fotos;
    private RatingService service;

    @BeforeEach
    void setUp() {
        ratingRepository = mock(RatingRepository.class);
        fotos = mock(FotosDeResenias.class);
        UserRepository userRepository = mock(UserRepository.class);
        BurgerJointRepository burgerJointRepository = mock(BurgerJointRepository.class);

        when(ratingRepository.findByUser_IdAndBurgerJoint_Id(YO, LOCAL)).thenReturn(Optional.empty());
        when(userRepository.findById(YO)).thenReturn(Optional.of(
            User.builder().id(YO).username("juanca").email("juan@example.com").build()));
        when(burgerJointRepository.findById(LOCAL)).thenReturn(Optional.of(
            BurgerJoint.builder().id(LOCAL).name("Un local").address("Una calle").build()));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(l -> l.getArgument(0));
        when(fotos.guardar(any())).thenReturn("/api/rating-photos/nueva.jpg");

        service = new RatingService(ratingRepository, burgerJointRepository, userRepository, fotos,
            mock(FollowRepository.class), mock(Bloqueos.class));
    }

    private MultipartFile unaFoto() {
        return new MockMultipartFile("foto", "hamburguesa.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    private RatingRequest loEscrito() {
        return new RatingRequest(4, "estaba buena");
    }

    private Rating laMiaCon(String fotoActual) {
        Rating mia = Rating.builder().id(9L).score(3).comment("vieja").photoUrl(fotoActual)
            .user(User.builder().id(YO).username("juanca").build())
            .burgerJoint(BurgerJoint.builder().id(LOCAL).name("Un local").build())
            .build();
        when(ratingRepository.findByUser_IdAndBurgerJoint_Id(YO, LOCAL)).thenReturn(Optional.of(mia));
        return mia;
    }

    @Test
    void unaReseniaNuevaSeGuardaConSuFoto() {
        var respuesta = service.rate(YO, LOCAL, loEscrito(), List.of(unaFoto()));

        assertThat(respuesta.fotos()).containsExactly("/api/rating-photos/nueva.jpg");
    }

    @Test
    void sinFotoNoSePublica() {
        assertThatThrownBy(() -> service.rate(YO, LOCAL, loEscrito(), null))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("foto de lo que comiste");
    }

    /**
     * Y no queda nada escrito: la reseña sin foto no llega a existir.
     *
     * Es lo que cambió al volverse obligatoria. Antes el texto se guardaba primero y la
     * foto iba después, para que un problema con la foto no se llevara puesto lo
     * escrito; ahora eso dejaría en la base justo lo que la app no acepta.
     */
    @Test
    void yTampocoSeGuardaElTextoSuelto() {
        assertThatThrownBy(() -> service.rate(YO, LOCAL, loEscrito(), null))
            .isInstanceOf(ConflictException.class);

        verify(ratingRepository, never()).save(any());
    }

    /** Un archivo vacío es lo mismo que no mandar nada. */
    @Test
    void unArchivoVacioNoCuentaComoFoto() {
        var vacio = new MockMultipartFile("foto", "vacia.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.rate(YO, LOCAL, loEscrito(), List.of(vacio)))
            .isInstanceOf(ConflictException.class);
    }

    /**
     * Si la foto no sirve, tampoco se guarda la reseña.
     *
     * Se intenta guardarla antes de tocar la base justamente para esto: el rechazo
     * ocurre antes de que haya nada escrito que deshacer.
     */
    @Test
    void siLaFotoNoSirveNoSeGuardaLaResenia() {
        when(fotos.guardar(any())).thenThrow(new ConflictException("Eso no es una imagen"));

        assertThatThrownBy(() -> service.rate(YO, LOCAL, loEscrito(), List.of(unaFoto())))
            .isInstanceOf(ConflictException.class);

        verify(ratingRepository, never()).save(any());
    }

    /** Editar el texto de una reseña que ya tiene foto no obliga a sacarla de nuevo. */
    @Test
    void editarSinMandarFotoDejaLaQueYaTenia() {
        Rating mia = laMiaCon("/api/rating-photos/vieja.jpg");

        service.update(YO, LOCAL, new RatingRequest(5, "mejoró"), null, null);

        assertThat(mia.getPhotoUrl()).isEqualTo("/api/rating-photos/vieja.jpg");
        assertThat(mia.getComment()).isEqualTo("mejoró");
    }

    /**
     * Pero una reseña vieja, de antes de esta regla, sí la pide al editarla.
     *
     * Es la forma de que "toda reseña tiene foto" termine siendo cierto sin borrarle la
     * reseña a nadie de un día para el otro.
     */
    @Test
    void editarUnaReseniaSinFotoPideUna() {
        laMiaCon(null);

        assertThatThrownBy(() -> service.update(YO, LOCAL, loEscrito(), null, null))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("necesita una foto");
    }

    /** Cambiar la foto borra la anterior: si no, quedarían archivos que nadie mira. */
    @Test
    void cambiarLaFotoBorraLaAnterior() {
        Rating mia = laMiaCon("/api/rating-photos/vieja.jpg");

        service.update(YO, LOCAL, loEscrito(), List.of(unaFoto()), null);

        assertThat(mia.getPhotoUrl()).isEqualTo("/api/rating-photos/nueva.jpg");
        ArgumentCaptor<String> borrada = ArgumentCaptor.forClass(String.class);
        verify(fotos).borrar(borrada.capture());
        assertThat(borrada.getValue()).isEqualTo("/api/rating-photos/vieja.jpg");
    }

    /** Y ponerle la primera foto a una vieja no intenta borrar nada. */
    @Test
    void ponerleFotoAUnaViejaNoBorraNada() {
        laMiaCon(null);

        service.update(YO, LOCAL, loEscrito(), List.of(unaFoto()), null);

        verify(fotos, never()).borrar(any());
    }
}
