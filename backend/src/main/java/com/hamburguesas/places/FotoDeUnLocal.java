package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;

/**
 * Qué pasó al pedir la portada de un solo local (#224).
 *
 * @param foto   la ruta de la portada que quedó, nueva o la de antes
 * @param huella la de la foto que quedó, para comparar con la elegida a mano
 * @param paga   si se pagó: se avisa siempre, porque el presupuesto es cero
 */
public record FotoDeUnLocal(String placeId, String nombre, String resultado, String foto,
                            String huella, boolean paga) {

    static FotoDeUnLocal sinCambios(BurgerJoint joint, String motivo) {
        return new FotoDeUnLocal(joint.getPlaceId(), joint.getName(), motivo,
            joint.getPhotoUrl(), joint.getPhotoFingerprint(), false);
    }
}
