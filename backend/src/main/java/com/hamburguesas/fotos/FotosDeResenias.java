package com.hamburguesas.fotos;

import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.places.ImageFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

/**
 * Las fotos que sube la gente con sus reseñas.
 *
 * Es otra cosa que {@link com.hamburguesas.places.PhotoStorage}, que guarda lo que
 * baja de Google. Acá los bytes los elige un desconocido, así que nada de lo que venga
 * con el archivo se toma por cierto: ni su nombre, ni su tipo declarado, ni que sea
 * una imagen.
 *
 * Lo que se guarda nunca es lo que subieron. La imagen se decodifica y se vuelve a
 * escribir como JPEG, y eso hace tres cosas de una: prueba que era una imagen de
 * verdad, borra los metadatos —que en una foto de celular incluyen dónde se sacó— y
 * deja afuera cualquier cosa escondida entre los bytes que el navegador pudiera
 * interpretar de otra manera.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FotosDeResenias {

    /**
     * El lado más largo que se guarda.
     *
     * Una foto de celular viene con varias veces esto. Achicarla es lo que hace que el
     * disco no crezca a diez megas por reseña. A 2048 una foto llena la pantalla de
     * cualquier teléfono con margen de sobra, y se sigue viendo bien si alguien la
     * abre en una notebook.
     */
    private static final int LADO_MAXIMO = 2048;

    /**
     * El lado más corto que se acepta.
     *
     * La tarjeta muestra la foto en un cuadrado, así que el lado corto es el que decide
     * cuán nítida se ve: una foto de 400 de alto estirada a 520 se ve borrosa por más
     * que la otra dimensión sobre. Acá no se agranda nada —eso solo inventa píxeles—
     * así que lo único que se puede hacer es avisarle a quien la sube.
     *
     * Cualquier foto sacada con un teléfono pasa los mil. Las que no llegan suelen ser
     * capturas de pantalla o imágenes bajadas de algún lado.
     */
    private static final int LADO_MINIMO = 600;

    /**
     * Cuántos píxeles se aceptan decodificar.
     *
     * El tamaño del archivo no alcanza como defensa: un PNG de un megabyte puede
     * declarar 30.000 por 30.000, y decodificarlo pide varios gigas de memoria antes
     * de que nadie pueda rechazarlo. Por eso se miran las medidas en el encabezado y
     * se corta ahí, sin leer la imagen.
     */
    private static final long PIXELES_MAXIMOS = 50_000_000L;

    private static final float CALIDAD = 0.9f;

    @Value("${app.fotos-de-resenias.directorio:./data/rating-photos}")
    private String directorio;

    /**
     * @return la ruta pública para guardar en la reseña.
     * @throws ConflictException si eso no era una imagen que podamos guardar.
     */
    public String guardar(byte[] subido) {
        if (subido == null || subido.length == 0) {
            throw new ConflictException("No llegó ninguna foto");
        }
        // La firma se mira primero porque es lo más barato: descarta un PDF o un texto
        // sin que ImageIO tenga que intentar nada.
        if (ImageFormat.of(subido) == null) {
            throw new ConflictException("Eso no es una imagen");
        }

        BufferedImage imagen = decodificar(subido);
        byte[] jpeg = aJpeg(achicar(imagen));

        String nombre = UUID.randomUUID() + ".jpg";
        try {
            Path directory = Paths.get(directorio);
            Files.createDirectories(directory);
            Files.write(directory.resolve(nombre), jpeg);
        } catch (IOException ex) {
            log.warn("No se pudo guardar la foto de una reseña: {}", ex.getMessage());
            throw new ConflictException("No pudimos guardar la foto. Probá de nuevo.");
        }

        return "/api/rating-photos/" + nombre;
    }

    /** Borra el archivo de una foto que ya no está en ninguna reseña. */
    public void borrar(String rutaPublica) {
        archivoDe(rutaPublica).ifPresent(archivo -> {
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException ex) {
                // Que quede un archivo huérfano no es motivo para fallar la operación
                // que lo pidió: la reseña ya no lo apunta, que es lo que importa.
                log.warn("Quedó sin borrar la foto {}: {}", rutaPublica, ex.getMessage());
            }
        });
    }

    /**
     * El archivo de una foto, o vacío si el nombre apunta afuera del directorio.
     *
     * El nombre viene de la URL, así que lo escribe quien pide. Los nombres que
     * generamos son UUID más ".jpg", pero eso no se puede dar por sentado del lado de
     * acá: se resuelve la ruta y se comprueba que caiga adentro.
     */
    public Optional<Path> resolve(String fileName) {
        Path base = Paths.get(directorio).toAbsolutePath().normalize();
        Path archivo = base.resolve(fileName).normalize();
        return archivo.startsWith(base) ? Optional.of(archivo) : Optional.empty();
    }

    private Optional<Path> archivoDe(String rutaPublica) {
        if (rutaPublica == null) {
            return Optional.empty();
        }
        return resolve(rutaPublica.substring(rutaPublica.lastIndexOf('/') + 1));
    }

    /** Mira las medidas antes de decodificar, y recién ahí lee la imagen. */
    private BufferedImage decodificar(byte[] subido) {
        try (ImageInputStream entrada =
                 ImageIO.createImageInputStream(new ByteArrayInputStream(subido))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                // Pasa con WEBP, que la firma reconoce y el JDK no sabe leer.
                throw new ConflictException("No podemos leer ese formato. Probá con JPG o PNG.");
            }

            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada);
                int ancho = lector.getWidth(0);
                int alto = lector.getHeight(0);

                if ((long) ancho * alto > PIXELES_MAXIMOS) {
                    throw new ConflictException("Esa imagen es demasiado grande");
                }
                if (Math.min(ancho, alto) < LADO_MINIMO) {
                    throw new ConflictException(
                        "Esa foto es muy chica (" + ancho + "×" + alto + ") y se vería"
                            + " borrosa. Necesita al menos " + LADO_MINIMO + " px de lado.");
                }
                return lector.read(0);
            } finally {
                lector.dispose();
            }
        } catch (IOException | ArrayIndexOutOfBoundsException ex) {
            // Un archivo cortado o mal formado llega hasta acá, y es del que pide, no
            // nuestro: se le contesta que no sirve en vez de devolver un 500.
            throw new ConflictException("Esa imagen está dañada o incompleta");
        }
    }

    private BufferedImage achicar(BufferedImage original) {
        int lado = Math.max(original.getWidth(), original.getHeight());
        if (lado <= LADO_MAXIMO) {
            return original;
        }

        double escala = (double) LADO_MAXIMO / lado;
        int ancho = Math.max(1, (int) Math.round(original.getWidth() * escala));
        int alto = Math.max(1, (int) Math.round(original.getHeight() * escala));

        BufferedImage achicada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = achicada.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // Sobre blanco: un PNG con transparencia, pasado a JPEG sin esto, queda con
            // el fondo negro.
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, ancho, alto);
            g.drawImage(original, 0, 0, ancho, alto, null);
        } finally {
            g.dispose();
        }
        return achicada;
    }

    private byte[] aJpeg(BufferedImage imagen) {
        BufferedImage sinTransparencia = imagen.getType() == BufferedImage.TYPE_INT_RGB
            ? imagen : sobreBlanco(imagen);

        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            if (!escribirJpeg(sinTransparencia, salida)) {
                throw new ConflictException("No pudimos procesar esa imagen");
            }
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new ConflictException("No pudimos procesar esa imagen");
        }
    }

    private BufferedImage sobreBlanco(BufferedImage imagen) {
        BufferedImage plana = new BufferedImage(
            imagen.getWidth(), imagen.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = plana.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, imagen.getWidth(), imagen.getHeight());
            g.drawImage(imagen, 0, 0, null);
        } finally {
            g.dispose();
        }
        return plana;
    }

    private boolean escribirJpeg(BufferedImage imagen, ByteArrayOutputStream salida)
        throws IOException {
        var escritores = ImageIO.getImageWritersByFormatName("jpg");
        if (!escritores.hasNext()) {
            return false;
        }

        var escritor = escritores.next();
        try (var destino = ImageIO.createImageOutputStream(salida)) {
            escritor.setOutput(destino);
            var parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(CALIDAD);
            escritor.write(null, new javax.imageio.IIOImage(imagen, null, null), parametros);
            return true;
        } finally {
            escritor.dispose();
        }
    }
}
