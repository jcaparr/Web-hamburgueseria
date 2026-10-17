package com.hamburguesas.fotos;

import com.hamburguesas.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * La foto que acompaña una sugerencia de local (#283).
 *
 * Pasa por el mismo procesado que las de las reseñas —se prueba que sea una imagen de
 * verdad, se reescribe como JPEG y se borran los metadatos— pero se guarda en otra
 * carpeta y no tiene ninguna ruta que la sirva: nadie la ve en la web hasta que Juan la
 * revisa y la sube él al agregar el local. Por eso no hay un controlador al lado.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FotosDeSugerencias {

    /**
     * Las fotos de los locales que vienen de Google miden 800 de ancho y la web las
     * muestra recortadas al marco con CSS. Acá pasa lo mismo: no se recorta nada ni se
     * le pide a nadie que lo haga, la foto conserva sus proporciones y queda en 1600 como
     * mucho (el doble, para pantallas nítidas). El mínimo es más bajo que en las reseñas
     * porque un local chico puede tener solo una foto del celular o de Instagram.
     */
    private static final int LADO_MAXIMO = 1600;
    private static final int LADO_MINIMO = 400;

    private final FotosDeResenias procesador;

    @Value("${app.sugerencias.directorio-de-fotos:./data/sugerencias-fotos}")
    private String directorio;

    /**
     * @return el nombre del archivo guardado.
     * @throws ConflictException si eso no era una imagen que podamos guardar.
     */
    public String guardar(byte[] subido) {
        byte[] jpeg = procesador.aJpegSeguro(subido, LADO_MAXIMO, LADO_MINIMO);

        String nombre = UUID.randomUUID() + ".jpg";
        try {
            Path carpeta = Paths.get(directorio);
            Files.createDirectories(carpeta);
            Files.write(carpeta.resolve(nombre), jpeg);
        } catch (IOException ex) {
            log.warn("No se pudo guardar la foto de una sugerencia: {}", ex.getMessage());
            throw new ConflictException("No pudimos guardar la foto. Probá de nuevo.");
        }
        return nombre;
    }

    /** Borra la foto de una sugerencia que no llegó a guardarse. */
    public void borrar(String nombre) {
        try {
            Path carpeta = Paths.get(directorio).toAbsolutePath().normalize();
            Files.deleteIfExists(carpeta.resolve(nombre).normalize());
        } catch (IOException ex) {
            log.warn("Quedó sin borrar la foto {} de una sugerencia: {}", nombre, ex.getMessage());
        }
    }
}
