package com.hamburguesas.places;

/**
 * Qué formato de imagen es un archivo, según sus primeros bytes.
 *
 * Google no dice qué manda: el endpoint de la foto devuelve lo que el local subió, y
 * eso es JPEG la mayoría de las veces pero también PNG. Guardábamos todo como ".jpg" y
 * lo servíamos como JPEG, así que 51 de las 424 fotos viajaban con el tipo equivocado.
 * El navegador las muestra igual —mira los bytes, no la etiqueta— pero cualquier otra
 * cosa que las lea se guía por lo que decimos nosotros.
 *
 * La firma son los primeros bytes, que en todos estos formatos es fija. No valida que
 * la imagen esté entera; alcanza para distinguir una foto de un JSON de error, que es
 * de lo que se trata.
 */
public enum ImageFormat {

    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    GIF("gif", "image/gif"),
    WEBP("webp", "image/webp");

    private final String extension;
    private final String mediaType;

    ImageFormat(String extension, String mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String extension() {
        return extension;
    }

    public String mediaType() {
        return mediaType;
    }

    /** @return el formato, o null si esos bytes no son ninguna imagen conocida. */
    public static ImageFormat of(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return null;
        }
        if (esByte(bytes, 0, 0xFF) && esByte(bytes, 1, 0xD8)) {
            return JPEG;
        }
        if (esByte(bytes, 0, 0x89) && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return PNG;
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return GIF;
        }
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
            && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return WEBP;
        }
        return null;
    }

    /** @return el formato que corresponde a una extensión, o JPEG si no se reconoce. */
    public static ImageFormat porExtension(String fileName) {
        int punto = fileName.lastIndexOf('.');
        String extension = punto < 0 ? "" : fileName.substring(punto + 1).toLowerCase();
        for (ImageFormat formato : values()) {
            if (formato.extension.equals(extension)) {
                return formato;
            }
        }
        return JPEG;
    }

    private static boolean esByte(byte[] bytes, int posicion, int valor) {
        return (bytes[posicion] & 0xFF) == valor;
    }
}
