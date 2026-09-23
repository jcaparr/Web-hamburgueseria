package com.hamburguesas.places;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre que el formato salga de los bytes y no del nombre.
 *
 * Guardar todo como ".jpg" dejó 51 de 424 fotos anunciando un formato que no era el
 * suyo. El navegador las mostraba igual, así que el error no se veía por ningún lado.
 */
class ImageFormatTest {

    private static byte[] conFirma(int... firma) {
        byte[] bytes = new byte[12];
        for (int i = 0; i < firma.length; i++) {
            bytes[i] = (byte) firma[i];
        }
        return bytes;
    }

    @Test
    void reconoceLosFormatosQueDevuelveGoogle() {
        assertThat(ImageFormat.of(conFirma(0xFF, 0xD8, 0xFF, 0xE0))).isEqualTo(ImageFormat.JPEG);
        assertThat(ImageFormat.of(conFirma(0x89, 'P', 'N', 'G'))).isEqualTo(ImageFormat.PNG);
        assertThat(ImageFormat.of(conFirma('G', 'I', 'F', '8'))).isEqualTo(ImageFormat.GIF);
    }

    @Test
    void reconoceWebpQueLlevaSuFirmaMasAdelante() {
        byte[] webp = conFirma('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P');
        assertThat(ImageFormat.of(webp)).isEqualTo(ImageFormat.WEBP);
    }

    /**
     * Lo que no es una imagen no se guarda. Cuando el cliente no seguía redirecciones,
     * Google contestaba un JSON y se guardaban 345 archivos de 700 bytes que la app
     * mostraba rotos, sin que nadie los reintentara: el local ya figuraba "con foto".
     */
    @Test
    void loQueNoEsUnaImagenNoTieneFormato() {
        assertThat(ImageFormat.of("{\"error\": 404}".getBytes())).isNull();
        assertThat(ImageFormat.of(new byte[0])).isNull();
        assertThat(ImageFormat.of(null)).isNull();
    }

    /** Un archivo de menos de doce bytes no alcanza ni para tener firma. */
    @Test
    void unArchivoDemasiadoCortoNoTieneFormato() {
        assertThat(ImageFormat.of(new byte[] { (byte) 0xFF, (byte) 0xD8 })).isNull();
    }

    @Test
    void cadaFormatoSeSirveConSuTipo() {
        assertThat(ImageFormat.PNG.mediaType()).isEqualTo("image/png");
        assertThat(ImageFormat.JPEG.mediaType()).isEqualTo("image/jpeg");
        assertThat(ImageFormat.WEBP.mediaType()).isEqualTo("image/webp");
    }

    @Test
    void elTipoParaServirSaleDeLaExtension() {
        assertThat(ImageFormat.porExtension("ChIJ123.png")).isEqualTo(ImageFormat.PNG);
        assertThat(ImageFormat.porExtension("ChIJ123.jpg")).isEqualTo(ImageFormat.JPEG);
    }

    /**
     * Las fotos viejas se llaman ".jpg" y la mayoría lo son. Ante un nombre raro conviene
     * decir JPEG antes que fallar: es lo que eran casi todas.
     */
    @Test
    void anteUnNombreSinExtensionConocidaQuedaJpeg() {
        assertThat(ImageFormat.porExtension("ChIJ123")).isEqualTo(ImageFormat.JPEG);
        assertThat(ImageFormat.porExtension("ChIJ123.bin")).isEqualTo(ImageFormat.JPEG);
    }
}
