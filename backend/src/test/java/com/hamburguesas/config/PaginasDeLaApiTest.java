package com.hamburguesas.config;

import com.hamburguesas.controller.RatingController;
import com.hamburguesas.service.RatingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Lo que el cliente puede pedir de una página (#201): no más de 50 por vez, y sin
 * elegir el orden.
 *
 * "?size=5000" traía los 1.549 locales en un solo pedido, y repetirlo era la forma más
 * barata de cargar el servidor. Y el "sort" del pedido llegaba hasta la consulta: uno
 * inválido terminaba en un 500 con la excepción entera en el log.
 */
@SpringBootTest
@ActiveProfiles("test")
class PaginasDeLaApiTest {

    @Autowired private PageableHandlerMethodArgumentResolver resolvedorDePaginas;

    private Pageable pedir(String consulta) throws Exception {
        MockHttpServletRequest pedido = new MockHttpServletRequest("GET", "/api/burger-joints/1/ratings");
        pedido.setQueryString(consulta);
        for (String par : consulta.split("&")) {
            String[] partes = par.split("=", 2);
            pedido.addParameter(partes[0], partes[1]);
        }
        MethodParameter parametro = new MethodParameter(
            RatingController.class.getMethod("list", Long.class, Pageable.class), 1);
        return resolvedorDePaginas.resolveArgument(parametro, null, new ServletWebRequest(pedido), null);
    }

    @Test
    void unaPaginaNoPasaDeCincuenta() throws Exception {
        assertThat(pedir("size=5000").getPageSize()).isEqualTo(50);
    }

    @Test
    void lasPaginasDeLaWebSiguenIgual() throws Exception {
        assertThat(pedir("size=18&page=2").getPageSize()).isEqualTo(18);
        assertThat(pedir("size=18&page=2").getPageNumber()).isEqualTo(2);
    }

    /** El orden que mande el cliente no llega a la consulta, sea válido o no. */
    @Test
    void elOrdenDelClienteNoLlegaALaConsulta() {
        RatingService servicio = mock(RatingService.class);
        RatingController controlador = new RatingController(servicio);

        controlador.list(1L, PageRequest.of(0, 20, Sort.by("user.email")));

        ArgumentCaptor<Pageable> pasada = ArgumentCaptor.forClass(Pageable.class);
        verify(servicio).list(eq(1L), any(), pasada.capture());
        assertThat(pasada.getValue().getSort().isUnsorted()).isTrue();
        assertThat(pasada.getValue().getPageSize()).isEqualTo(20);
    }
}
