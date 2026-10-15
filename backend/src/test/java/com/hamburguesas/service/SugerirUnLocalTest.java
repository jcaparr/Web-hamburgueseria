package com.hamburguesas.service;

import com.hamburguesas.dto.SugerenciaDeLocalRequest;
import com.hamburguesas.exception.EmailNotVerifiedException;
import com.hamburguesas.exception.TooManyRequestsException;
import com.hamburguesas.mail.EmailDeliveryException;
import com.hamburguesas.mail.EmailService;
import com.hamburguesas.model.SugerenciaDeLocal;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.SugerenciaDeLocalRepository;
import com.hamburguesas.repository.UserRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** "Sugerir un local" (#257): qué se guarda, el mail a Juan y cada uno de los topes. */
@SpringBootTest
@ActiveProfiles("test")
class SugerirUnLocalTest {

    @Autowired private SugerenciasDeLocales sugerenciasDeLocales;
    @Autowired private SugerenciaDeLocalRepository sugerencias;
    @Autowired private UserRepository userRepository;
    @Autowired private SugerenciasProperties topes;
    @Autowired private Validator validator;
    @MockitoBean private EmailService emailService;

    private User ana;
    private User beto;

    /** La base es compartida entre tests: se busca antes de crear. */
    @BeforeEach
    void setUp() {
        sugerencias.deleteAll();
        ana = alguien("anasug", true);
        beto = alguien("betosug", true);
    }

    /** Los topes se achican en algunos tests: vuelven a los de siempre. */
    @AfterEach
    void tearDown() {
        sugerencias.deleteAll();
        topes.setTopeTotal(100);
        topes.setTopeDeMails(20);
    }

    private User alguien(String username, boolean verificado) {
        return userRepository.findByUsername(username).orElseGet(() -> userRepository.save(
            User.builder().username(username).email(username + "@example.com").emailVerified(verificado).build()));
    }

    private static SugerenciaDeLocalRequest pedido(String nombre) {
        return new SugerenciaDeLocalRequest(nombre, "Av. Corrientes 1234", "ana@example.com", null);
    }

    /** Una que ya estaba, de hace un rato: pasa por fuera del servicio y sus topes. */
    private void yaHabia(User quien, Duration haceCuanto) {
        sugerencias.save(SugerenciaDeLocal.builder().user(quien).nombre("Anterior")
            .direccion("x").email(quien.getEmail()).createdAt(Instant.now().minus(haceCuanto)).build());
    }

    @Test
    void guardaLoQueMandoSinLosEspaciosDeLosBordes() {
        sugerenciasDeLocales.sugerir(ana.getId(), new SugerenciaDeLocalRequest(
            "  Burger Uno ", " instagram.com/burgeruno ", " otro@example.com ", "   "));

        SugerenciaDeLocal guardada = sugerencias.findAll().get(0);
        assertThat(guardada.getUser().getId()).isEqualTo(ana.getId());
        assertThat(guardada.getNombre()).isEqualTo("Burger Uno");
        assertThat(guardada.getDireccion()).isEqualTo("instagram.com/burgeruno");
        assertThat(guardada.getEmail()).isEqualTo("otro@example.com");
        assertThat(guardada.getComentario()).isNull();
        assertThat(guardada.getCreatedAt()).isNotNull();
    }

    /** A hola@, con todo lo que mandó y respondiendo a quien la sugirió. */
    @Test
    void leMandaUnMailAJuanQueSeContestaAQuienLaSugirio() {
        sugerenciasDeLocales.sugerir(ana.getId(), new SugerenciaDeLocalRequest(
            "Burger\nUno", "instagram.com/burgeruno", "otro@example.com", "La mejor de Caballito"));

        verify(emailService).send(
            eq("hola@burgometro.com.ar"),
            eq("Sugerencia de local: Burger Uno"),
            contains("instagram.com/burgeruno"),
            eq("otro@example.com"));
        verify(emailService).send(anyString(), anyString(), contains("@anasug"), anyString());
        verify(emailService).send(anyString(), anyString(), contains("La mejor de Caballito"), anyString());
    }

    @Test
    void siElMailFallaLaSugerenciaQuedaGuardada() {
        doThrow(new EmailDeliveryException("caído", null))
            .when(emailService).send(anyString(), anyString(), anyString(), anyString());

        sugerenciasDeLocales.sugerir(ana.getId(), pedido("Burger Uno"));

        assertThat(sugerencias.count()).isEqualTo(1);
    }

    @Test
    void sinElEmailVerificadoNoPuede() {
        User carla = alguien("carlasug", false);

        assertThatThrownBy(() -> sugerenciasDeLocales.sugerir(carla.getId(), pedido("Burger")))
            .isInstanceOf(EmailNotVerifiedException.class);
        assertThat(sugerencias.count()).isZero();
    }

    @Test
    void hayQueEsperarUnMinutoEntreUnaYOtra() {
        sugerenciasDeLocales.sugerir(ana.getId(), pedido("Primera"));

        assertThatThrownBy(() -> sugerenciasDeLocales.sugerir(ana.getId(), pedido("Segunda")))
            .isInstanceOf(TooManyRequestsException.class)
            .hasMessage("Esperá un minuto antes de mandar otra.");

        // Pasado el minuto, sí.
        sugerencias.deleteAll();
        yaHabia(ana, Duration.ofSeconds(61));
        sugerenciasDeLocales.sugerir(ana.getId(), pedido("Segunda"));
        assertThat(sugerencias.count()).isEqualTo(2);
    }

    @Test
    void laSextaDelDiaSeRechazaYNoSeGuarda() {
        for (int i = 1; i <= 5; i++) {
            yaHabia(ana, Duration.ofMinutes(i * 10L));
        }

        assertThatThrownBy(() -> sugerenciasDeLocales.sugerir(ana.getId(), pedido("Sexta")))
            .isInstanceOf(TooManyRequestsException.class)
            .hasMessage("Ya mandaste varias hoy. Probá mañana.");
        assertThat(sugerencias.count()).isEqualTo(5);
    }

    /** El tope es de cada persona: que Ana lo gaste no deja afuera a Beto. */
    @Test
    void elTopeEsPorPersona() {
        for (int i = 1; i <= 5; i++) {
            yaHabia(ana, Duration.ofMinutes(i * 10L));
        }

        sugerenciasDeLocales.sugerir(beto.getId(), pedido("La de Beto"));

        assertThat(sugerencias.count()).isEqualTo(6);
    }

    /** Las de hace más de 24 horas no cuentan para el tope. */
    @Test
    void lasDeAyerNoCuentan() {
        for (int i = 1; i <= 5; i++) {
            yaHabia(ana, Duration.ofHours(25));
        }

        sugerenciasDeLocales.sugerir(ana.getId(), pedido("Nueva"));

        assertThat(sugerencias.count()).isEqualTo(6);
    }

    /** Muchas cuentas juntas tampoco pasan del tope total. */
    @Test
    void pasadoElTopeTotalSeRechazaParaTodos() {
        topes.setTopeTotal(3);
        yaHabia(beto, Duration.ofMinutes(10));
        yaHabia(beto, Duration.ofMinutes(20));
        yaHabia(beto, Duration.ofMinutes(30));

        assertThatThrownBy(() -> sugerenciasDeLocales.sugerir(ana.getId(), pedido("Una más")))
            .isInstanceOf(TooManyRequestsException.class)
            .hasMessage("Hoy recibimos muchas sugerencias. Probá mañana.");
        assertThat(sugerencias.count()).isEqualTo(3);
    }

    /** Con 5 y la última recién mandada, lo que la frena es el tope del día, no el minuto. */
    @Test
    void conElDiaLlenoAvisaQueEsHastaMananaAunqueNoHayaPasadoElMinuto() {
        yaHabia(ana, Duration.ofSeconds(10));
        for (int i = 1; i <= 4; i++) {
            yaHabia(ana, Duration.ofMinutes(i * 10L));
        }

        assertThatThrownBy(() -> sugerenciasDeLocales.sugerir(ana.getId(), pedido("Sexta")))
            .hasMessage("Ya mandaste varias hoy. Probá mañana.");
    }

    /** La última que entra en el tope de mails todavía lo manda: el borde, donde se esconde un error de a uno. */
    @Test
    void laUltimaDentroDelTopeDeMailsSiAvisa() {
        topes.setTopeDeMails(2);
        yaHabia(beto, Duration.ofMinutes(10));

        sugerenciasDeLocales.sugerir(ana.getId(), pedido("Segunda del día"));

        verify(emailService).send(anyString(), anyString(), anyString(), anyString());
    }

    /** Pasado el tope de mails se guarda igual, pero sin mail: los códigos necesitan los de Brevo. */
    @Test
    void pasadoElTopeDeMailsSeGuardaSinMail() {
        topes.setTopeDeMails(2);
        yaHabia(beto, Duration.ofMinutes(10));
        yaHabia(beto, Duration.ofMinutes(20));

        sugerenciasDeLocales.sugerir(ana.getId(), pedido("Sin mail"));

        assertThat(sugerencias.count()).isEqualTo(3);
        verify(emailService, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void sinNombreSinDireccionConEmailMalOMuyLargoNoPasa() {
        assertThat(validator.validate(new SugerenciaDeLocalRequest(" ", "Calle 1", "a@b.com", null)))
            .extracting(v -> v.getPropertyPath().toString()).containsExactly("nombre");
        assertThat(validator.validate(new SugerenciaDeLocalRequest("Burger", "", "a@b.com", null)))
            .extracting(v -> v.getPropertyPath().toString()).containsExactly("direccion");
        assertThat(validator.validate(new SugerenciaDeLocalRequest("Burger", "Calle 1", "no-es-un-mail", null)))
            .extracting(v -> v.getPropertyPath().toString()).containsExactly("email");
        assertThat(validator.validate(new SugerenciaDeLocalRequest("x".repeat(121), "Calle 1", "a@b.com", null)))
            .extracting(v -> v.getPropertyPath().toString()).containsExactly("nombre");
        assertThat(validator.validate(new SugerenciaDeLocalRequest("Burger", "x".repeat(301), "a@b.com", null)))
            .extracting(v -> v.getPropertyPath().toString()).containsExactly("direccion");
        assertThat(validator.validate(new SugerenciaDeLocalRequest("Burger", "Calle 1", "a@b.com", "x".repeat(501))))
            .extracting(v -> v.getPropertyPath().toString()).containsExactly("comentario");
        assertThat(validator.validate(new SugerenciaDeLocalRequest("Burger", "Calle 1", "a@b.com", null))).isEmpty();
    }
}
