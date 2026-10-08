package com.hamburguesas.fotos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Una foto sacada con el teléfono vertical queda vertical (#196).
 *
 * La foto de prueba tiene un color en cada cuarto —rojo arriba a la izquierda, verde
 * arriba a la derecha, azul abajo a la izquierda, amarillo abajo a la derecha—, así se
 * ve no solo si cambiaron las medidas sino hacia qué lado giró.
 */
class FotosDerechasTest {

    private static final char ROJO = 'R';
    private static final char VERDE = 'V';
    private static final char AZUL = 'A';
    private static final char AMARILLO = 'Y';

    @TempDir
    Path carpeta;

    private FotosDeResenias fotos;

    @BeforeEach
    void setUp() {
        fotos = new FotosDeResenias();
        ReflectionTestUtils.setField(fotos, "directorio", carpeta.toString());
    }

    /** Apaisada, de 800 × 600, como guarda el sensor una foto sacada en vertical. */
    private static byte[] cuatroColores(int ancho, int alto) throws IOException {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, ancho / 2, alto / 2);
        g.setColor(Color.GREEN);
        g.fillRect(ancho / 2, 0, ancho - ancho / 2, alto / 2);
        g.setColor(Color.BLUE);
        g.fillRect(0, alto / 2, ancho / 2, alto - alto / 2);
        g.setColor(Color.YELLOW);
        g.fillRect(ancho / 2, alto / 2, ancho - ancho / 2, alto - alto / 2);
        g.dispose();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "jpg", salida);
        return salida.toByteArray();
    }

    /**
     * El mismo JPEG con un segmento EXIF que anota la orientación, como el de un celular.
     *
     * Va después del encabezado JFIF que escribe ImageIO, que es donde lo ponen muchas
     * cámaras; la lectura no depende de que sea el primero.
     */
    private static byte[] conOrientacion(byte[] jpeg, int orientacion, boolean intel) {
        byte[] tiff = intel
            ? new byte[] {'I', 'I', 0x2A, 0, 8, 0, 0, 0, 1, 0, 0x12, 0x01, 3, 0, 1, 0, 0, 0,
                (byte) orientacion, 0, 0, 0, 0, 0, 0, 0}
            : new byte[] {'M', 'M', 0, 0x2A, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1,
                0, (byte) orientacion, 0, 0, 0, 0, 0, 0};
        int largo = 2 + 6 + tiff.length;

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        int despuesDelJfif = 4 + (((jpeg[4] & 0xFF) << 8) | (jpeg[5] & 0xFF));
        salida.write(jpeg, 0, despuesDelJfif);
        salida.write(0xFF);
        salida.write(0xE1);
        salida.write(largo >> 8);
        salida.write(largo & 0xFF);
        salida.writeBytes(new byte[] {'E', 'x', 'i', 'f', 0, 0});
        salida.writeBytes(tiff);
        salida.write(jpeg, despuesDelJfif, jpeg.length - despuesDelJfif);
        return salida.toByteArray();
    }

    private BufferedImage guardadaDe(byte[] subida) throws IOException {
        String ruta = fotos.guardar(subida);
        return ImageIO.read(carpeta.resolve(ruta.substring(ruta.lastIndexOf('/') + 1)).toFile());
    }

    /** El color que domina en el centro de cada cuarto, en orden de lectura. */
    private static String cuartos(BufferedImage imagen) {
        int w = imagen.getWidth();
        int h = imagen.getHeight();
        return "" + color(imagen.getRGB(w / 4, h / 4)) + color(imagen.getRGB(3 * w / 4, h / 4))
            + color(imagen.getRGB(w / 4, 3 * h / 4)) + color(imagen.getRGB(3 * w / 4, 3 * h / 4));
    }

    private static char color(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        if (r > 150 && g > 150 && b < 100) return AMARILLO;
        if (r > 150) return ROJO;
        if (g > 150) return VERDE;
        if (b > 150) return AZUL;
        return '?';
    }

    @Test
    void sinExifQuedaComoEsta() throws IOException {
        BufferedImage guardada = guardadaDe(cuatroColores(800, 600));

        assertThat(guardada.getWidth()).isEqualTo(800);
        assertThat(cuartos(guardada)).isEqualTo("RVAY");
    }

    /**
     * Las ocho orientaciones, en los dos órdenes de bytes.
     *
     * Lo esperado es la foto como la vería quien la sacó: con el 6, por ejemplo, lo que
     * estaba abajo a la izquierda en el sensor (azul) queda arriba a la izquierda.
     */
    @ParameterizedTest
    @CsvSource({
        "1, 800, RVAY", "2, 800, VRYA", "3, 800, YAVR", "4, 800, AYRV",
        "5, 600, RAVY", "6, 600, ARYV", "7, 600, YVAR", "8, 600, VYRA",
    })
    void cadaOrientacionQuedaDerecha(int orientacion, int anchoEsperado, String esperado) throws IOException {
        for (boolean intel : new boolean[] {true, false}) {
            BufferedImage guardada = guardadaDe(conOrientacion(cuatroColores(800, 600), orientacion, intel));

            assertThat(guardada.getWidth()).as("ancho, orientación %d", orientacion).isEqualTo(anchoEsperado);
            assertThat(cuartos(guardada)).as("colores, orientación %d", orientacion).isEqualTo(esperado);
        }
    }

    /** Girar y achicar van juntos: una foto grande y de costado sale vertical y a 2048. */
    @Test
    void unaGrandeDeCostadoSaleVerticalYAchicada() throws IOException {
        BufferedImage guardada = guardadaDe(conOrientacion(cuatroColores(4000, 3000), 6, true));

        assertThat(guardada.getWidth()).isEqualTo(1536);
        assertThat(guardada.getHeight()).isEqualTo(2048);
        assertThat(cuartos(guardada)).isEqualTo("ARYV");
    }

    /** El EXIF de la foto guardada se va igual: girarla no es razón para conservarlo. */
    @Test
    void laGuardadaNoLlevaElExif() throws IOException {
        String ruta = fotos.guardar(conOrientacion(cuatroColores(800, 600), 6, true));
        byte[] guardada = java.nio.file.Files.readAllBytes(carpeta.resolve(ruta.substring(ruta.lastIndexOf('/') + 1)));

        assertThat(OrientacionExif.de(guardada)).isEqualTo(OrientacionExif.DERECHA);
        assertThat(new String(guardada, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
    }

    /** Un EXIF roto o mentiroso no rompe nada: la foto queda como está. */
    @Test
    void unExifRotoSeIgnora() throws IOException {
        byte[] conExif = conOrientacion(cuatroColores(800, 600), 6, true);
        // El largo del segmento dice que sigue más allá del final del archivo.
        int app1 = 4 + (((conExif[4] & 0xFF) << 8) | (conExif[5] & 0xFF));
        conExif[app1 + 2] = (byte) 0xFF;
        conExif[app1 + 3] = (byte) 0xFF;

        assertThat(OrientacionExif.de(conExif)).isEqualTo(OrientacionExif.DERECHA);
        assertThat(OrientacionExif.de(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})).isEqualTo(OrientacionExif.DERECHA);
        assertThat(OrientacionExif.de(new byte[0])).isEqualTo(OrientacionExif.DERECHA);
    }

    @Test
    void unValorFueraDeRangoSeIgnora() throws IOException {
        assertThat(OrientacionExif.de(conOrientacion(cuatroColores(800, 600), 9, true)))
            .isEqualTo(OrientacionExif.DERECHA);
    }
}
