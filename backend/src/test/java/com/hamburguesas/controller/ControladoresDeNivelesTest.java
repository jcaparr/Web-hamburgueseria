package com.hamburguesas.controller;

import com.hamburguesas.auth.SessionCookies;
import com.hamburguesas.logros.LogroDeUsuarioRepository;
import com.hamburguesas.model.Block;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BlockRepository;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import com.hamburguesas.security.JwtService;
import com.hamburguesas.service.RecalculoDeXp;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Por HTTP y con toda la cadena de seguridad (#309): qué responden sin sesión los endpoints de
 * niveles y logros, los 409 y 400 que prometen las issues, y que el podio y el perfil ajeno
 * respeten los bloqueos y la privacidad de cada uno.
 */
@SpringBootTest
@ActiveProfiles("test")
class ControladoresDeNivelesTest {

    private static final String LARGO =
        "Muy buena la carne, el pan estaba tostado justo y la salsa me encantó bastante; las papas también salieron crocantes";

    @Autowired private WebApplicationContext contexto;
    @Autowired private JwtService jwt;
    @Autowired private UserRepository userRepository;
    @Autowired private BlockRepository blockRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired private LogroDeUsuarioRepository logroRepository;
    @Autowired private RecalculoDeXp recalculoDeXp;

    private MockMvc mvc;
    private User ana;
    private User beto;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        limpiar();
        ana = cuenta("http_ana");
        beto = cuenta("http_beto");
    }

    @AfterEach
    void limpiar() {
        blockRepository.deleteAll();
        ratingRepository.deleteAll();
        logroRepository.deleteAll();
    }

    private User cuenta(String nombre) {
        User u = userRepository.findByUsername(nombre).orElseGet(() -> userRepository.save(
            User.builder().username(nombre).email(nombre + "@example.com").emailVerified(true).build()));
        u.setLogroExhibido(null);
        u.setMuestraListas(true);
        return userRepository.save(u);
    }

    /** Con la cookie de acceso de verdad, como la manda el navegador. */
    private MockHttpServletRequestBuilder conSesion(MockHttpServletRequestBuilder pedido, User quien) {
        return pedido.cookie(new Cookie(SessionCookies.ACCESS_COOKIE, jwt.generateToken(quien.getId(), quien.getEmail())));
    }

    private void resenia(User quien, String id, String comentario) {
        BurgerJoint local = burgerJointRepository.findByPlaceId(id).orElseGet(() -> burgerJointRepository.save(
            BurgerJoint.builder().name(id).address("c").area("Palermo").slug(id).placeId(id).build()));
        ratingRepository.save(Rating.builder().user(quien).burgerJoint(local).score(4).comment(comentario)
            .createdAt(Instant.now()).build());
        recalculoDeXp.recalcular(quien.getId());
    }

    // --- Sin sesión ---

    @Test
    void sinSesionTodoLoDeNivelesYLogrosPideEntrar() throws Exception {
        mvc.perform(get("/api/logros")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/logros/novedades")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/logros/vistos").contentType(MediaType.APPLICATION_JSON).content("{\"codigos\":[]}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/podio")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/profile/logro-exhibido").contentType(MediaType.APPLICATION_JSON).content("{\"logro\":null}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/profile/hamburguesa").contentType(MediaType.APPLICATION_JSON).content("{\"receta\":null}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notificaciones/nuevas")).andExpect(status().isUnauthorized());
    }

    // --- 400 y 409 ---

    @Test
    void unPeriodoInvalidoDelPodioEsUn400() throws Exception {
        mvc.perform(conSesion(get("/api/podio").param("periodo", "siglo"), ana)).andExpect(status().isBadRequest());
    }

    @Test
    void elPodioConPeriodoValidoResponde() throws Exception {
        mvc.perform(conSesion(get("/api/podio").param("periodo", "general"), ana))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.periodo").value("general"));
    }

    @Test
    void exhibirUnLogroNoGanadoEsUn409() throws Exception {
        mvc.perform(conSesion(put("/api/profile/logro-exhibido").contentType(MediaType.APPLICATION_JSON)
                .content("{\"logro\":\"COMBO\"}"), ana))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("Todavía no ganaste ese logro"));
    }

    @Test
    void sacarElLogroExhibidoSiempreSePuede() throws Exception {
        mvc.perform(conSesion(put("/api/profile/logro-exhibido").contentType(MediaType.APPLICATION_JSON)
                .content("{\"logro\":null}"), ana))
            .andExpect(status().isOk());
    }

    @Test
    void unPremioDeLaHamburguesaBloqueadoEsUn409() throws Exception {
        // Sin XP: nivel 1, el bacon (nivel 5) y la cuarta carne (nivel 10) no están desbloqueados.
        mvc.perform(conSesion(put("/api/profile/hamburguesa").contentType(MediaType.APPLICATION_JSON)
                .content("{\"receta\":\"20131010\"}"), ana))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("Todavía no desbloqueaste el bacon"));
        mvc.perform(conSesion(put("/api/profile/hamburguesa").contentType(MediaType.APPLICATION_JSON)
                .content("{\"receta\":\"20134\"}"), ana))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("Todavía no desbloqueaste la cuarta carne"));
    }

    @Test
    void unaHamburguesaSinPremiosSeGuarda() throws Exception {
        mvc.perform(conSesion(put("/api/profile/hamburguesa").contentType(MediaType.APPLICATION_JSON)
                .content("{\"receta\":\"20131\"}"), ana))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hamburguesa").value("20131"));
    }

    @Test
    void laPantallaDeLogrosTraeElNivelDeQuienPide() throws Exception {
        mvc.perform(conSesion(get("/api/logros"), ana))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nivel").value(1))
            .andExpect(jsonPath("$.logros.length()").value(38));
    }

    // --- Bloqueos ---

    @Test
    void elPodioNoMuestraAQuienBloqueasNiAQuienTeBloqueo() throws Exception {
        resenia(ana, "http-1", LARGO);
        resenia(beto, "http-2", LARGO);
        blockRepository.save(Block.builder().blocker(ana).blocked(beto).build());

        // Ana bloqueó a Beto: ella no lo ve...
        mvc.perform(conSesion(get("/api/podio").param("periodo", "general"), ana))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.top[*].username", not(hasItem("http_beto"))))
            .andExpect(jsonPath("$.top[*].username", hasItem("http_ana")));
        // ...y él tampoco la ve a ella, ni siquiera sabe que lo bloquearon.
        mvc.perform(conSesion(get("/api/podio").param("periodo", "general"), beto))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.top[*].username", not(hasItem("http_ana"))))
            .andExpect(jsonPath("$.top[*].username", hasItem("http_beto")));
    }

    @Test
    void unPerfilBloqueadoNoDejaVerSusLogros() throws Exception {
        blockRepository.save(Block.builder().blocker(ana).blocked(beto).build());

        mvc.perform(conSesion(get("/api/usuarios/http_ana"), beto)).andExpect(status().isNotFound());
        mvc.perform(conSesion(get("/api/usuarios/http_beto"), ana)).andExpect(status().isNotFound());
    }

    // --- Privacidad del perfil ajeno ---

    private void ganar(User quien, String... codigos) {
        Instant ahora = Instant.now();
        for (String codigo : codigos) {
            logroRepository.save(new com.hamburguesas.logros.LogroDeUsuario(
                new com.hamburguesas.logros.LogroDeUsuarioId(quien.getId(), codigo), ahora, true, ahora, null, false, null));
        }
    }

    @Test
    void elPerfilAjenoNoMuestraElSliderNiLoQueDependeDeLasListasOcultas() throws Exception {
        ana.setMuestraListas(false);
        userRepository.save(ana);
        ganar(ana, "NIVEL_1", "PRIMERA_RESENIA", "PLANIFICADOR", "PRIMER_TOUR", "GUIA");

        mvc.perform(conSesion(get("/api/usuarios/http_ana"), beto))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.logros", hasItem("PRIMERA_RESENIA")))
            .andExpect(jsonPath("$.logros", not(hasItem("NIVEL_1"))))
            .andExpect(jsonPath("$.logros", not(hasItem("PLANIFICADOR"))))
            .andExpect(jsonPath("$.logros", not(hasItem("PRIMER_TOUR"))))
            .andExpect(jsonPath("$.logros", not(hasItem("GUIA"))));
    }

    @Test
    void conLasListasVisiblesSeVenTodosMenosElSlider() throws Exception {
        ganar(ana, "NIVEL_1", "PLANIFICADOR", "PRIMER_TOUR", "GUIA");

        mvc.perform(conSesion(get("/api/usuarios/http_ana"), beto))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.logros", hasItem("PLANIFICADOR")))
            .andExpect(jsonPath("$.logros", hasItem("PRIMER_TOUR")))
            .andExpect(jsonPath("$.logros", hasItem("GUIA")))
            .andExpect(jsonPath("$.logros", not(hasItem("NIVEL_1"))));
    }

    @Test
    void siElExhibidoDependeDeLasListasOcultasNoSeMuestra() throws Exception {
        ana.setMuestraListas(false);
        userRepository.save(ana);
        ganar(ana, "PLANIFICADOR", "PRIMERA_RESENIA");
        mvc.perform(conSesion(put("/api/profile/logro-exhibido").contentType(MediaType.APPLICATION_JSON)
                .content("{\"logro\":\"PLANIFICADOR\"}"), ana))
            .andExpect(status().isOk());

        mvc.perform(conSesion(get("/api/usuarios/http_ana"), beto))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.logroExhibido").doesNotExist());
        // En el propio perfil sí se ve.
        mvc.perform(conSesion(get("/api/usuarios/http_ana"), ana))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.logroExhibido").value("PLANIFICADOR"));
    }
}
