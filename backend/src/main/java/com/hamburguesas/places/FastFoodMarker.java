package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Marca qué locales son sucursales de una cadena de comida rápida.
 *
 * Explorar deja apagarlas, y para eso hay que saber cuáles son. El rubro que declara
 * Google no alcanza: de las nueve sucursales de Mostaza marca una como comida rápida y
 * el resto como hamburguesería, y con Wendy's pasa lo mismo. Clasifica desparejo dentro
 * de una misma cadena, así que lo que decide es la marca en el nombre.
 *
 * Las marcas están en la configuración y se reconocen por el principio del nombre:
 * "Burger King - Sucursal P.Italia" y "McDonald's Abasto Patio de Comidas" son
 * sucursales, y un local que apenas mencione la marca más adelante no lo es.
 *
 * Corre al arrancar, y no en la sincronización, para que agregar una marca a la lista
 * alcance con reiniciar. Son cuatrocientas comparaciones de texto y solo escribe los
 * que cambian.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FastFoodMarker {

    private final BurgerJointRepository burgerJointRepository;
    private final PlacesProperties properties;

    @Bean
    ApplicationRunner marcarCadenasDeComidaRapida() {
        return args -> marcar();
    }

    @Transactional
    void marcar() {
        List<String> marcas = properties.getFastFoodBrands().stream()
            .map(FastFoodMarker::sinAdornos)
            .filter(marca -> !marca.isBlank())
            .toList();

        int cambiados = 0;
        for (BurgerJoint joint : burgerJointRepository.findAll()) {
            boolean esCadena = esDeUnaCadena(joint.getName(), marcas);
            if (esCadena != joint.isFastFood()) {
                joint.setFastFood(esCadena);
                burgerJointRepository.save(joint);
                cambiados++;
            }
        }

        if (cambiados > 0) {
            log.info("Locales que cambiaron de marca de comida rápida: {}", cambiados);
        }
    }

    static boolean esDeUnaCadena(String nombre, List<String> marcas) {
        String limpio = sinAdornos(nombre);
        return marcas.stream().anyMatch(limpio::startsWith);
    }

    /**
     * El nombre sin mayúsculas, acentos ni puntuación. Es lo que hace que "Wendys" y
     * "Wendy's" sean la misma cadena: en Google están escritas de las dos formas.
     */
    private static String sinAdornos(String valor) {
        return Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
    }
}
