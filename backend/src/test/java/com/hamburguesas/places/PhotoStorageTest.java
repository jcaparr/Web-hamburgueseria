package com.hamburguesas.places;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre que no se guarde como foto algo que no es una imagen.
 *
 * Nace de un caso real: el endpoint de fotos de Google no devuelve la imagen sino una
 * redirección hacia ella, con un JSON en el cuerpo. El cliente HTTP no la seguía, así
 * que se guardaron 345 archivos de ~700 bytes con ese JSON adentro. En la app se veían
 * rotos, y lo peor es que nadie los iba a reintentar: el local ya figuraba con foto.
 */
class PhotoStorageTest {

    private static final byte[] JPEG_MINIMO = new byte[] {
        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3
    };

    private PhotoStorage storageEn(Path directorio) {
        PlacesProperties properties = new PlacesProperties();
        properties.getPhotos().setDirectory(directorio.toString());
        return new PhotoStorage(properties);
    }

    @Test
    void guardaUnaImagenYDevuelveSuRuta(@TempDir Path dir) {
        String url = storageEn(dir).save("ChIJ123", JPEG_MINIMO);

        assertThat(url).matches("/api/place-photos/ChIJ123\\.jpg\\?v=[0-9a-f]{10}");
        assertThat(dir.resolve("ChIJ123.jpg")).exists();
    }

    /**
     * Una foto nueva para el mismo local es una dirección nueva (#229): el archivo se llama
     * igual y se sirve con 30 días de caché, así que sin esto quien ya la había visto
     * seguía viendo la vieja hasta un mes.
     */
    @Test
    void otraFotoDelMismoLocalTieneOtraDireccion(@TempDir Path dir) {
        PhotoStorage storage = storageEn(dir);
        byte[] otra = JPEG_MINIMO.clone();
        otra[otra.length - 1] = 9;

        assertThat(storage.save("ChIJ123", otra)).isNotEqualTo(storage.save("ChIJ123", JPEG_MINIMO));
    }

    /** Y la misma foto bajada dos veces da la misma: nadie tiene que volver a bajarla. */
    @Test
    void laMismaFotoDosVecesDaLaMismaDireccion(@TempDir Path dir) {
        PhotoStorage storage = storageEn(dir);

        assertThat(storage.save("ChIJ123", JPEG_MINIMO)).isEqualTo(storage.save("ChIJ123", JPEG_MINIMO));
    }

    /**
     * Google manda lo que el local subió, no siempre JPEG: una de cada ocho fotos es
     * PNG. Guardarlas todas como ".jpg" dejó 51 archivos anunciando un formato ajeno.
     */
    @Test
    void guardaCadaFormatoConSuExtension(@TempDir Path dir) {
        byte[] png = new byte[] {
            (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 1, 2, 3
        };

        String url = storageEn(dir).save("ChIJ456", png);

        assertThat(url).matches("/api/place-photos/ChIJ456\\.png\\?v=[0-9a-f]{10}");
        assertThat(dir.resolve("ChIJ456.png")).exists();
        assertThat(dir.resolve("ChIJ456.jpg")).doesNotExist();
    }

    @Test
    void noGuardaUnJsonDisfrazadoDeFoto(@TempDir Path dir) {
        byte[] loQueDevolvioGoogle = """
            {
              "name": "places/ChIJ123/photos/abc",
              "photoUri": "https://lh3.googleusercontent.com/places/abc"
            }
            """.getBytes(StandardCharsets.UTF_8);

        String url = storageEn(dir).save("ChIJ123", loQueDevolvioGoogle);

        assertThat(url).isNull();
        // Que no quede el archivo importa tanto como devolver null: si queda, la app
        // sirve un archivo roto.
        assertThat(dir.resolve("ChIJ123.jpg")).doesNotExist();
    }

    @Test
    void noGuardaUnaRespuestaVaciaNiUnaDemasiadoCorta(@TempDir Path dir) {
        PhotoStorage storage = storageEn(dir);

        assertThat(storage.save("a", new byte[0])).isNull();
        assertThat(storage.save("b", null)).isNull();
        assertThat(storage.save("c", new byte[] { (byte) 0xFF, (byte) 0xD8 })).isNull();
        assertThat(Files.exists(dir.resolve("c.jpg"))).isFalse();
    }

    /**
     * El nombre del archivo lo escribe quien pide la foto —viene de la URL— así que
     * puede intentar salirse del directorio.
     *
     * Con barras no llega a ningún lado: sanitize() las convierte en guión bajo, así que
     * "../secreto.yml" termina siendo el archivo ".._secreto.yml" del propio directorio.
     * El punto en cambio tiene que pasar, porque hace falta para la extensión, y por eso
     * un ".." pelado sobrevive entero y sube un nivel. Eso es lo que corta esta
     * comprobación; antes lo único que lo frenaba era que Spring rechaza esos segmentos
     * antes de llegar acá, una protección que no es de este código.
     */
    @Test
    void unNombreQueSaleDelDirectorioNoDevuelveArchivo(@TempDir Path dir) {
        PhotoStorage storage = storageEn(dir.resolve("fotos"));

        assertThat(storage.resolve("..")).isEmpty();
    }

    /** Con barras no hace falta cortar nada: quedan tapadas antes de mirar la ruta. */
    @Test
    void lasBarrasQuedanTapadasYNoAbrenRutas(@TempDir Path dir) {
        Path fotos = dir.toAbsolutePath().normalize();

        assertThat(storageEn(dir).resolve("../secreto.yml"))
            .contains(fotos.resolve(".._secreto.yml"));
        assertThat(storageEn(dir).resolve("sub/otra.jpg"))
            .contains(fotos.resolve("sub_otra.jpg"));
    }

    /** Y lo normal sigue funcionando, que es lo que la comprobación no puede romper. */
    @Test
    void devuelveElArchivoDeUnNombreComun(@TempDir Path dir) {
        assertThat(storageEn(dir).resolve("ChIJ123.jpg"))
            .contains(dir.toAbsolutePath().normalize().resolve("ChIJ123.jpg"));
    }
}
