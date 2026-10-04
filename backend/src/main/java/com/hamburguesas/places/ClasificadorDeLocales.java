package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.texto.Texto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.EnumSet;
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
 * local guardado se mira como si recién lo hubiera devuelto Google, con una diferencia,
 * que es la prueba con la que entró (ver {@link #evaluarGuardado}).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ClasificadorDeLocales {

    private final PlacesProperties properties;
    private final LlamadasAGoogle google;
    private final BurgerJointRepository burgerJointRepository;

    /**
     * Las pruebas de que es una hamburguesería, que son las que se anotan y se vuelven a
     * usar. A_MANO también: un local que agregó una persona entra sin pasar por el
     * clasificador, porque esa decisión vale más que cualquier regla nuestra, y antes la
     * limpieza siguiente lo podía borrar si Google no tenía resumen de reseñas. Para
     * sacar uno se lo anota en la lista de excluidos, que se mira antes que todo.
     */
    private static final Set<Veredicto.Prueba> PRUEBAS_QUE_SE_ANOTAN = EnumSet.of(
        Veredicto.Prueba.RUBRO_DE_GOOGLE,
        Veredicto.Prueba.EL_NOMBRE_LO_DICE,
        Veredicto.Prueba.SUCURSAL_DE_UNA_CADENA,
        Veredicto.Prueba.LO_DICEN_LAS_RESENIAS,
        Veredicto.Prueba.A_MANO);

    /**
     * Qué se decidió sobre un local y con qué prueba.
     *
     * Devuelve el veredicto entero y no un sí o un no porque la limpieza necesita
     * distinguir por qué: borrar es definitivo y no todas las razones alcanzan.
     */
    Veredicto evaluar(PlacesSearchResult.Place place, Set<String> cadenas) {
        return evaluar(place, cadenas, null);
    }

    /**
     * Lo mismo que {@link #evaluar(PlacesSearchResult.Place, Set)}, para un local que ya
     * está guardado: con la prueba con la que entró.
     *
     * Un local guardado tiene menos datos que cuando lo devolvió la búsqueda: el rubro
     * principal y no la lista entera, y no su resumen de reseñas. Los que entraron por un
     * rubro secundario o por su resumen caían siempre en "sin pruebas", y cada limpieza le
     * volvía a pedir a Google el resumen, que es el tramo más caro de la API: unas 32
     * llamadas por corrida, siempre por los mismos locales (#97). Y uno que entró por su
     * resumen quedaba atado a él: si Google lo cambiaba, la limpieza siguiente lo borraba.
     *
     * Ahora, si las pruebas baratas no alcanzan y el local tiene una prueba anotada, vale
     * esa. Lo que puede cambiar de este lado se sigue mirando siempre: las listas a mano y
     * los rubros de otra cosa de la configuración van antes que la prueba anotada.
     */
    Veredicto evaluarGuardado(BurgerJoint joint, Set<String> cadenas) {
        return evaluar(comoLugar(joint), cadenas, pruebaAnotada(joint));
    }

    /**
     * @param anotada la prueba con la que entró, si ya está guardado; vale solo cuando
     *                ninguna prueba barata alcanza, y evita pagar el resumen de reseñas
     */
    Veredicto evaluar(PlacesSearchResult.Place place, Set<String> cadenas, Veredicto.Prueba anotada) {
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

        // Nada barato alcanzó, pero ya se sabe con qué prueba entró: no hace falta volver
        // a pagarla.
        if (veredicto.prueba() == Veredicto.Prueba.SIN_PRUEBAS && anotada != null) {
            return Veredicto.si(anotada);
        }

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

            String resumen;
            try {
                resumen = google.resumenDe(place.placeId());
            } catch (RestClientException ex) {
                // Google no contestó: un error, un corte de red, un timeout. Es lo mismo
                // que la cuota agotada —no pudimos mirar— y tiene que terminar igual. Se
                // leía como "no tiene resumen", que es "sin pruebas" y borra: un 503 de
                // un rato durante una limpieza alcanzaba para perder hamburgueserías de
                // verdad (#96). Y un corte de red se escapaba y frenaba el barrido entero.
                //
                // Atrapa cualquier falla del cliente y no solo las respuestas con código,
                // porque acá equivocarse es borrar.
                log.warn("No se pudo pedir el resumen de {}: {}", place.placeId(), ex.getMessage());
                return Veredicto.no(Veredicto.Prueba.NO_SE_PUDO_PREGUNTAR);
            }

            // Google contestó que no tiene: eso sí es una respuesta, y sin resumen no
            // quedan pruebas de que venda hamburguesas.
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
     * La prueba con la que entró el local, si es de las que se pueden volver a usar.
     *
     * Nulo si no tiene anotada o si es un nombre que ya no existe en
     * {@link Veredicto.Prueba}: en esos casos se lo evalúa como a cualquiera.
     */
    static Veredicto.Prueba pruebaAnotada(BurgerJoint joint) {
        if (joint.getPruebaDeHamburguesas() == null) {
            return null;
        }
        try {
            Veredicto.Prueba prueba = Veredicto.Prueba.valueOf(joint.getPruebaDeHamburguesas());
            return PRUEBAS_QUE_SE_ANOTAN.contains(prueba) ? prueba : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
