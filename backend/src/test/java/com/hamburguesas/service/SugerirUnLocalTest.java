package com.hamburguesas.service;

import com.hamburguesas.dto.SugerenciaDeLocalRequest;
import com.hamburguesas.exception.TooManyRequestsException;
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

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** "Sugerir un local" (#257): qué se guarda, el tope de 5 por día y qué se rechaza. */
@SpringBootTest
@ActiveProfiles("test")
class SugerirUnLocalTest {

    @Autowired private SugerenciasDeLocales sugerenciasDeLocales;
    @Autowired private SugerenciaDeLocalRepository sugerencias;
    @Autowired private UserRepository userRepository;
    @Autowired private Validator validator;

    private User ana;
    private User beto;

    /** La base es compartida entre tests: se busca antes de crear. */
    @BeforeEach
    void setUp() {
        sugerencias.deleteAll();
        ana = alguien("anasug");
        beto = alguien("betosug");
    }

    @AfterEach
    void tearDown() {
        sugerencias.deleteAll();
    }

    private User alguien(String username) {
        return userRepository.findByUsername(username).orElseGet(() -> userRepository.save(
            User.builder().username(username).email(username + "@example.com").emailVerified(true).build()));
    }

    private static SugerenciaDeLocalRequest pedido(String nombre) {
        return new SugerenciaDeLocalRequest(nombre, "Av. Corrientes 1234", "ana@example.com", null);
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

    @Test
    void laSextaDelDiaSeRechazaYNoSeGuarda() {
        for (int i = 1; i <= SugerenciasDeLocales.TOPE_POR_DIA; i++) {
            sugerenciasDeLocales.sugerir(ana.getId(), pedido("Local " + i));
        }

        assertThatThrownBy(() -> sugerenciasDeLocales.sugerir(ana.getId(), pedido("Local 6")))
            .isInstanceOf(TooManyRequestsException.class);
        assertThat(sugerencias.count()).isEqualTo(SugerenciasDeLocales.TOPE_POR_DIA);
    }

    /** El tope es de cada persona: que Ana lo gaste no deja afuera a Beto. */
    @Test
    void elTopeEsPorPersona() {
        for (int i = 1; i <= SugerenciasDeLocales.TOPE_POR_DIA; i++) {
            sugerenciasDeLocales.sugerir(ana.getId(), pedido("Local " + i));
        }

        sugerenciasDeLocales.sugerir(beto.getId(), pedido("El de Beto"));

        assertThat(sugerencias.count()).isEqualTo(SugerenciasDeLocales.TOPE_POR_DIA + 1);
    }

    /** Las de hace más de 24 horas no cuentan para el tope. */
    @Test
    void lasDeAyerNoCuentan() {
        Instant ayer = Instant.now().minus(Duration.ofHours(25));
        for (int i = 1; i <= SugerenciasDeLocales.TOPE_POR_DIA; i++) {
            sugerencias.save(SugerenciaDeLocal.builder().user(ana).nombre("Viejo " + i)
                .direccion("x").email("ana@example.com").createdAt(ayer).build());
        }

        sugerenciasDeLocales.sugerir(ana.getId(), pedido("Nuevo"));

        assertThat(sugerencias.count()).isEqualTo(SugerenciasDeLocales.TOPE_POR_DIA + 1);
    }

    @Test
    void sinNombreSinDireccionOConEmailMalNoPasa() {
        assertThat(validator.validate(new SugerenciaDeLocalRequest(" ", "Calle 1", "a@b.com", null)))
            .extracting(v -> v.getPropertyPath().toString()).containsExactly("nombre");
        assertThat(validator.validate(new SugerenciaDeLocalRequest("Burger", "", "a@b.com", null)))
            .extracting(v -> v.getPropertyPath().toString()).containsExactly("direccion");
        assertThat(validator.validate(new SugerenciaDeLocalRequest("Burger", "Calle 1", "no-es-un-mail", null)))
            .extracting(v -> v.getPropertyPath().toString()).containsExactly("email");
        assertThat(validator.validate(new SugerenciaDeLocalRequest("Burger", "Calle 1", "a@b.com", "x".repeat(1001))))
            .extracting(v -> v.getPropertyPath().toString()).containsExactly("comentario");
        assertThat(validator.validate(new SugerenciaDeLocalRequest("Burger", "Calle 1", "a@b.com", null))).isEmpty();
    }
}
