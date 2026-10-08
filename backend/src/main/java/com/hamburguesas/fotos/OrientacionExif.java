package com.hamburguesas.fotos;

import java.awt.geom.AffineTransform;

/**
 * Cómo hay que girar una foto para verla como la sacaron (#196).
 *
 * El celular no gira los píxeles: los guarda como salen del sensor y anota en el EXIF
 * un número del 1 al 8 que dice qué hacerles. El 1 es "así está bien"; el 6, el más
 * común, es "girala un cuarto a la derecha", que es lo que pasa con cualquier foto
 * sacada con el teléfono vertical.
 *
 * Se lee a mano, sin una biblioteca, porque es un solo número en un lugar fijo: el
 * segmento APP1 del JPEG, y dentro de él la primera tabla del TIFF. Como los bytes los
 * elige quien sube la foto, cualquier cosa rara —un largo que se pasa del archivo, un
 * orden de bytes desconocido, un valor fuera de rango— se toma como "sin orientación".
 * En el peor caso la foto queda como está, que es lo que pasaba antes.
 */
final class OrientacionExif {

    /** Sin nada que hacer: así se ve bien. */
    static final int DERECHA = 1;

    private static final int ETIQUETA_ORIENTACION = 0x0112;

    private OrientacionExif() {}

    /** La orientación anotada en el EXIF de un JPEG, o {@link #DERECHA} si no hay. */
    static int de(byte[] archivo) {
        try {
            return leer(archivo);
        } catch (RuntimeException ex) {
            return DERECHA;
        }
    }

    /** De la 5 a la 8 hay un cuarto de giro, y el ancho y el alto se cambian de lugar. */
    static boolean cambiaLosLados(int orientacion) {
        return orientacion >= 5 && orientacion <= 8;
    }

    /**
     * Lo que hay que hacerle a una imagen de ancho × alto para dejarla derecha.
     *
     * Los cuartos de giro van con quadrantRotate y no con rotate(Math.PI / 2): son
     * exactos, sin el error de redondeo que corre medio píxel la foto entera.
     */
    static AffineTransform transformacion(int orientacion, int ancho, int alto) {
        AffineTransform t = new AffineTransform();
        switch (orientacion) {
            case 2 -> { // espejada
                t.translate(ancho, 0);
                t.scale(-1, 1);
            }
            case 3 -> { // dada vuelta
                t.translate(ancho, alto);
                t.quadrantRotate(2);
            }
            case 4 -> { // espejada de arriba a abajo
                t.translate(0, alto);
                t.scale(1, -1);
            }
            case 5 -> { // espejada y con un cuarto de giro a la izquierda
                t.scale(-1, 1);
                t.quadrantRotate(1);
            }
            case 6 -> { // un cuarto de giro a la derecha: el teléfono vertical
                t.translate(alto, 0);
                t.quadrantRotate(1);
            }
            case 7 -> { // espejada y con un cuarto de giro a la derecha
                t.translate(alto, ancho);
                t.scale(-1, 1);
                t.quadrantRotate(3);
            }
            case 8 -> { // un cuarto de giro a la izquierda
                t.translate(0, ancho);
                t.quadrantRotate(3);
            }
            default -> { }
        }
        return t;
    }

    /** Recorre los segmentos del JPEG hasta el APP1 con el EXIF, o hasta las imágenes. */
    private static int leer(byte[] b) {
        if (b.length < 4 || (b[0] & 0xFF) != 0xFF || (b[1] & 0xFF) != 0xD8) {
            return DERECHA;
        }
        int i = 2;
        while (i + 4 <= b.length) {
            if ((b[i] & 0xFF) != 0xFF) {
                return DERECHA;
            }
            int marcador = b[i + 1] & 0xFF;
            // Después de esto ya son los datos de la imagen: si el EXIF no apareció, no está.
            if (marcador == 0xDA || marcador == 0xD9) {
                return DERECHA;
            }
            int largo = u16(b, i + 2, false);
            if (largo < 2 || i + 2 + largo > b.length) {
                return DERECHA;
            }
            if (marcador == 0xE1 && largo >= 8 && empiezaConExif(b, i + 4)) {
                return enElTiff(b, i + 10, i + 2 + largo);
            }
            i += 2 + largo;
        }
        return DERECHA;
    }

    private static boolean empiezaConExif(byte[] b, int desde) {
        return b[desde] == 'E' && b[desde + 1] == 'x' && b[desde + 2] == 'i' && b[desde + 3] == 'f'
            && b[desde + 4] == 0 && b[desde + 5] == 0;
    }

    /**
     * Busca la orientación en la primera tabla del TIFF que va adentro del APP1.
     *
     * El TIFF dice al principio en qué orden van sus bytes —"II" de menor a mayor, "MM"
     * al revés— y cada cámara elige uno: los dos aparecen en fotos de celular.
     */
    private static int enElTiff(byte[] b, int inicio, int fin) {
        if (inicio + 8 > fin) {
            return DERECHA;
        }
        boolean alReves;
        if (b[inicio] == 'I' && b[inicio + 1] == 'I') {
            alReves = true;
        } else if (b[inicio] == 'M' && b[inicio + 1] == 'M') {
            alReves = false;
        } else {
            return DERECHA;
        }

        long desplazamiento = u32(b, inicio + 4, alReves);
        if (desplazamiento < 8 || inicio + desplazamiento + 2 > fin) {
            return DERECHA;
        }
        int tabla = (int) (inicio + desplazamiento);
        int entradas = u16(b, tabla, alReves);
        for (int k = 0; k < entradas; k++) {
            int entrada = tabla + 2 + k * 12;
            if (entrada + 12 > fin) {
                return DERECHA;
            }
            if (u16(b, entrada, alReves) == ETIQUETA_ORIENTACION) {
                // Es un SHORT, y un valor de dos bytes va al principio del lugar del valor.
                int valor = u16(b, entrada + 8, alReves);
                return valor >= 1 && valor <= 8 ? valor : DERECHA;
            }
        }
        return DERECHA;
    }

    private static int u16(byte[] b, int i, boolean alReves) {
        int uno = b[i] & 0xFF;
        int otro = b[i + 1] & 0xFF;
        return alReves ? (otro << 8) | uno : (uno << 8) | otro;
    }

    private static long u32(byte[] b, int i, boolean alReves) {
        long resultado = 0;
        for (int k = 0; k < 4; k++) {
            int indice = alReves ? i + 3 - k : i + k;
            resultado = (resultado << 8) | (b[indice] & 0xFF);
        }
        return resultado;
    }
}
