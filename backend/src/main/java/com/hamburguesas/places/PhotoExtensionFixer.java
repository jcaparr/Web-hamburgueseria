package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Le pone a cada foto ya guardada la extensión que le corresponde.
 *
 * Durante meses se guardó todo como ".jpg", porque se daba por sentado que Google
 * manda JPEG. Manda lo que el local subió: de 424 fotos, 51 eran PNG con nombre de
 * JPEG, y el servidor las anunciaba como JPEG. El navegador las muestra igual —mira
 * los bytes, no la etiqueta— así que el error era invisible, pero cualquier otra cosa
 * que las lea se guía por lo que decimos nosotros.
 *
 * Corre al arrancar porque los archivos están en el disco del servidor: una migración
 * de base de datos puede arreglar la dirección guardada, pero no renombrar el archivo.
 * Cuando no hay nada que corregir no cuesta nada: son cuatrocientas lecturas de doce
 * bytes y ninguna escritura.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PhotoExtensionFixer {

    private final BurgerJointRepository burgerJointRepository;
    private final PhotoStorage photoStorage;

    @Bean
    ApplicationRunner corregirExtensionesDeFotos() {
        return args -> corregir();
    }

    @Transactional
    void corregir() {
        int corregidas = 0;

        for (var entrada : porArchivo().entrySet()) {
            String nombreActual = entrada.getKey();
            Path archivo = photoStorage.resolve(nombreActual);

            ImageFormat real = formatoDe(archivo);
            if (real == null || real == ImageFormat.porExtension(nombreActual)) {
                continue;
            }

            String nombreNuevo = sinExtension(nombreActual) + "." + real.extension();
            try {
                Files.move(archivo, photoStorage.resolve(nombreNuevo), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                log.warn("No se pudo renombrar {}: {}", nombreActual, ex.getMessage());
                continue;
            }

            for (BurgerJoint joint : entrada.getValue()) {
                joint.setPhotoUrl("/api/place-photos/" + nombreNuevo);
                burgerJointRepository.save(joint);
            }
            corregidas++;
        }

        if (corregidas > 0) {
            log.info("Fotos que tenían la extensión equivocada y se corrigieron: {}", corregidas);
        }
    }

    /**
     * Los locales agrupados por el archivo al que apuntan. Son varios por archivo
     * porque las sucursales de una cadena se prestan la foto entre ellas, y renombrar
     * el archivo sin corregirlas a todas dejaría a las otras sin imagen.
     */
    private Map<String, List<BurgerJoint>> porArchivo() {
        Map<String, List<BurgerJoint>> mapa = new LinkedHashMap<>();
        for (BurgerJoint joint : burgerJointRepository.findByPhotoUrlIsNotNull()) {
            String nombre = nombreDeArchivo(joint.getPhotoUrl());
            if (nombre != null) {
                mapa.computeIfAbsent(nombre, clave -> new ArrayList<>()).add(joint);
            }
        }
        return mapa;
    }

    /** @return el formato real del archivo, o null si no existe o no es una imagen. */
    private static ImageFormat formatoDe(Path archivo) {
        if (!Files.isRegularFile(archivo)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(archivo)) {
            return ImageFormat.of(in.readNBytes(12));
        } catch (IOException ex) {
            log.warn("No se pudo leer {}: {}", archivo.getFileName(), ex.getMessage());
            return null;
        }
    }

    private static String nombreDeArchivo(String photoUrl) {
        int barra = photoUrl.lastIndexOf('/');
        return barra < 0 ? null : photoUrl.substring(barra + 1);
    }

    private static String sinExtension(String fileName) {
        int punto = fileName.lastIndexOf('.');
        return punto < 0 ? fileName : fileName.substring(0, punto);
    }
}
