package com.hamburguesas.service;

import com.hamburguesas.dto.DesgloseDeNotasDto;
import com.hamburguesas.dto.RatingRequest;
import com.hamburguesas.dto.RatingResponse;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.fotos.FotosDeResenias;
import com.hamburguesas.logros.Logros;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** La fecha de la visita y las tres notas opcionales (#336): qué se valida y qué se guarda. */
class FechaYNotasDeLaReseniaTest {

    private static final Long YO = 1L;
    private static final Long LOCAL = 5L;

    private RatingRepository ratingRepository;
    private ApplicationEventPublisher eventos;
    private RatingService service;

    @BeforeEach
    void setUp() {
        ratingRepository = mock(RatingRepository.class);
        FotosDeResenias fotos = mock(FotosDeResenias.class);
        eventos = mock(ApplicationEventPublisher.class);
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
            mock(Bloqueos.class), mock(Reacciones.class), eventos);
    }

    private static LocalDate hoy() {
        return LocalDate.now(Logros.ZONA);
    }

    private MultipartFile unaFoto() {
        return new MockMultipartFile("foto", "hamburguesa.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    private RatingRequest con(LocalDate dia, Integer local, Integer atencion, Integer precio) {
        return new RatingRequest(4, "rica", dia, local, atencion, precio);
    }

    @Test
    void guardaElDiaYLasTresNotas() {
        RatingResponse r = service.rate(YO, LOCAL, con(hoy().minusDays(3), 5, 3, 2), List.of(unaFoto()));

        assertThat(r.visitDate()).isEqualTo(hoy().minusDays(3));
        assertThat(r.scoreLocal()).isEqualTo(5);
        assertThat(r.scoreAtencion()).isEqualTo(3);
        assertThat(r.scorePrecio()).isEqualTo(2);
        // La general sigue siendo la que cuenta.
        assertThat(r.score()).isEqualTo(4);
    }

    @Test
    void sinFechaNiNotasSigueAndandoComoAntes() {
        RatingResponse r = service.rate(YO, LOCAL, new RatingRequest(4, "rica"), List.of(unaFoto()));

        assertThat(r.visitDate()).isNull();
        assertThat(r.scoreLocal()).isNull();
        assertThat(r.scoreAtencion()).isNull();
        assertThat(r.scorePrecio()).isNull();
    }

    @Test
    void hoyYHaceUnAnioEntran() {
        service.rate(YO, LOCAL, con(hoy(), null, null, null), List.of(unaFoto()));
        when(ratingRepository.findByUser_IdAndBurgerJoint_Id(YO, LOCAL)).thenReturn(Optional.empty());
        service.rate(YO, LOCAL, con(hoy().minusYears(1), null, null, null), List.of(unaFoto()));
    }

    @Test
    void unaFechaFuturaNoEntra() {
        assertThatThrownBy(() -> service.rate(YO, LOCAL, con(hoy().plusDays(1), null, null, null),
            List.of(unaFoto())))
            .isInstanceOf(ConflictException.class).hasMessageContaining("futura");
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void unaFechaDeHaceMasDeUnAnioNoEntra() {
        assertThatThrownBy(() -> service.rate(YO, LOCAL, con(hoy().minusYears(1).minusDays(1), null, null, null),
            List.of(unaFoto())))
            .isInstanceOf(ConflictException.class).hasMessageContaining("un año");
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void alEditarSeCambianLosCamposYNoSePideRevalidarLaFechaVieja() {
        LocalDate vieja = hoy().minusYears(1).minusDays(40);
        Rating mia = Rating.builder().id(9L).score(3).comment("vieja").photoUrl("/api/rating-photos/a.jpg")
            .visitDate(vieja).scoreLocal(1)
            .user(User.builder().id(YO).username("juanca").build())
            .burgerJoint(BurgerJoint.builder().id(LOCAL).name("Un local").build())
            .build();
        when(ratingRepository.findByUser_IdAndBurgerJoint_Id(YO, LOCAL)).thenReturn(Optional.of(mia));

        // La misma fecha vieja se acepta: se corrige otra cosa.
        service.update(YO, LOCAL, con(vieja, 4, null, 5), null, null);

        assertThat(mia.getVisitDate()).isEqualTo(vieja);
        assertThat(mia.getScoreLocal()).isEqualTo(4);
        assertThat(mia.getScoreAtencion()).isNull();
        assertThat(mia.getScorePrecio()).isEqualTo(5);

        // Pero una fecha nueva de hace más de un año, no.
        assertThatThrownBy(() -> service.update(YO, LOCAL, con(vieja.minusDays(1), null, null, null), null, null))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void elDesgloseDelResumenSaleDelRepositorio() {
        var desglose = new DesgloseDeNotasDto(4.5, 2L, null, 0L, 3.0, 1L);
        when(ratingRepository.desgloseDeNotas(LOCAL)).thenReturn(desglose);
        when(ratingRepository.distribucionDeNotas(LOCAL)).thenReturn(List.of());
        when(ratingRepository.notasYComentarios(LOCAL)).thenReturn(List.of());

        assertThat(service.resumen(LOCAL, null).desglose()).isEqualTo(desglose);
    }
    @Test
    void editarUnaReseniaDeAntesDeUnAnioConSuFechaDeCargaNoSeRechaza() {
        java.time.Instant cargada = hoy().minusYears(1).minusDays(30).atStartOfDay(Logros.ZONA).toInstant();
        Rating vieja = Rating.builder().id(9L).score(3).comment("vieja").photoUrl("/api/rating-photos/a.jpg")
            .createdAt(cargada)
            .user(User.builder().id(YO).username("juanca").build())
            .burgerJoint(BurgerJoint.builder().id(LOCAL).name("Un local").build())
            .build();
        when(ratingRepository.findByUser_IdAndBurgerJoint_Id(YO, LOCAL)).thenReturn(Optional.of(vieja));

        LocalDate diaDeCarga = cargada.atZone(Logros.ZONA).toLocalDate();
        service.update(YO, LOCAL, con(diaDeCarga, null, null, null), null, null);

        assertThat(vieja.getVisitDate()).isEqualTo(diaDeCarga);
    }

    @Test
    void laReseniaPropiaSeBuscaPorLocalYPorUsuario() {
        assertThat(service.mia(YO, LOCAL)).isEmpty();

        Rating mia = Rating.builder().id(9L).score(3).comment("x").visitDate(hoy())
            .user(User.builder().id(YO).username("juanca").build())
            .burgerJoint(BurgerJoint.builder().id(LOCAL).name("Un local").build())
            .build();
        when(ratingRepository.findByUser_IdAndBurgerJoint_Id(YO, LOCAL)).thenReturn(Optional.of(mia));

        assertThat(service.mia(YO, LOCAL)).get().extracting(RatingResponse::visitDate).isEqualTo(hoy());
    }
}
