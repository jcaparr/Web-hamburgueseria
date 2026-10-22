package com.hamburguesas.controller;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.places.PhotoStorage;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.service.FichaDeLocal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** La página que ven WhatsApp, Instagram y X al compartir la ficha de un local. */
class VistaPreviaControllerTest {

    @TempDir
    Path carpeta;

    private final BurgerJointRepository repo = mock(BurgerJointRepository.class);
    private final FichaDeLocal ficha = mock(FichaDeLocal.class);
    private final PhotoStorage fotos = mock(PhotoStorage.class);
    private final VistaPreviaController controlador =
        new VistaPreviaController(repo, ficha, fotos, "https://burgometro.com.ar/");

    private String vistaPrevia(String nombre, String fotoUrl, Double promedio, long cuantas) {
        BurgerJoint local = BurgerJoint.builder().id(7L).name(nombre).area("Palermo")
            .photoUrl(fotoUrl).build();
        when(repo.findById(7L)).thenReturn(Optional.of(local));
        when(ficha.para(any(BurgerJoint.class), any())).thenReturn(new BurgerJointDto(
            7L, null, "p", nombre, "Honduras 5509", "Palermo", fotoUrl, null, null, promedio, cuantas, false));
        return controlador.vistaPrevia(7L).getBody();
    }

    private void fotoGuardada(String nombre, int bytes) throws IOException {
        Path archivo = carpeta.resolve(nombre);
        Files.write(archivo, new byte[bytes]);
        when(fotos.resolve(nombre)).thenReturn(Optional.of(archivo));
    }

    @Test
    void llevaElNombreLaNotaYLaFotoDelLocal() throws IOException {
        fotoGuardada("abc.jpg", 100 * 1024);

        String html = vistaPrevia("Burger Joint", "/api/place-photos/abc.jpg?v=1", 4.5, 12);

        assertThat(html)
            .contains("<meta property=\"og:title\" content=\"Burger Joint\">")
            .contains("content=\"Nota 4,5 de 5 con 12 reseñas en Burgómetro. Palermo.\"")
            .contains("<meta property=\"og:image\" content=\"https://burgometro.com.ar/api/place-photos/abc.jpg?v=1\">")
            .contains("<meta property=\"og:url\" content=\"https://burgometro.com.ar/burger-joints/7\">")
            .contains("<meta name=\"twitter:card\" content=\"summary\">");
    }

    @Test
    void porSlugUsaLaDireccionLinda() {
        BurgerJoint local = BurgerJoint.builder().id(7L).slug("la-birra-bar").name("La Birra Bar")
            .area("Palermo").build();
        when(repo.findBySlug("la-birra-bar")).thenReturn(Optional.of(local));
        when(ficha.para(any(BurgerJoint.class), any())).thenReturn(new BurgerJointDto(
            7L, "la-birra-bar", "p", "La Birra Bar", "Honduras 5509", "Palermo", null, null, null, null, 0L, false));

        String html = controlador.vistaPreviaPorSlug("la-birra-bar").getBody();

        assertThat(html).contains("<meta property=\"og:url\" content=\"https://burgometro.com.ar/la-birra-bar\">");
        assertThat(controlador.vistaPreviaPorSlug("no-existe").getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void sinReseniasNoInventaUnaNota() {
        String html = vistaPrevia("Burger Joint", null, null, 0);

        assertThat(html).contains("Hamburguesería en Palermo.").doesNotContain("Nota ");
    }

    @Test
    void unaReseniaVaEnSingular() {
        assertThat(vistaPrevia("Burger Joint", null, 5.0, 1)).contains("con 1 reseña en");
    }

    @Test
    void sinFotoUsaLaImagenDelSitio() {
        assertThat(vistaPrevia("Burger Joint", null, null, 0))
            .contains("content=\"https://burgometro.com.ar/og.png\"");
    }

    /** WhatsApp no levanta imágenes pesadas y muestra el enlace pelado: mejor el logo. */
    @Test
    void unaFotoPesadaCaeALaImagenDelSitio() throws IOException {
        fotoGuardada("grande.png", (int) VistaPreviaController.PESO_MAXIMO_DE_FOTO + 1);

        assertThat(vistaPrevia("Burger Joint", "/api/place-photos/grande.png?v=1", null, 0))
            .contains("content=\"https://burgometro.com.ar/og.png\"")
            .doesNotContain("grande.png");
    }

    @Test
    void unaFotoQueYaNoEstaEnElDiscoCaeALaImagenDelSitio() {
        when(fotos.resolve(anyString())).thenReturn(Optional.empty());

        assertThat(vistaPrevia("Burger Joint", "/api/place-photos/perdida.jpg", null, 0))
            .contains("content=\"https://burgometro.com.ar/og.png\"");
    }

    /** El nombre lo carga Google o la gente: no puede cerrar la etiqueta ni abrir otra. */
    @Test
    void escapaElNombreDelLocal() {
        String html = vistaPrevia("Rock & \"Burger\" <script>alert(1)</script>", null, null, 0);

        assertThat(html)
            .doesNotContain("<script>")
            .contains("content=\"Rock &amp; &quot;Burger&quot; &lt;script&gt;alert(1)&lt;/script&gt;\"");
    }

    @Test
    void unLocalQueNoExisteEsUn404() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThat(controlador.vistaPrevia(99L).getStatusCode().value()).isEqualTo(404);
    }
}
