package com.hamburguesas.places;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Las fotos chicas de los puntos del mapa (#395).
 *
 * Un punto es un círculo de 46 px: bajar la foto de 100 a 800 KB por cada uno haría del
 * mapa la pantalla más pesada de la app. Cada miniatura se genera una sola vez, la
 * primera vez que alguien la pide, a partir de la foto que ya está en el disco, y queda
 * guardada al lado en {@code mini/}. No llama a Google.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MiniaturasDeFotos {

    /** Lado del cuadrado: el doble de los 62 px del punto seleccionado, para pantallas con zoom de 2. */
    static final int LADO = 128;

    private final PhotoStorage photoStorage;
    private final PlacesProperties properties;

    /**
     * La miniatura de esa foto, generándola si falta.
     *
     * @return vacío si la foto original no existe o no se pudo leer (por ejemplo un WebP,
     *         que Java no abre): quien llama sirve entonces la foto entera.
     */
    public Optional<Path> de(String nombreDeLaFoto) {
        Optional<Path> original = photoStorage.resolve(nombreDeLaFoto).filter(Files::isRegularFile);
        if (original.isEmpty()) {
            return Optional.empty();
        }

        Path directorio = Path.of(properties.getPhotos().getDirectory()).toAbsolutePath().normalize();
        // Siempre .jpg, venga como venga la original: el nombre cambia, la carpeta es otra.
        String nombre = original.get().getFileName().toString().replaceAll("\\.[^.]+$", "") + ".jpg";
        Path miniatura = directorio.resolve("mini").resolve(nombre);

        // Si la foto original se cambió después (portada nueva), la miniatura vieja no sirve.
        try {
            if (Files.isRegularFile(miniatura)
                && Files.getLastModifiedTime(miniatura).compareTo(Files.getLastModifiedTime(original.get())) >= 0) {
                return Optional.of(miniatura);
            }
            Files.createDirectories(miniatura.getParent());
            return generar(original.get(), miniatura) ? Optional.of(miniatura) : Optional.empty();
        } catch (IOException ex) {
            log.warn("No se pudo preparar la miniatura de {}: {}", nombreDeLaFoto, ex.getMessage());
            return Optional.empty();
        }
    }

    private boolean generar(Path original, Path destino) throws IOException {
        BufferedImage foto = ImageIO.read(original.toFile());
        if (foto == null) {
            return false;
        }

        // Recorte cuadrado desde el centro: el punto es un círculo.
        int lado = Math.min(foto.getWidth(), foto.getHeight());
        BufferedImage cuadrada = foto.getSubimage((foto.getWidth() - lado) / 2, (foto.getHeight() - lado) / 2, lado, lado);

        // Sin canal alfa: JPEG no lo admite, y un PNG con transparencia lo rompería.
        BufferedImage chica = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_RGB);
        var g = chica.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(cuadrada, 0, 0, LADO, LADO, null);
        } finally {
            g.dispose();
        }

        // Se escribe en un temporal y se mueve: dos pedidos a la vez no pueden dejar a uno
        // leyendo una miniatura a medio escribir.
        Path temporal = Files.createTempFile(destino.getParent(), "mini-", ".tmp");
        try (ImageOutputStream salida = ImageIO.createImageOutputStream(Files.newOutputStream(temporal))) {
            ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
            try {
                ImageWriteParam parametros = escritor.getDefaultWriteParam();
                parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parametros.setCompressionQuality(0.8f);
                escritor.setOutput(salida);
                escritor.write(null, new IIOImage(chica, null, null), parametros);
            } finally {
                escritor.dispose();
            }
        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(temporal);
            throw ex;
        }
        Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }
}
