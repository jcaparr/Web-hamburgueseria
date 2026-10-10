package com.hamburguesas.places;

import java.util.List;

/**
 * Lo que trae un barrido que solo mira (#225): los locales de una zona que todavía no
 * están en la web, con lo que hace falta para revisarlos a mano antes de agregarlos.
 *
 * @param busquedas  cuántas búsquedas se gastaron
 * @param yaEstaban  cuántos de los que trajo Google ya están en la web
 * @param fueraDeLaZona cuántos cayeron fuera de lo pedido: otro país, o fuera del radio
 * @param aviso      si algo quedó a medias: la cuota, o Google que no contestó
 */
public record CandidatosDeZona(String lugar, int busquedas, int yaEstaban, int fueraDeLaZona,
                               List<Candidato> candidatos, String aviso) {

    /**
     * Un local para revisar.
     *
     * @param fotos, opiniones, puntaje, resumen lo que dice Google; null si no se pudo
     *              preguntar porque se acabó la cuota de resúmenes
     * @param loAceptaria si el clasificador del barrido lo dejaría entrar, y con qué prueba:
     *              una opinión más, no la decisión
     * @param mapa  el enlace a la ficha en Google Maps, para mirar fotos y reseñas
     */
    public record Candidato(String placeId, String nombre, String direccion, String zona,
                            String rubro, Integer fotos, Integer opiniones, Double puntaje,
                            String resumen, boolean loAceptaria, String prueba, String mapa) {}

    static CandidatosDeZona salteado(String lugar, String motivo) {
        return new CandidatosDeZona(lugar, 0, 0, 0, List.of(), motivo);
    }
}
