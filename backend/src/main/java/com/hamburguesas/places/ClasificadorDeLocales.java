package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.texto.Texto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Si un local es una hamburguesería, y con qué prueba.
 *
 * La idea de la app son los locales que se especializan en hamburguesas o que la
 * tienen como plato destacado. Google no dice cuál es el plato destacado: dice el
 * rubro. Así que entran los que declara hamburguesería o comida rápida —que son las
 * cadenas— y los que se llaman a sí mismos así, que es la forma que tiene un local
 * de decir a qué se dedica. Quedan afuera las pizzerías, las parrillas, las
 * panaderías y los bares: venden hamburguesas, pero no es lo que uno busca acá.
 *
 * Con dos arreglos, porque la clasificación de Google es despareja.
 *
 * Uno automático: si otra sucursal de la misma cadena sí figura como hamburguesería,
 * esta también lo es. De las cuatro sucursales de "La Birra Bar", Google marca tres
 * como hamburguesería y la de Colegiales como restaurante.
 *
 * Y uno a mano, para los que no hay forma de deducir: "Beggars" figura como bar y
 * "Draken" como cervecería, y son hamburgueserías. Van anotados en la configuración
 * con el motivo al lado, igual que los que hay que sacar.
 *
 * Es la misma regla para lo que trae una búsqueda y para lo que ya está guardado: un
 * local guardado se mira como si recién lo hubiera devuelto Google.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ClasificadorDeLocales {

    private final PlacesProperties properties;
    private final LlamadasAGoogle google;
    private final BurgerJointRepository burgerJointRepository;

    /** Si lo que devolvió Google no es una hamburguesería. */
    boolean noEsUnaHamburgueseria(PlacesSearchResult.Place place, Set<String> cadenas) {
        return !evaluar(place, cadenas).vendeHamburguesas();
    }

    /**
     * Qué se decidió sobre un local y con qué prueba.
     *
     * Devuelve el veredicto entero y no un sí o un no porque la limpieza necesita
     * distinguir por qué: borrar es definitivo y no todas las razones alcanzan.
     */
    Veredicto evaluar(PlacesSearchResult.Place place, Set<String> cadenas) {
        var sync = properties.getSync();

        if (sync.getExcludedPlaceIds().contains(place.placeId())) {
            return Veredicto.no(Veredicto.Prueba.A_MANO);
        }
        if (sync.getIncludedPlaceIds().contains(place.placeId())) {
            return Veredicto.si(Veredicto.Prueba.A_MANO);
        }

        Set<String> rubrosDeOtraCosa = new HashSet<>(sync.getExcludedPrimaryTypes());
        Set<String> rubros = rubrosDe(place);

        Veredicto veredicto = VendeHamburguesas.evaluar(
            place.name(), rubros, null, rubrosDeOtraCosa, cadenas);

        // Solo cuando nada barato alcanzó se pregunta por el resumen de reseñas, que es
        // el tramo más caro de la API. Es la minoría de los casos, y es donde está la
        // diferencia: "Austin's Diner & Grill" no tiene el rubro ni lo dice el nombre.
        if (veredicto.prueba() == Veredicto.Prueba.SIN_PRUEBAS) {
            if (!google.quedan(PlacesCallType.RESUMEN)) {
                // Se acabó la cuota. No sabemos, y no saber no es lo mismo que saber que
                // no: queda anotado para que la limpieza no lo borre por un límite
                // nuestro.
                return Veredicto.no(Veredicto.Prueba.NO_SE_PUDO_PREGUNTAR);
            }

            String resumen = google.resumenDe(place.placeId());
            if (resumen == null) {
                return veredicto;
            }
            veredicto = VendeHamburguesas.evaluar(
                place.name(), rubros, resumen, rubrosDeOtraCosa, cadenas);
        }

        if (!veredicto.vendeHamburguesas()) {
            log.debug("{} queda afuera: {}", place.name(), veredicto.prueba());
        }
        return veredicto;
    }

    /**
     * Pocas fotos en Google y encima Google no lo llama hamburguesería.
     *
     * Las dos condiciones juntas, nunca sueltas, y el motivo es que por separado las dos
     * se equivocan. Que Google lo llame "restaurant" o "bar" no alcanza: La Birra Bar
     * figura como bar y es de las mejores de la ciudad. Y tener pocas fotos tampoco: de
     * los 228 locales con menos de diez, 221 son hamburgueserías de barrio de Quilmes,
     * Florencio Varela y Berazategui a las que simplemente nadie les sacó fotos. Borrar
     * por eso sería castigar al conurbano por ser menos fotografiado.
     *
     * Cruzadas sí dicen algo: un lugar que Google clasifica como kiosco, bar o casa de
     * artículos para el hogar, y que además nadie fotografió, casi nunca es una
     * hamburguesería. Los primeros que salieron así fueron un maxikiosco de Florencio
     * Varela y "Market up burger", que Google tiene como home_goods_store.
     *
     * Las cadenas quedan afuera: una sucursal de Mostaza con siete fotos sigue siendo un
     * Mostaza. Y los locales a los que todavía no se les preguntó cuántas fotos tienen
     * también: nulo no es cero, y no saber no es motivo para borrar.
     */
    boolean casiSinFotosYNoEsHamburgueseria(BurgerJoint joint) {
        int minimas = properties.getSync().getFotosMinimasSiNoEsHamburgueseria();
        return minimas > 0
            && !joint.isFastFood()
            && joint.getFotosEnGoogle() != null
            && joint.getFotosEnGoogle() < minimas
            && !"hamburger_restaurant".equals(joint.getGooglePrimaryType());
    }

    /**
     * Las marcas que Google sí reconoce como hamburgueserías, para el arreglo automático.
     *
     * Se arma una vez por pasada y se le pasa a cada evaluación: son las que rescatan a
     * las sucursales que Google clasificó distinto al resto.
     */
    Set<String> cadenasDeHamburguesas() {
        Set<String> cadenas = new HashSet<>();
        for (String nombre : burgerJointRepository.nombresDeRubro("hamburger_restaurant")) {
            String limpio = Texto.paraComparar(nombre);
            if (limpio.length() >= 8) {
                cadenas.add(limpio);
            }
        }
        return cadenas;
    }

    /**
     * Un local ya guardado, visto como lo que devolvería una búsqueda, para poder
     * pasarlo por el mismo filtro y que la regla sea una sola.
     */
    static PlacesSearchResult.Place comoLugar(BurgerJoint joint) {
        return new PlacesSearchResult.Place(
            joint.getPlaceId(), joint.getName(), joint.getAddress(),
            joint.getLatitude(), joint.getLongitude(), null, null, joint.getGooglePrimaryType(),
            joint.getGooglePrimaryType() == null ? Set.of() : Set.of(joint.getGooglePrimaryType()));
    }

    /**
     * Los rubros del local, cayendo al principal cuando no vinieron todos.
     *
     * Las fichas guardadas de antes solo tienen el principal, y pasan por este mismo
     * filtro en la limpieza: sin esto quedarían con la lista vacía y las sacaría a todas.
     */
    private static Set<String> rubrosDe(PlacesSearchResult.Place place) {
        if (place.types() != null && !place.types().isEmpty()) {
            return place.types();
        }
        return place.primaryType() == null ? Set.of() : Set.of(place.primaryType());
    }
}
