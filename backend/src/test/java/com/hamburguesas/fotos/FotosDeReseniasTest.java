package com.hamburguesas.fotos;

import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.places.ImageFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Las fotos que sube la gente.
 *
 * Son bytes que eligió un desconocido, así que lo que se afirma acá es sobre todo qué
 * no llega a disco: lo que no es una imagen, lo que está roto, y lo que viene con
 * metadatos que no son asunto nuestro.
 */
class FotosDeReseniasTest {

    @TempDir
    Path carpeta;

    private FotosDeResenias fotos;

    @BeforeEach
    void setUp() {
        fotos = new FotosDeResenias();
        ReflectionTestUtils.setField(fotos, "directorio", carpeta.toString());
    }

    private byte[] unaImagen(String formato, int ancho, int alto) throws IOException {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, ancho, alto);
        g.dispose();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, formato, salida);
        return salida.toByteArray();
    }

    private Path archivoDe(String rutaPublica) {
        return carpeta.resolve(rutaPublica.substring(rutaPublica.lastIndexOf('/') + 1));
    }

    private BufferedImage leer(String rutaPublica) throws IOException {
        return ImageIO.read(archivoDe(rutaPublica).toFile());
    }

    @Test
    void guardaUnaFotoYDevuelveSuRutaPublica() throws IOException {
        String ruta = fotos.guardar(unaImagen("jpg", 800, 600));

        assertThat(ruta).startsWith("/api/rating-photos/").endsWith(".jpg");
        assertThat(archivoDe(ruta)).exists();
    }

    /**
     * El nombre lo ponemos nosotros, no quien sube.
     *
     * Dos personas subiendo "foto.jpg" no pueden pisarse, y nadie puede elegir dónde ni
     * con qué nombre queda su archivo.
     */
    @Test
    void dosFotosIgualesNoSePisan() throws IOException {
        byte[] misma = unaImagen("jpg", 100, 100);

        assertThat(fotos.guardar(misma)).isNotEqualTo(fotos.guardar(misma));
        assertThat(carpeta.toFile().list()).hasSize(2);
    }

    /** Lo que no es una imagen no llega a disco. */
    @Test
    void loQueNoEsUnaImagenSeRechaza() {
        byte[] texto = "esto es un archivo de texto cualquiera".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> fotos.guardar(texto))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("no es una imagen");

        assertThat(carpeta.toFile().list()).isEmpty();
    }

    /**
     * Ni lo que empieza como una imagen y sigue siendo otra cosa.
     *
     * La firma sola no alcanza: cualquiera puede escribir los dos bytes de un JPEG
     * adelante de lo que quiera. Lo que descarta esto es que haya que decodificarla.
     */
    @Test
    void loQueSoloSeDisfrazaDeImagenTambien() {
        byte[] disfrazado = new byte[64];
        disfrazado[0] = (byte) 0xFF;
        disfrazado[1] = (byte) 0xD8;
        System.arraycopy("<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8), 0,
            disfrazado, 2, 25);

        assertThatThrownBy(() -> fotos.guardar(disfrazado)).isInstanceOf(ConflictException.class);

        assertThat(carpeta.toFile().list()).isEmpty();
    }

    @Test
    void sinNadaNoSeGuardaNada() {
        assertThatThrownBy(() -> fotos.guardar(new byte[0]))
            .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> fotos.guardar(null))
            .isInstanceOf(ConflictException.class);
    }

    /**
     * Lo guardado es siempre JPEG, aunque hayan subido un PNG.
     *
     * Reescribirla es lo que borra los metadatos —que en una foto de celular incluyen
     * dónde se sacó— y lo que deja servirla con un tipo fijo sin tener que adivinar.
     */
    @Test
    void unPngSeGuardaComoJpeg() throws IOException {
        String ruta = fotos.guardar(unaImagen("png", 400, 300));

        byte[] guardado = Files.readAllBytes(archivoDe(ruta));
        assertThat(ImageFormat.of(guardado)).isEqualTo(ImageFormat.JPEG);
    }

    /** Una foto grande se achica: el disco no tiene por qué crecer a diez megas por reseña. */
    @Test
    void unaFotoGrandeSeAchica() throws IOException {
        String ruta = fotos.guardar(unaImagen("jpg", 4000, 3000));

        BufferedImage guardada = leer(ruta);
        assertThat(Math.max(guardada.getWidth(), guardada.getHeight())).isEqualTo(1600);
        // Y sin deformarla: 4000x3000 es 4:3, y tiene que seguir siéndolo.
        assertThat(guardada.getHeight()).isEqualTo(1200);
    }

    /** Una que ya entra se deja como está, en vez de agrandarla. */
    @Test
    void unaFotoChicaNoSeAgranda() throws IOException {
        String ruta = fotos.guardar(unaImagen("jpg", 300, 200));

        BufferedImage guardada = leer(ruta);
        assertThat(guardada.getWidth()).isEqualTo(300);
        assertThat(guardada.getHeight()).isEqualTo(200);
    }

    /**
     * Un JPEG cortado a la mitad se guarda con lo que se pudo leer, no se rechaza.
     *
     * El lector del JDK es tolerante y decodifica lo que llegó, así que el archivo
     * entra. Está bien que así sea —una foto a la que le faltan las últimas filas
     * sigue siendo su foto— y lo que importa se cumple igual: lo que queda en disco es
     * un JPEG que escribimos nosotros, no los bytes que mandaron.
     */
    @Test
    void unJpegCortadoSeGuardaConLoQueSePudoLeer() throws IOException {
        byte[] entera = unaImagen("jpg", 800, 600);
        byte[] cortada = new byte[entera.length / 3];
        System.arraycopy(entera, 0, cortada, 0, cortada.length);

        String ruta = fotos.guardar(cortada);

        byte[] guardado = Files.readAllBytes(archivoDe(ruta));
        assertThat(ImageFormat.of(guardado)).isEqualTo(ImageFormat.JPEG);
        assertThat(guardado).isNotEqualTo(cortada);
        assertThat(leer(ruta).getWidth()).isEqualTo(800);
    }

    /** Un archivo que ni siquiera se puede empezar a leer sí se rechaza. */
    @Test
    void unaImagenIlegibleSeRechazaSinRomper() throws IOException {
        byte[] entera = unaImagen("png", 800, 600);
        byte[] apenasLaFirma = new byte[20];
        System.arraycopy(entera, 0, apenasLaFirma, 0, 20);

        assertThatThrownBy(() -> fotos.guardar(apenasLaFirma))
            .isInstanceOf(ConflictException.class);

        assertThat(carpeta.toFile().list()).isEmpty();
    }

    @Test
    void borrarSacaElArchivoDeDisco() throws IOException {
        String ruta = fotos.guardar(unaImagen("jpg", 100, 100));

        fotos.borrar(ruta);

        assertThat(archivoDe(ruta)).doesNotExist();
    }

    /** Borrar algo que ya no está no es un error: el objetivo ya se cumplió. */
    @Test
    void borrarDosVecesNoFalla() throws IOException {
        String ruta = fotos.guardar(unaImagen("jpg", 100, 100));

        fotos.borrar(ruta);
        fotos.borrar(ruta);
        fotos.borrar(null);
    }

    /**
     * El nombre viene de la URL, así que lo escribe quien pide.
     *
     * Los que generamos son UUID más ".jpg", pero de este lado eso no se puede dar por
     * sentado: lo que apunte afuera del directorio no se resuelve.
     */
    @Test
    void unNombreQueApuntaAfueraNoSeResuelve() {
        assertThat(fotos.resolve("../../../etc/passwd")).isEmpty();
        assertThat(fotos.resolve("..")).isEmpty();
    }

    @Test
    void unNombreNormalSiSeResuelve() {
        assertThat(fotos.resolve("algo.jpg")).isPresent();
    }
}
