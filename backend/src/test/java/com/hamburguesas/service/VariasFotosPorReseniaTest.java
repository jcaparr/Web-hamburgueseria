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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Hasta cuatro fotos por reseña, en orden, y editar cuáles quedan (#185). */
class VariasFotosPorReseniaTest {

    private static final Long YO = 1L;
    private static final Long LOCAL = 5L;
    private static final RatingRequest LO_ESCRITO = new RatingRequest(4, "estaba buena");

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
            User.builder().id(YO).username("juanca").build()));
        when(burgerJointRepository.findById(LOCAL)).thenReturn(Optional.of(
            BurgerJoint.builder().id(LOCAL).name("Un local").build()));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(l -> l.getArgument(0));

        // Cada foto guardada con un nombre distinto, como en disco: n1, n2, n3...
        AtomicInteger guardadas = new AtomicInteger();
        when(fotos.guardar(any())).thenAnswer(l -> ruta("n" + guardadas.incrementAndGet()));

        service = new RatingService(ratingRepository, burgerJointRepository, userRepository, fotos,
            mock(FollowRepository.class), mock(Bloqueos.class));
    }

    @AfterEach
    void sinTransaccion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static String ruta(String nombre) {
        return "/api/rating-photos/" + nombre + ".jpg";
    }

    private static List<MultipartFile> fotosNuevas(int cuantas) {
        return IntStream.range(0, cuantas)
            .<MultipartFile>mapToObj(i -> new MockMultipartFile("foto", "f" + i + ".jpg", "image/jpeg", new byte[] {1}))
            .toList();
    }

    private Rating laMiaCon(String... actuales) {
        Rating mia = Rating.builder().id(9L).score(3).comment("vieja")
            .user(User.builder().id(YO).username("juanca").build())
            .burgerJoint(BurgerJoint.builder().id(LOCAL).name("Un local").build())
            .build();
        mia.ponerFotos(List.of(actuales));
        when(ratingRepository.findByUser_IdAndBurgerJoint_Id(YO, LOCAL)).thenReturn(Optional.of(mia));
        return mia;
    }

    // ---- al publicar ----

    @Test
    void seGuardanTodasEnElOrdenEnQueLlegaron() {
        var respuesta = service.rate(YO, LOCAL, LO_ESCRITO, fotosNuevas(3));

        assertThat(respuesta.fotos()).containsExactly(ruta("n1"), ruta("n2"), ruta("n3"));
    }

    /** La primera es la portada, y queda también donde la busca el código de antes. */
    @Test
    void laPrimeraEsLaPortada() {
        service.rate(YO, LOCAL, LO_ESCRITO, fotosNuevas(2));

        verify(ratingRepository).save(argThat(
            r -> ruta("n1").equals(r.getPhotoUrl())));
    }

    @Test
    void conMasDeCuatroNoSePublicaNiSeGuardaNada() {
        assertThatThrownBy(() -> service.rate(YO, LOCAL, LO_ESCRITO, fotosNuevas(5)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("hasta 4 fotos");

        verify(fotos, never()).guardar(any());
        verify(ratingRepository, never()).save(any());
    }

    /** Si la tercera no sirve, las dos primeras ya estaban en disco y no las apunta nadie. */
    @Test
    void siUnaNoSirveSeBorranLasQueYaSeHabianGuardado() {
        when(fotos.guardar(any()))
            .thenReturn(ruta("n1"), ruta("n2"))
            .thenThrow(new ConflictException("Eso no es una imagen"));

        assertThatThrownBy(() -> service.rate(YO, LOCAL, LO_ESCRITO, fotosNuevas(3)))
            .isInstanceOf(ConflictException.class);

        verify(fotos).borrar(ruta("n1"));
        verify(fotos).borrar(ruta("n2"));
        verify(ratingRepository, never()).save(any());
    }

    /** Y si la que falla es la base, después de guardarlas, también. */
    @Test
    void siLaBaseNoConfirmaSeBorranLasNuevas() {
        TransactionSynchronizationManager.initSynchronization();

        service.rate(YO, LOCAL, LO_ESCRITO, fotosNuevas(2));
        verify(fotos, never()).borrar(anyString());

        TransactionSynchronizationManager.getSynchronizations()
            .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(fotos).borrar(ruta("n1"));
        verify(fotos).borrar(ruta("n2"));
    }

    // ---- al editar ----

    @Test
    void sumarUnaDejaLasDeAntesPrimero() {
        Rating mia = laMiaCon(ruta("a"), ruta("b"));

        service.update(YO, LOCAL, LO_ESCRITO, fotosNuevas(1), List.of(ruta("a"), ruta("b")));

        assertThat(mia.getFotos()).containsExactly(ruta("a"), ruta("b"), ruta("n1"));
        verify(fotos, never()).borrar(anyString());
    }

    @Test
    void sacarUnaLaBorraDelDisco() {
        Rating mia = laMiaCon(ruta("a"), ruta("b"), ruta("c"));

        service.update(YO, LOCAL, LO_ESCRITO, null, List.of(ruta("a"), ruta("c")));

        assertThat(mia.getFotos()).containsExactly(ruta("a"), ruta("c"));
        verify(fotos).borrar(ruta("b"));
    }

    /** Sacar la portada deja de portada a la que seguía. */
    @Test
    void sacarLaPortadaPoneLaSiguienteEnSuLugar() {
        Rating mia = laMiaCon(ruta("a"), ruta("b"));

        service.update(YO, LOCAL, LO_ESCRITO, null, List.of(ruta("b")));

        assertThat(mia.getPhotoUrl()).isEqualTo(ruta("b"));
    }

    /** El archivo se borra recién cuando la base confirmó: si no, la reseña lo seguiría apuntando. */
    @Test
    void laSacadaSeBorraRecienCuandoLaBaseConfirma() {
        laMiaCon(ruta("a"), ruta("b"));
        TransactionSynchronizationManager.initSynchronization();

        service.update(YO, LOCAL, LO_ESCRITO, null, List.of(ruta("a")));
        verify(fotos, never()).borrar(anyString());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(fotos).borrar(ruta("b"));
    }

    /**
     * Solo se puede dejar una foto que ya era de la reseña.
     *
     * Si no, alguien podría poner la ruta de la foto de otra persona, y en la próxima
     * edición, al sacarla, borrarle el archivo.
     */
    @Test
    void unaFotoQueNoEraDeLaReseniaSeRechaza() {
        Rating mia = laMiaCon(ruta("a"));

        assertThatThrownBy(() -> service.update(YO, LOCAL, LO_ESCRITO, null, List.of(ruta("ajena"))))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("no es de tu reseña");

        assertThat(mia.getFotos()).containsExactly(ruta("a"));
        verify(fotos, never()).borrar(anyString());
    }

    @Test
    void sacarlasTodasSinSumarNingunaSeRechaza() {
        laMiaCon(ruta("a"));

        assertThatThrownBy(() -> service.update(YO, LOCAL, LO_ESCRITO, null, List.of()))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("necesita una foto");
    }

    @Test
    void pasarseDeCuatroAlEditarSeRechazaSinGuardarNada() {
        laMiaCon(ruta("a"), ruta("b"), ruta("c"));

        assertThatThrownBy(() -> service.update(
            YO, LOCAL, LO_ESCRITO, fotosNuevas(2), List.of(ruta("a"), ruta("b"), ruta("c"))))
            .isInstanceOf(ConflictException.class);

        verify(fotos, never()).guardar(any());
    }

    /**
     * La versión de la app de antes de #185 no manda cuáles quedan: si trae una foto,
     * reemplaza a la que había, como hacía siempre.
     */
    @Test
    void sinDecirCualesQuedanUnaNuevaReemplazaATodas() {
        Rating mia = laMiaCon(ruta("a"));

        service.update(YO, LOCAL, LO_ESCRITO, fotosNuevas(1), null);

        assertThat(mia.getFotos()).containsExactly(ruta("n1"));
        verify(fotos).borrar(ruta("a"));
    }

    /** Y sin fotos nuevas ni lista, editar el texto deja las fotos como estaban. */
    @Test
    void sinDecirCualesQuedanNiMandarNuevasQuedanTodas() {
        Rating mia = laMiaCon(ruta("a"), ruta("b"));

        service.update(YO, LOCAL, LO_ESCRITO, null, null);

        assertThat(mia.getFotos()).containsExactly(ruta("a"), ruta("b"));
        verify(fotos, never()).borrar(anyString());
    }

    // ---- al borrar ----

    @Test
    void borrarLaReseniaBorraTodasSusFotos() {
        laMiaCon(ruta("a"), ruta("b"), ruta("c"));

        service.borrar(YO, LOCAL);

        verify(fotos).borrar(ruta("a"));
        verify(fotos).borrar(ruta("b"));
        verify(fotos).borrar(ruta("c"));
    }
}
