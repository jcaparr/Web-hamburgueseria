package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * El arreglo de extensiones con las direcciones que llevan marca de versión (#229).
 *
 * Lee el nombre del archivo de la dirección guardada. Con "x.jpg?v=a1b2" tomaba
 * "x.jpg?v=a1b2" como nombre, no encontraba el archivo y no corregía nada.
 */
class PhotoExtensionFixerTest {

    private static final byte[] PNG = new byte[] {
        (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 1, 2, 3
    };

    @Test
    void corrigeUnaFotoConMarcaDeVersion(@TempDir Path dir) throws Exception {
        PlacesProperties properties = new PlacesProperties();
        properties.getPhotos().setDirectory(dir.toString());
        BurgerJointRepository repository = mock(BurgerJointRepository.class);

        // Un PNG guardado con extensión .jpg, y la dirección con su marca.
        Files.write(dir.resolve("ChIJ1.jpg"), PNG);
        BurgerJoint joint = BurgerJoint.builder().id(1L).placeId("ChIJ1")
            .photoUrl("/api/place-photos/ChIJ1.jpg?v=a1b2c3d4e5").build();
        when(repository.findByPhotoUrlIsNotNull()).thenReturn(List.of(joint));

        new PhotoExtensionFixer(repository, new PhotoStorage(properties)).corregir();

        assertThat(dir.resolve("ChIJ1.png")).exists();
        assertThat(joint.getPhotoUrl()).isEqualTo("/api/place-photos/ChIJ1.png");
    }
}
