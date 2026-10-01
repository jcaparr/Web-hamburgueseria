package com.hamburguesas.places;

/**
 * Qué se decidió sobre un local y con qué prueba.
 *
 * La prueba se guarda además de la decisión porque sin ella no hay forma de revisar un
 * error: "no entró" no se puede discutir, "no entró porque el nombre dice pizzería" sí.
 */
public record Veredicto(boolean vendeHamburguesas, Prueba prueba) {

    public enum Prueba {
        /** Google lo tiene entre sus rubros, aunque no sea el principal. */
        RUBRO_DE_GOOGLE,
        /** El nombre lo dice: "algo Burger", "Hamburguesas algo". */
        EL_NOMBRE_LO_DICE,
        /** Es sucursal de una cadena que Google sí reconoce como hamburguesería. */
        SUCURSAL_DE_UNA_CADENA,
        /** El resumen de reseñas de Google habla de hamburguesas. */
        LO_DICEN_LAS_RESENIAS,
        /** Anotado a mano en la configuración. */
        A_MANO,

        /** Se dedica a otra cosa: kiosco, pizzería, panadería, parrilla. */
        SE_DEDICA_A_OTRA_COSA,
        /** El resumen de reseñas habla de lo que vende, y no son hamburguesas. */
        LAS_RESENIAS_HABLAN_DE_OTRA_COSA,
        /** Google no tiene ni una foto del local. */
        SIN_FOTOS_EN_GOOGLE,
        /** Ninguna de sus fotos muestra una hamburguesa. */
        NINGUNA_FOTO_ES_DE_HAMBURGUESA,
        /** Nada dijo que venda hamburguesas. */
        SIN_PRUEBAS,

        /**
         * No se pudo preguntar: se acabó la cuota del resumen de reseñas, o Google no
         * contestó.
         *
         * No es lo mismo que no encontrar pruebas. Que no hayamos podido mirar es un
         * límite nuestro o una falla de Google, y nunca puede terminar en borrarle el
         * local a alguien.
         */
        NO_SE_PUDO_PREGUNTAR
    }

    static Veredicto si(Prueba prueba) {
        return new Veredicto(true, prueba);
    }

    static Veredicto no(Prueba prueba) {
        return new Veredicto(false, prueba);
    }
}
