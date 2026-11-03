package com.hamburguesas.places;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Las fotos chicas del mapa (#395): cuadradas, livianas y generadas una sola vez. */
class MiniaturasDeFotosTest {

    private MiniaturasDeFotos miniaturasEn(Path directorio) {
        PlacesProperties properties = new PlacesProperties();
        properties.getPhotos().setDirectory(directorio.toString());
        return new MiniaturasDeFotos(new PhotoStorage(properties), properties);
    }

    private void foto(Path dir, String nombre, int ancho, int alto, String formato) throws IOException {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(imagen, formato, dir.resolve(nombre).toFile());
    }

    @Test
    void recortaCuadradoYLaDejaDe128(@TempDir Path dir) throws IOException {
        foto(dir, "a.jpg", 800, 600, "jpg");

        Path mini = miniaturasEn(dir).de("a.jpg").orElseThrow();

        BufferedImage chica = ImageIO.read(mini.toFile());
        assertThat(chica.getWidth()).isEqualTo(MiniaturasDeFotos.LADO);
        assertThat(chica.getHeight()).isEqualTo(MiniaturasDeFotos.LADO);
        assertThat(mini).isEqualTo(dir.toAbsolutePath().normalize().resolve("mini").resolve("a.jpg"));
        assertThat(Files.size(mini)).isLessThan(20_000);
    }

    @Test
    void unaFotoPngTambienTieneMiniaturaJpg(@TempDir Path dir) throws IOException {
        foto(dir, "b.png", 300, 300, "png");

        Path mini = miniaturasEn(dir).de("b.png").orElseThrow();

        assertThat(mini.getFileName().toString()).isEqualTo("b.jpg");
    }

    @Test
    void laGeneraUnaSolaVez(@TempDir Path dir) throws IOException {
        foto(dir, "c.jpg", 400, 400, "jpg");
        MiniaturasDeFotos miniaturas = miniaturasEn(dir);

        Path primera = miniaturas.de("c.jpg").orElseThrow();
        // Se le marca una fecha futura: si la volviera a generar, la fecha cambiaría.
        Files.setLastModifiedTime(primera, FileTime.fromMillis(System.currentTimeMillis() + 60_000));
        FileTime antes = Files.getLastModifiedTime(primera);

        miniaturas.de("c.jpg");

        assertThat(Files.getLastModifiedTime(primera)).isEqualTo(antes);
    }

    @Test
    void siCambiaLaFotoOriginalSeRegenera(@TempDir Path dir) throws IOException {
        foto(dir, "d.jpg", 400, 400, "jpg");
        MiniaturasDeFotos miniaturas = miniaturasEn(dir);
        Path mini = miniaturas.de("d.jpg").orElseThrow();
        Files.setLastModifiedTime(mini, FileTime.fromMillis(1_000));
        long viejo = Files.getLastModifiedTime(mini).toMillis();

        miniaturas.de("d.jpg");

        assertThat(Files.getLastModifiedTime(mini).toMillis()).isGreaterThan(viejo);
    }

    @Test
    void sinFotoOSinImagenLegibleNoHayMiniatura(@TempDir Path dir) throws IOException {
        Files.write(dir.resolve("roto.jpg"), "no soy una imagen".getBytes(StandardCharsets.UTF_8));

        assertThat(miniaturasEn(dir).de("no-existe.jpg")).isEmpty();
        assertThat(miniaturasEn(dir).de("roto.jpg")).isEmpty();
        // Un nombre que apunta afuera del directorio tampoco se resuelve a nada.
        assertThat(miniaturasEn(dir).de("../x.jpg")).isEmpty();
    }
}
