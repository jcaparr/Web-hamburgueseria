package com.hamburguesas.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cómo sale una página en el JSON: la lista en "content" y sus datos aparte, en "page".
 *
 * Es el formato estable de Spring Data. Antes se serializaba el PageImpl tal cual, en
 * un formato que Spring no garantiza y que una actualización podía cambiar, rompiendo
 * la paginación de Explorar sin ningún error (#103). El frontend lee "page", así que
 * este test es lo que avisa si la configuración se pierde.
 */
@SpringBootTest
@ActiveProfiles("test")
class FormatoDeLasPaginasTest {

    @Autowired private JsonMapper jsonMapper;

    @Test
    void losDatosDeLaPaginaVanAparteEnPage() {
        Page<String> pagina = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 7);

        JsonNode json = jsonMapper.readTree(jsonMapper.writeValueAsString(pagina));

        assertThat(json.get("content").size()).isEqualTo(2);
        assertThat(json.get("page").get("size").asInt()).isEqualTo(2);
        assertThat(json.get("page").get("number").asInt()).isEqualTo(1);
        assertThat(json.get("page").get("totalElements").asLong()).isEqualTo(7);
        assertThat(json.get("page").get("totalPages").asInt()).isEqualTo(4);
        assertThat(json.has("totalElements")).isFalse();
    }
}
