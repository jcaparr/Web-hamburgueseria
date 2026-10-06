package com.hamburguesas.auth;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Las reglas del nombre de usuario, sin base de datos de por medio.
 *
 * Están todas acá y no repartidas entre el formulario y el servicio porque el nombre
 * se valida en tres momentos distintos —al registrarse, al entrar con Google por
 * primera vez y al preguntar si está libre— y en los tres tiene que valer lo mismo.
 */
public final class Usernames {

    public static final int MINIMO = 3;
    public static final int MAXIMO = 20;

    /**
     * Lo que se acepta escribir. Es más cerrado de lo que podría ser a propósito: sin
     * puntos, guiones ni acentos no hay dos nombres que se lean igual y sean distintos,
     * que es la puerta de entrada a hacerse pasar por otro.
     */
    public static final String FORMA = "[A-Za-z0-9_]{" + MINIMO + "," + MAXIMO + "}";

    private static final Pattern VALIDO = Pattern.compile("[a-z0-9_]{" + MINIMO + "," + MAXIMO + "}");

    /**
     * Nombres que nadie puede tomar.
     *
     * No es una lista de malas palabras —eso no se puede ganar— sino de los pocos
     * nombres con los que alguien podría pasar por la app misma o por quien la atiende.
     */
    private static final Set<String> RESERVADOS = Set.of(
        "admin", "administrador", "administracion", "soporte", "ayuda", "oficial",
        "staff", "equipo", "moderador", "moderacion", "root", "sistema", "api",
        "hamburguesas", "hamburgueserias", "burgometro", "null", "undefined");

    private Usernames() {
    }

    /**
     * Cómo se guarda lo que la persona escribió. Todo en minúsculas, porque adentro
     * son el mismo nombre y afuera se leen igual.
     */
    public static String normalizar(String crudo) {
        return crudo == null ? "" : crudo.trim().toLowerCase(Locale.ROOT);
    }

    /** Sobre un nombre ya normalizado. */
    public static boolean tieneFormaValida(String normalizado) {
        return VALIDO.matcher(normalizado).matches();
    }

    /** Sobre un nombre ya normalizado. */
    public static boolean esReservado(String normalizado) {
        return RESERVADOS.contains(normalizado);
    }

    /**
     * Un primer nombre para proponerle a quien entra con Google, sacado de lo que su
     * email tiene antes de la arroba. Puede estar tomado: es un punto de partida para
     * que no tenga que inventar de cero, no una respuesta definitiva.
     */
    public static String baseDesdeEmail(String email) {
        String local = email == null ? "" : email.split("@")[0];
        String limpio = normalizar(local).replaceAll("[^a-z0-9_]", "");
        // Con margen para el número que haya que agregarle si ya está tomado.
        if (limpio.length() > MAXIMO - 5) {
            limpio = limpio.substring(0, MAXIMO - 5);
        }
        return limpio.length() < MINIMO ? "usuario" : limpio;
    }
}
