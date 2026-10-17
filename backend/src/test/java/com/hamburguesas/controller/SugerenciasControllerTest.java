package com.hamburguesas.controller;

import com.hamburguesas.model.User;
import com.hamburguesas.security.UserPrincipal;
import com.hamburguesas.service.SugerenciasDeLocales;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Que el multipart de "Sugerir un local" (#283) llegue bien armado al servicio. */
class SugerenciasControllerTest {

    private final SugerenciasDeLocales servicio = mock(SugerenciasDeLocales.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SugerenciasController(servicio)).build();

    @BeforeEach
    void iniciarSesion() {
        User user = User.builder().id(7L).username("ana").email("ana@example.com").build();
        var principal = new UserPrincipal(user, List.of());
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void cerrarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void llevaLosCamposYLaFoto() throws Exception {
        byte[] bytes = {1, 2, 3};
        mvc.perform(multipart("/api/sugerencias")
                .file(new MockMultipartFile("foto", "f.jpg", "image/jpeg", bytes))
                .param("nombre", "Burger Uno").param("direccion", "Calle 1").param("email", "a@b.com"))
            .andExpect(status().isCreated());

        verify(servicio).sugerir(eq(7L), any(), eq(bytes));
    }

    @Test
    void sinFotoVaNulo() throws Exception {
        mvc.perform(multipart("/api/sugerencias")
                .param("nombre", "Burger Uno").param("direccion", "Calle 1").param("email", "a@b.com"))
            .andExpect(status().isCreated());

        verify(servicio).sugerir(eq(7L), any(), eq((byte[]) null));
    }

    @Test
    void sinNombreNoLlegaAlServicio() throws Exception {
        mvc.perform(multipart("/api/sugerencias")
                .param("direccion", "Calle 1").param("email", "a@b.com"))
            .andExpect(status().isBadRequest());

        verify(servicio, never()).sugerir(any(), any(), any());
    }
}
