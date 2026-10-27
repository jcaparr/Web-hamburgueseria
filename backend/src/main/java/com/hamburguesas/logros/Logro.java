package com.hamburguesas.logros;

/**
 * El catálogo de logros (#314): 38, de los que 32 se ven y 6 son secretos.
 *
 * El código es lo que se guarda y no cambia nunca; los nombres y los textos viven en el
 * frontend ({@code utils/logros.ts}).
 *
 * <p>{@link #ajeno} es la regla de qué se pierde: lo que hacés vos se pierde si lo
 * deshacés, y lo que te dan otros (seguidores, reacciones, resultados del podio) queda una
 * vez ganado.
 */
public enum Logro {
    NIVEL_1(Grupo.NIVELES, false, false),
    NIVEL_3(Grupo.NIVELES, false, false),
    NIVEL_5(Grupo.NIVELES, false, false),
    NIVEL_10(Grupo.NIVELES, false, false),
    NIVEL_15(Grupo.NIVELES, false, false),
    NIVEL_20(Grupo.NIVELES, false, false),

    SEGUIDORES_10(Grupo.SEGUIDORES, false, true),
    SEGUIDORES_50(Grupo.SEGUIDORES, false, true),
    SEGUIDORES_100(Grupo.SEGUIDORES, false, true),
    SEGUIDORES_500(Grupo.SEGUIDORES, false, true),
    SEGUIDORES_1000(Grupo.SEGUIDORES, false, true),

    BARRIO_ESQUINA(Grupo.BARRIOS, false, false),
    BARRIOS_5(Grupo.BARRIOS, false, false),
    BARRIOS_15(Grupo.BARRIOS, false, false),
    BARRIOS_25(Grupo.BARRIOS, false, false),
    INTENDENTE(Grupo.BARRIOS, false, false),

    /** Los tres de ranking los entrega el cierre del podio (#318); acá no se calculan. */
    PODIO_SEMANAL(Grupo.RANKING, false, true),
    PRIMERO_DEL_MES(Grupo.RANKING, false, true),
    RACHA_DE_ORO(Grupo.RANKING, false, true),

    PRIMERA_RESENIA(Grupo.ACTIVIDAD, false, false),
    /** Armar la hamburguesa por primera vez: no se pierde al volver a la del nombre. */
    PICASSO(Grupo.ACTIVIDAD, false, true),
    ANSIOSO(Grupo.ACTIVIDAD, false, false),
    HAMBRE_AJENA(Grupo.ACTIVIDAD, false, true),
    PLANIFICADOR(Grupo.ACTIVIDAD, false, false),
    STALKER(Grupo.ACTIVIDAD, false, false),
    PRIMER_TOUR(Grupo.ACTIVIDAD, false, false),
    GUIA(Grupo.ACTIVIDAD, false, false),
    ALBUM(Grupo.ACTIVIDAD, false, false),
    PROMESA(Grupo.ACTIVIDAD, false, false),
    DE_AUTOR(Grupo.ACTIVIDAD, false, false),

    PIONERO(Grupo.ESPECIALES, false, true),
    MENU_COMPLETO(Grupo.ESPECIALES, false, false),

    COMBO(Grupo.SECRETOS, true, true),
    BOCA_EN_BOCA(Grupo.SECRETOS, true, true),
    COMEDIANTE(Grupo.SECRETOS, true, true),
    BAJONERO(Grupo.SECRETOS, true, false),
    PATRIOTA(Grupo.SECRETOS, true, false),
    PALADAR(Grupo.SECRETOS, true, false);

    public enum Grupo { NIVELES, SEGUIDORES, BARRIOS, RANKING, ACTIVIDAD, ESPECIALES, SECRETOS }

    private final Grupo grupo;
    private final boolean secreto;
    private final boolean ajeno;

    Logro(Grupo grupo, boolean secreto, boolean ajeno) {
        this.grupo = grupo;
        this.secreto = secreto;
        this.ajeno = ajeno;
    }

    public Grupo grupo() {
        return grupo;
    }

    public boolean secreto() {
        return secreto;
    }

    /** Queda una vez ganado, aunque deje de cumplirse lo que lo dio. */
    public boolean ajeno() {
        return ajeno;
    }

    /** Los que cuenta el título de la pantalla, "Logros (N de 32)": los secretos no. */
    public static long visibles() {
        long cuantos = 0;
        for (Logro logro : values()) {
            if (!logro.secreto) {
                cuantos++;
            }
        }
        return cuantos;
    }
}
