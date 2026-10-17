package com.hamburguesas.fotos;

import com.hamburguesas.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La foto opcional de "Sugerir un local" (#283): qué se guarda y qué se rechaza. */
class FotosDeSugerenciasTest {

    private FotosDeSugerencias en(Path carpeta) {
        FotosDeSugerencias fotos = new FotosDeSugerencias(new FotosDeResenias());
        ReflectionTestUtils.setField(fotos, "directorio", carpeta.toString());
        return fotos;
    }

    private static byte[] imagen(String formato, int ancho, int alto) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB), formato, salida);
        return salida.toByteArray();
    }

    /** Lo que queda en disco es un JPEG, aunque lo que subieron fuera un PNG. */
    @Test
    void guardaUnJpegAunqueSubanUnPng(@TempDir Path carpeta) throws IOException {
        String nombre = en(carpeta).guardar(imagen("png", 800, 800));

        assertThat(nombre).endsWith(".jpg");
        byte[] guardado = Files.readAllBytes(carpeta.resolve(nombre));
        assertThat(guardado[0]).isEqualTo((byte) 0xFF);
        assertThat(guardado[1]).isEqualTo((byte) 0xD8);
    }

    /** Un texto con nombre de foto no pasa: se mira el contenido, no la extensión. */
    @Test
    void rechazaLoQueNoEsUnaImagen(@TempDir Path carpeta) throws IOException {
        assertThatThrownBy(() -> en(carpeta).guardar("<script>alert(1)</script>".getBytes()))
            .isInstanceOf(ConflictException.class);
        try (var archivos = Files.list(carpeta)) {
            assertThat(archivos).isEmpty();
        }
    }

    @Test
    void rechazaLaFotoMuyChica(@TempDir Path carpeta) {
        assertThatThrownBy(() -> en(carpeta).guardar(imagen("png", 300, 300)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("muy chica");
    }

    /** No se recorta: una foto apaisada grande queda achicada con las mismas proporciones. */
    @Test
    void achicaSinRecortar(@TempDir Path carpeta) throws IOException {
        String nombre = en(carpeta).guardar(imagen("png", 3200, 1800));

        BufferedImage guardada = ImageIO.read(carpeta.resolve(nombre).toFile());
        assertThat(guardada.getWidth()).isEqualTo(1600);
        assertThat(guardada.getHeight()).isEqualTo(900);
    }

    @Test
    void aceptaUnaFotoChicaDeCelularViejo(@TempDir Path carpeta) throws IOException {
        assertThat(en(carpeta).guardar(imagen("png", 500, 450))).endsWith(".jpg");
    }

    @Test
    void borrarSacaElArchivo(@TempDir Path carpeta) throws IOException {
        FotosDeSugerencias fotos = en(carpeta);
        String nombre = fotos.guardar(imagen("png", 800, 800));

        fotos.borrar(nombre);

        assertThat(carpeta.resolve(nombre)).doesNotExist();
    }
}
