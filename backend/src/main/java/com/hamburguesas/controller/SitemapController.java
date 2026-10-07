package com.hamburguesas.controller;

import com.hamburguesas.repository.BurgerJointRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * El mapa del sitio para los buscadores: las páginas públicas y la ficha de cada local.
 *
 * Lo arma el backend y no un archivo fijo porque las fichas son una por local, y los
 * locales cambian con cada sincronización. Caddy lo publica en /sitemap.xml, que es
 * donde lo anuncia robots.txt.
 *
 * Solo lo que se ve sin cuenta: el feed, los perfiles y lo guardado piden sesión, y un
 * buscador ahí solo encontraría la pantalla de entrar.
 */
@RestController
public class SitemapController {

    /** Las páginas públicas que no dependen de ningún dato. */
    static final List<String> FIJAS = List.of("/", "/ranking", "/tour", "/terminos", "/privacidad");

    private final BurgerJointRepository burgerJointRepository;
    private final String sitio;

    public SitemapController(BurgerJointRepository burgerJointRepository,
                             @Value("${app.sitio.url}") String sitio) {
        this.burgerJointRepository = burgerJointRepository;
        // Sin la barra final: los caminos ya empiezan con una.
        this.sitio = sitio.replaceAll("/+$", "");
    }

    @GetMapping(value = "/api/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder(64 * 1024)
            .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String camino : FIJAS) {
            url(xml, camino);
        }
        for (Long id : burgerJointRepository.idsDeTodos()) {
            url(xml, "/burger-joints/" + id);
        }
        xml.append("</urlset>\n");

        // Los buscadores lo piden de vez en cuando; una hora alcanza para que un local
        // nuevo aparezca pronto sin armarlo de nuevo en cada pedido.
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body(xml.toString());
    }

    private void url(StringBuilder xml, String camino) {
        xml.append("  <url><loc>").append(escapar(sitio + camino)).append("</loc></url>\n");
    }

    /** La dirección del sitio viene de la configuración: por las dudas, que no rompa el XML. */
    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
