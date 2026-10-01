package com.hamburguesas.places;

import com.hamburguesas.geo.Distancias;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.texto.Texto;

/**
 * Cuándo dos fichas de Google son el mismo local.
 *
 * Google a veces tiene dos fichas para un mismo negocio, con identificadores distintos
 * —una vieja y una nueva, o dos cargadas por gente distinta— y para nosotros son dos
 * locales: lo que evita repetidos es el identificador, y acá son dos. En la lista
 * aparecía "24th Street Burger, Av. Triunvirato 4375" dos veces seguidas, una con foto
 * y la otra sin.
 *
 * Se los considera el mismo cuando tienen el mismo nombre y están en el mismo lugar.
 * Las dos condiciones juntas, porque ninguna alcanza sola: hay locales distintos en la
 * misma dirección —las cocinas que comparten espacio son comunes, y en la base hay tres
 * pares así— y hay cadenas con el mismo nombre en dos barrios.
 */
final class Duplicados {

    /**
     * Cuántos metros pueden separar a dos fichas del mismo negocio.
     *
     * Ciento cincuenta metros es media cuadra larga: alcanza para que dos fichas del
     * mismo local con el punto corrido se reconozcan, y no tanto como para confundir a
     * dos sucursales de la misma cadena, que nunca están tan cerca.
     */
    private static final double METROS = 150;

    private Duplicados() {}

    static boolean sonElMismoLocal(BurgerJoint a, BurgerJoint b) {
        return mismoNombre(a.getName(), b.getName()) && estanEnElMismoLugar(a, b);
    }

    /** Ignora mayúsculas, acentos y puntuación: "Voraz" y "VORAZ!" son el mismo. */
    static boolean mismoNombre(String uno, String otro) {
        String a = Texto.soloLetrasYNumeros(uno);
        return !a.isEmpty() && a.equals(Texto.soloLetrasYNumeros(otro));
    }

    static boolean estanEnElMismoLugar(BurgerJoint a, BurgerJoint b) {
        if (a.getLatitude() == null || a.getLongitude() == null
            || b.getLatitude() == null || b.getLongitude() == null) {
            return false;
        }
        return Distancias.metrosEntre(
            a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude()) <= METROS;
    }

    /**
     * Cuál de las dos fichas conviene conservar.
     *
     * Primero la que tiene foto, que es lo que se ve en la lista; y si las dos están
     * igual, la más vieja, que es la que ya puede estar enlazada desde algún lado.
     * Quién tiene reseñas se decide antes de llamar acá: eso no se borra nunca.
     */
    static BurgerJoint mejorDeLasDos(BurgerJoint a, BurgerJoint b) {
        boolean fotoA = a.getPhotoUrl() != null;
        boolean fotoB = b.getPhotoUrl() != null;
        if (fotoA != fotoB) {
            return fotoA ? a : b;
        }
        return a.getId() <= b.getId() ? a : b;
    }
}
