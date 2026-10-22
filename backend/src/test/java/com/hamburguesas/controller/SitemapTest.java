package com.hamburguesas.controller;

import com.hamburguesas.repository.BurgerJointRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** El mapa del sitio que leen los buscadores. */
class SitemapTest {

    private String sitemap(String sitio, List<Object[]> ids) {
        BurgerJointRepository repo = mock(BurgerJointRepository.class);
        when(repo.idsYSlugs()).thenReturn(ids);
        return new SitemapController(repo, sitio).sitemap().getBody();
    }

    @Test
    void tieneLasPaginasPublicasYLaFichaDeCadaLocal() {
        String xml = sitemap("https://burgometro.com.ar", List.of(new Object[]{9L, null}, new Object[]{42L, "la-birra-bar"}));

        assertThat(xml)
            .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            .contains("<loc>https://burgometro.com.ar/</loc>")
            .contains("<loc>https://burgometro.com.ar/ranking</loc>")
            .contains("<loc>https://burgometro.com.ar/tour</loc>")
            .contains("<loc>https://burgometro.com.ar/privacidad</loc>")
            .contains("<loc>https://burgometro.com.ar/burger-joints/9</loc>")
            .contains("<loc>https://burgometro.com.ar/la-birra-bar</loc>")
            .endsWith("</urlset>\n");
    }

    /** Lo que pide sesión no sirve en un buscador: solo vería la pantalla de entrar. */
    @Test
    void noTieneLoQuePideSesion() {
        String xml = sitemap("https://burgometro.com.ar", List.of());

        assertThat(xml).doesNotContain("/feed", "/profile", "/u/", "/reviews", "/wishlist", "/login");
    }

    @Test
    void unaBarraDeMasEnLaConfiguracionNoDuplicaLasBarras() {
        String xml = sitemap("https://burgometro.com.ar/", java.util.Collections.singletonList(new Object[]{9L, null}));

        assertThat(xml)
            .contains("<loc>https://burgometro.com.ar/burger-joints/9</loc>")
            .doesNotContain(".ar//");
    }
}
