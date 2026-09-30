package com.hamburguesas.places;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre el descarte de logos y cartas de menú de la portada de un local.
 *
 * El umbral salió de medir las 422 fotos guardadas: por debajo de 600 colores distintos
 * había seis y las seis eran logos, sin un solo falso positivo. Acá las imágenes se
 * fabrican para que el test no dependa de archivos.
 */
class EsUnaFotografiaTest {

    /** Un logo: dos planos de color y un círculo, que es de lo que están hechos. */
    private byte[] unLogo() throws Exception {
        BufferedImage img = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(220, 50, 40));
        g.fillRect(0, 0, 600, 600);
        g.setColor(Color.WHITE);
        g.fillOval(120, 120, 360, 360);
        g.dispose();
        return aBytes(img);
    }

    /** Una fotografía: miles de tonos, que es lo que deja una cámara. */
    private byte[] unaFotografia() throws Exception {
        BufferedImage img = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        Random azar = new Random(7);
        for (int y = 0; y < 600; y++) {
            for (int x = 0; x < 600; x++) {
                int base = (x + y) / 5;
                img.setRGB(x, y, new Color(
                    acotado(base + azar.nextInt(60)),
                    acotado(base / 2 + azar.nextInt(60)),
                    acotado(azar.nextInt(120))).getRGB());
            }
        }
        return aBytes(img);
    }

    private int acotado(int valor) {
        return Math.min(255, Math.max(0, valor));
    }

    private byte[] aBytes(BufferedImage img) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(img, "png", salida);
        return salida.toByteArray();
    }

    @Test
    void unLogoDeDosColoresNoEsUnaFotografia() throws Exception {
        assertThat(EsUnaFotografia.pareceUnLogo(unLogo())).isTrue();
    }

    @Test
    void unaFotografiaConMilesDeTonosPasa() throws Exception {
        assertThat(EsUnaFotografia.pareceUnLogo(unaFotografia())).isFalse();
    }

    /**
     * Lo que no se puede leer no se descarta acá.
     *
     * De rechazar lo que no es una imagen ya se encarga la validación de formato, y
     * hacerlo dos veces con criterios distintos es la forma de que un día no coincidan.
     */
    @Test
    void loQueNoSePuedeLeerNoSeDescartaPorEsto() {
        assertThat(EsUnaFotografia.pareceUnLogo(new byte[] {1, 2, 3})).isFalse();
        assertThat(EsUnaFotografia.pareceUnLogo(new byte[0])).isFalse();
    }
}
