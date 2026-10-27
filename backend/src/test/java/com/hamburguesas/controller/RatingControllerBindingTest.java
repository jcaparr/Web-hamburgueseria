package com.hamburguesas.controller;

import com.hamburguesas.dto.RatingRequest;
import com.hamburguesas.dto.RatingResponse;
import com.hamburguesas.model.User;
import com.hamburguesas.security.UserPrincipal;
import com.hamburguesas.service.RatingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cómo llegan los campos nuevos de la reseña (#336) desde el formulario: el día como
 * "2026-10-12", las notas del 1 al 5 y lo que no viene como nulo.
 */
class RatingControllerBindingTest {

    private RatingService servicio;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        servicio = mock(RatingService.class);
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new RatingController(servicio)).setValidator(validador).build();

        var usuario = new UserPrincipal(User.builder().id(1L).username("ana").email("a@example.com").build(), Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder pedido() {
        return multipart("/api/burger-joints/5/ratings").contentType(MediaType.MULTIPART_FORM_DATA);
    }

    @Test
    void elDiaYLasNotasLlegan() throws Exception {
        mvc.perform(pedido().param("score", "4").param("visitDate", "2026-10-12")
                .param("scoreLocal", "5").param("scoreAtencion", "3"))
            .andExpect(status().isCreated());

        ArgumentCaptor<RatingRequest> pedido = ArgumentCaptor.forClass(RatingRequest.class);
        verify(servicio).rate(eq(1L), eq(5L), pedido.capture(), any());
        assertThat(pedido.getValue().visitDate()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(pedido.getValue().scoreLocal()).isEqualTo(5);
        assertThat(pedido.getValue().scoreAtencion()).isEqualTo(3);
        assertThat(pedido.getValue().scorePrecio()).isNull();
    }

    @Test
    void unCampoVacioEsNulo() throws Exception {
        mvc.perform(pedido().param("score", "4").param("visitDate", "").param("scorePrecio", ""))
            .andExpect(status().isCreated());

        ArgumentCaptor<RatingRequest> pedido = ArgumentCaptor.forClass(RatingRequest.class);
        verify(servicio).rate(eq(1L), eq(5L), pedido.capture(), any());
        assertThat(pedido.getValue().visitDate()).isNull();
        assertThat(pedido.getValue().scorePrecio()).isNull();
    }

    @Test
    void laReseniaPropiaSeDevuelveOEs204() throws Exception {
        mvc.perform(get("/api/burger-joints/5/ratings/mine")).andExpect(status().isNoContent());

        when(servicio.mia(1L, 5L)).thenReturn(Optional.of(mock(RatingResponse.class)));
        mvc.perform(get("/api/burger-joints/5/ratings/mine")).andExpect(status().isOk());
    }

    @Test
    void sinSesionNoHayReseniaPropia() {
        SecurityContextHolder.clearContext();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mvc.perform(get("/api/burger-joints/5/ratings/mine")))
            .hasRootCauseInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(servicio, never()).mia(any(), any());
    }

    @Test
    void unaNotaFueraDeRangoEsUnPedidoMalo() throws Exception {
        mvc.perform(pedido().param("score", "4").param("scoreLocal", "6")).andExpect(status().isBadRequest());
        mvc.perform(pedido().param("score", "4").param("scorePrecio", "0")).andExpect(status().isBadRequest());
        verify(servicio, never()).rate(any(), any(), any(), any());
    }
}
