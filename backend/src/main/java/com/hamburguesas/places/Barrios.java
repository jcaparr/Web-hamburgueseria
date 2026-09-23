package com.hamburguesas.places;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * En qué barrio de la Ciudad cae un punto del mapa.
 *
 * Hasta acá el barrio de un local era el de la búsqueda que lo encontró: si aparecía
 * en "hamburguesería en Palermo", quedaba en Palermo. Google no respeta ese límite
 * —devuelve lo que le parece cerca— así que 143 de los 438 locales mostraban un barrio
 * que no tenía nada que ver con su dirección: Fitz Roy 1731 figuraba en Recoleta y
 * Honduras 4733 en Villa Real.
 *
 * Peor todavía: como la búsqueda es texto libre, "hamburguesería en San Nicolás" traía
 * locales de San Nicolás de los Arroyos, "Versalles" uno de Colombia y "San Cristóbal"
 * uno de México. Ninguno tiene coordenadas dentro de la Ciudad, así que este mismo
 * archivo los deja afuera.
 *
 * Los límites son los oficiales del Gobierno de la Ciudad (los 48 barrios de la ley
 * 2.650, dataset "barrios" de data.buenosaires.gob.ar). Están guardados acá al lado
 * como recurso, así que resolver un barrio no sale a la red ni cuesta una llamada.
 */
@Component
@Slf4j
public class Barrios {

    private static final String RECURSO = "barrios-caba.json";

    private final List<Barrio> barrios;

    /** Un barrio y su contorno. Cada anillo es una lista de puntos [longitud, latitud]. */
    private record Barrio(String nombre, List<double[][]> anillos) {}

    public Barrios() {
        this.barrios = cargar();
        log.info("Límites de barrios cargados: {}", barrios.size());
    }

    /**
     * @return el barrio que contiene el punto, o vacío si cae fuera de la Ciudad.
     */
    public Optional<String> barrioDe(Double latitud, Double longitud) {
        if (latitud == null || longitud == null) {
            return Optional.empty();
        }

        for (Barrio barrio : barrios) {
            for (double[][] anillo : barrio.anillos()) {
                if (contiene(anillo, longitud, latitud)) {
                    return Optional.of(barrio.nombre());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Si un punto cae dentro de un anillo, por el método de tirar una semirrecta y
     * contar cuántos lados cruza: impar adentro, par afuera.
     *
     * Solo se guardan los contornos exteriores. El dataset marca como agujeros los
     * espejos de agua, y un local sobre los diques —"Isla Puerto Madero", en Pierina
     * Dealessi 701— cae justo adentro de uno: para Google está sobre el agua, y con
     * los agujeros puestos se quedaba sin barrio. Ningún barrio de la Ciudad rodea a
     * otro, así que ignorarlos no genera ambigüedad.
     */
    private static boolean contiene(double[][] anillo, double x, double y) {
        boolean adentro = false;
        for (int i = 0, j = anillo.length - 1; i < anillo.length; j = i++) {
            double xi = anillo[i][0], yi = anillo[i][1];
            double xj = anillo[j][0], yj = anillo[j][1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                adentro = !adentro;
            }
        }
        return adentro;
    }

    private static List<Barrio> cargar() {
        try (InputStream in = new ClassPathResource(RECURSO).getInputStream()) {
            JsonNode raiz = new ObjectMapper().readTree(in);
            List<Barrio> leidos = new ArrayList<>();

            for (JsonNode nodo : raiz) {
                List<double[][]> anillos = new ArrayList<>();
                for (JsonNode anillo : nodo.path("anillos")) {
                    double[][] puntos = new double[anillo.size()][2];
                    for (int i = 0; i < anillo.size(); i++) {
                        puntos[i][0] = anillo.get(i).get(0).asDouble();
                        puntos[i][1] = anillo.get(i).get(1).asDouble();
                    }
                    anillos.add(puntos);
                }
                leidos.add(new Barrio(nodo.path("nombre").asText(), anillos));
            }

            return List.copyOf(leidos);
        } catch (IOException ex) {
            // Sin los límites no se puede saber qué local está en la Ciudad y cuál no,
            // y la sincronización llenaría la base de locales de otras provincias.
            throw new IllegalStateException("No se pudo leer " + RECURSO, ex);
        }
    }
}
