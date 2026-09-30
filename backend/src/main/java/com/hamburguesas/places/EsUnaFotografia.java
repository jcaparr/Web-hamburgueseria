package com.hamburguesas.places;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * Si lo que bajamos es una fotografía o un dibujo.
 *
 * Google no dice qué muestra una foto, y de la ficha no se puede deducir: el logo de un
 * local lo sube el local, tiene buen tamaño y suele ser apaisado, así que gana por
 * puntaje como cualquier fotografía buena. La única forma de distinguirlo es mirar los
 * píxeles.
 *
 * Lo que separa un logo de una foto es cuántos colores distintos hay. Un logo o una
 * carta de menú están hechos de planos de color; una cámara, por más pareja que sea la
 * escena, deja miles de tonos.
 *
 * El umbral salió de medir las 422 fotos que teníamos guardadas. Por debajo de 600
 * colores había seis, y las seis eran logos —"La Tercera Burger", "Colo's Burger",
 * "Burger House"— sin un solo falso positivo. Arriba de ahí ya aparecen fotografías
 * reales, aunque sean oscuras: un frente de noche daba 736.
 *
 * No pretende reconocer una hamburguesa. Solo saca de la portada lo que ni siquiera es
 * una foto, que es la mitad barata del problema.
 */
public final class EsUnaFotografia {

    /**
     * Debajo de esta cantidad de colores distintos la imagen es un dibujo.
     *
     * Deliberadamente conservador: entre 600 y 1.400 hay fotos oscuras y afiches
     * mezclados, y equivocarse para el lado de dejar pasar un afiche cuesta menos que
     * descartarle la única foto a un local.
     */
    private static final int COLORES_DE_UNA_FOTO = 600;

    /**
     * Se miran a lo sumo 200 píxeles por lado.
     *
     * Con una foto de 4.800 de ancho, contar todos sus píxeles son veintitrés millones
     * de operaciones para una pregunta que se responde igual con una muestra.
     */
    private static final int LADO_DE_LA_MUESTRA = 200;

    private EsUnaFotografia() {
    }

    /**
     * @return false si la imagen no se puede leer: no es una razón para descartarla, de
     *         eso se encarga la validación de formato que ya existe
     */
    public static boolean pareceUnLogo(byte[] imagen) {
        BufferedImage foto = leer(imagen);
        if (foto == null) {
            return false;
        }
        return coloresDistintos(foto) < COLORES_DE_UNA_FOTO;
    }

    private static BufferedImage leer(byte[] imagen) {
        try {
            return ImageIO.read(new ByteArrayInputStream(imagen));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Cuántos colores distintos hay, agrupando los muy parecidos.
     *
     * Se quedan los cinco bits de más peso de cada canal: dos tonos de rojo que difieren
     * en uno son el mismo color para el ojo, y contarlos por separado haría que el ruido
     * de compresión de un logo lo disfrace de fotografía.
     */
    private static int coloresDistintos(BufferedImage foto) {
        boolean[] vistos = new boolean[32 * 32 * 32];
        int paso = Math.max(1, Math.min(foto.getWidth(), foto.getHeight()) / LADO_DE_LA_MUESTRA);

        int distintos = 0;
        for (int y = 0; y < foto.getHeight(); y += paso) {
            for (int x = 0; x < foto.getWidth(); x += paso) {
                int rgb = foto.getRGB(x, y);
                int indice = ((rgb >> 19) & 31) << 10 | ((rgb >> 11) & 31) << 5 | ((rgb >> 3) & 31);
                if (!vistos[indice]) {
                    vistos[indice] = true;
                    distintos++;
                }
            }
        }
        return distintos;
    }
}
