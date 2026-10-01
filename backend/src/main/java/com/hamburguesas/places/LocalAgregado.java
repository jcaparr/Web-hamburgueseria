package com.hamburguesas.places;

/**
 * Qué pasó al intentar agregar un local a mano.
 *
 * Lleva el identificador porque es lo que hay que anotar después en la configuración
 * para que la limpieza no lo borre: un local que se agrega a mano suele ser
 * justamente uno que ninguna regla reconoce.
 */
public record LocalAgregado(String placeId, String nombre, String direccion,
                            String zona, String resultado) {}
