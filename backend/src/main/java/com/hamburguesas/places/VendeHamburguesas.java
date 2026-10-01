package com.hamburguesas.places;

import com.hamburguesas.texto.Texto;
import java.util.List;
import java.util.Set;

import static com.hamburguesas.places.Veredicto.Prueba;

/**
 * Si un local vende hamburguesas, y con qué prueba.
 *
 * Reemplaza a una cadena de ifs que había que retocar cada vez que aparecía un caso
 * nuevo. La idea acá es otra: en vez de una regla que decide, varias pruebas
 * independientes, cada una barata y cada una con su fuerza. Se agrega una prueba nueva
 * sin tocar las que ya están.
 *
 * Lo que se busca son lugares donde comer una hamburguesa, no lugares que la tengan en
 * la carta. La diferencia importa en las dos direcciones, y las dos costaron:
 *
 * Quedaban afuera locales que sí lo son. "Austin's Diner & Grill" no tiene
 * hamburger_restaurant entre sus rubros y su nombre no dice hamburguesa, así que no
 * entraba nunca; sus reseñas hablan de "delicious grilled and smash burgers". "La Birra
 * Bar Colegiales" figura como restaurante, y es de las más conocidas de la ciudad.
 *
 * Y quedaban adentro locales que no. "Las Delicias de O & P" es una rotisería y Google
 * la da como hamburguesería; de sus diez fotos ninguna es una hamburguesa.
 *
 * El rubro principal no alcanza para ninguna de las dos cosas, así que se mira la lista
 * completa de rubros, que Google devuelve en el mismo tramo barato y nunca usábamos.
 */
public final class VendeHamburguesas {

    /** Lo que Google llama hamburguesería, y la comida rápida, que son las cadenas. */
    private static final Set<String> RUBROS_DE_HAMBURGUESAS =
        Set.of("hamburger_restaurant", "fast_food_restaurant");

    /**
     * Las palabras con las que un local dice a qué se dedica.
     *
     * "smash" entra porque una smash burger es una hamburguesa y nada más: no hay otro
     * rubro que use esa palabra.
     */
    private static final List<String> EL_NOMBRE_DICE_HAMBURGUESAS =
        List.of("burger", "hamburgues", "smash");

    /**
     * Lo que dicen las reseñas cuando hablan de hamburguesas.
     *
     * El resumen viene en inglés aunque el local sea de acá, así que van las dos formas.
     */
    private static final List<String> LAS_RESENIAS_DICEN_HAMBURGUESAS =
        List.of("burger", "hamburgues");

    private VendeHamburguesas() {
    }

    /**
     * @param rubros         todos los que Google le pone, no solo el principal
     * @param resumenDeResenias  el resumen de Google, o null si no tiene: solo seis de
     *                       cada diez locales tienen uno, así que su ausencia no prueba
     *                       nada y nunca decide por sí sola
     * @param rubrosDeOtraCosa   los que declaran dedicarse a otra cosa, de la configuración
     * @param cadenas        las marcas que Google sí reconoce como hamburgueserías
     */
    public static Veredicto evaluar(String nombre, Set<String> rubros, String resumenDeResenias,
                                    Set<String> rubrosDeOtraCosa, Set<String> cadenas) {
        // Primero lo que descalifica. Va antes que todo lo demás porque una fábrica de
        // medallones y un kiosco tienen "hamburguesas" en el nombre, y el nombre es la
        // puerta más ancha: sin esto entrarían por ahí.
        if (seDedicaAOtraCosa(rubros, rubrosDeOtraCosa)) {
            return Veredicto.no(Prueba.SE_DEDICA_A_OTRA_COSA);
        }

        // Que Google lo tenga entre sus rubros, aunque no sea el principal. Es lo que
        // rescata a "La Birra Bar Colegiales", que figura como restaurante y tiene
        // hamburger_restaurant en la lista.
        if (rubros.stream().anyMatch(RUBROS_DE_HAMBURGUESAS::contains)) {
            return Veredicto.si(Prueba.RUBRO_DE_GOOGLE);
        }

        if (elNombreLoDice(nombre)) {
            return Veredicto.si(Prueba.EL_NOMBRE_LO_DICE);
        }

        if (esSucursalDeUnaCadena(nombre, cadenas)) {
            return Veredicto.si(Prueba.SUCURSAL_DE_UNA_CADENA);
        }

        // Lo último, porque es lo que cuesta: el resumen de reseñas está en el tramo más
        // caro de la API. Se pregunta solo por los que ninguna prueba barata resolvió,
        // que es la minoría.
        if (resumenDeResenias != null && !resumenDeResenias.isBlank()) {
            return hablaDeHamburguesas(resumenDeResenias)
                ? Veredicto.si(Prueba.LO_DICEN_LAS_RESENIAS)
                : Veredicto.no(Prueba.LAS_RESENIAS_HABLAN_DE_OTRA_COSA);
        }

        return Veredicto.no(Prueba.SIN_PRUEBAS);
    }

    /**
     * Si sus rubros dicen que se dedica a otra cosa.
     *
     * Tener el rubro de hamburguesas lo salva: un local puede ser una hamburguesería y
     * además un almacén. "Panchos Y Hamburguesas" figura como grocery_store y como
     * hamburger_restaurant, y vende hamburguesas; la foto que Google muestra son
     * marcadores Stabilo, pero eso es un problema de la foto, no del local.
     */
    private static boolean seDedicaAOtraCosa(Set<String> rubros, Set<String> rubrosDeOtraCosa) {
        if (rubros.stream().anyMatch(RUBROS_DE_HAMBURGUESAS::contains)) {
            return false;
        }
        return rubros.stream().anyMatch(rubrosDeOtraCosa::contains);
    }

    static boolean elNombreLoDice(String nombre) {
        String limpio = Texto.paraComparar(nombre);
        return EL_NOMBRE_DICE_HAMBURGUESAS.stream().anyMatch(limpio::contains);
    }

    private static boolean hablaDeHamburguesas(String resumen) {
        String limpio = Texto.paraComparar(resumen);
        return LAS_RESENIAS_DICEN_HAMBURGUESAS.stream().anyMatch(limpio::contains);
    }

    /**
     * Si el nombre empieza con el de una cadena que Google sí reconoce.
     *
     * Se exige que sea el principio y que siga un espacio —"La Birra Bar Colegiales"
     * empieza con "La Birra Bar"—, para que un nombre corto no se lleve puesto a
     * cualquiera que empiece parecido.
     */
    static boolean esSucursalDeUnaCadena(String nombre, Set<String> cadenas) {
        String limpio = Texto.paraComparar(nombre);
        return cadenas.stream().anyMatch(cadena -> limpio.startsWith(cadena + " "));
    }
}
