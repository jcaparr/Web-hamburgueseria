package com.hamburguesas.places;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre la configuración con la que sale a buscar el trabajo de Places.
 *
 * Las reglas de qué entra y qué no viven en application.yml, no en el código, así que
 * son un renglón que cualquiera puede borrar sin que falle nada. Acá se afirma lo que
 * costó descubrir: los rubros que no son una hamburguesería aunque el nombre lo diga, y
 * que cada forma de preguntar sea distinta de las demás.
 */
@SpringBootTest
@ActiveProfiles("test")
class PlacesConfiguracionTest {

    @Autowired private PlacesProperties properties;

    /**
     * Una pizzería que se llama "Pizza, Birra & Burger" entra por la regla del nombre,
     * que es la puerta más ancha del filtro: alcanza con que diga "burger". Lo único
     * que la frena es que su rubro esté en esta lista.
     */
    @Test
    void dejaAfueraLosRubrosDeComidaQueNoSonHamburgueseria() {
        assertThat(properties.getSync().getExcludedPrimaryTypes())
            .contains("pizza_restaurant", "bakery", "barbecue_restaurant",
                "steak_house", "ice_cream_shop");
    }

    /** Y los rubros que directamente no dan de comer, que fue por donde entraron. */
    @Test
    void dejaAfueraLosRubrosQueNoDanDeComer() {
        assertThat(properties.getSync().getExcludedPrimaryTypes())
            .contains("manufacturer", "wholesaler", "butcher_shop", "indoor_playground");
    }

    /**
     * Cada pregunta tiene su propio tope de sesenta resultados, así que repetida no
     * suma nada: son sesenta llamadas por barrio a cambio de los mismos locales.
     */
    @Test
    void cadaFormaDePreguntarEsDistinta() {
        List<String> preguntas = properties.getSync().getQueryTemplates();

        assertThat(preguntas).doesNotHaveDuplicates().hasSizeGreaterThanOrEqualTo(4);
        assertThat(preguntas).allSatisfy(pregunta ->
            assertThat(pregunta).contains("{barrio}"));
    }

    /**
     * Dónde se busca: los 48 barrios de la Ciudad más el conurbano y las ciudades del
     * borde.
     *
     * Se afirman los límites que se quisieron cubrir, que es lo que costó decidir. La
     * lista dice dónde preguntar; que un local entre o no lo decide el radio.
     */
    @Test
    void buscaEnLaCiudadYEnElConurbano() {
        List<String> donde = properties.getSync().getAreas();

        assertThat(donde).doesNotHaveDuplicates().hasSizeGreaterThan(48);
        assertThat(donde).contains("Palermo", "Mataderos");
        assertThat(donde).contains("La Plata", "San Vicente", "Cañuelas", "Luján",
            "Belén de Escobar");
    }

    /** El radio es lo que decide qué entra, y tiene que cubrir esos bordes. */
    @Test
    void elRadioCubreLosBordes() {
        assertThat(properties.getSync().getRadioEnKm()).isGreaterThanOrEqualTo(75);
    }

    /**
     * Los locales que se anotaron a mano después de mirar sus fotos en Google.
     *
     * Ninguna regla los agarra: Google les pone el rubro hamburguesería igual, y de la
     * ficha sola no se puede deducir que cocinan otra cosa. Lo único que los frena es
     * este renglón, y borrarlo los devuelve al listado sin que falle nada más.
     */
    @Test
    void dejaAfueraLasFichasQueSeRevisaronAMano() {
        assertThat(properties.getSync().getExcludedPlaceIds())
            // "Las Delicias de O & P": rotisería, ninguna de sus diez fotos es una hamburguesa.
            .contains("ChIJeUPEbarOvJURhWbGRz-dl6c")
            // "Pizza Burger": pizzería, entró solo por tener "burger" en el nombre.
            .contains("ChIJB2UW9My1vJUR4qN1P3ZodXM")
            // Los cinco de abajo son los que quedaron de la revisión de los dudosos:
            // Labarden muestra pizzas, Colombres desayunos, Sanguchazo es una
            // fiambrería, Xardo una parrilla y La Rubia un almacén.
            .contains("ChIJfY3jagDLvJURbkncyQMEuSw")
            .contains("ChIJXyCeMFLLvJURaclMz2Im9zM")
            .contains("ChIJ44LnQwDJvJURSTzNIX6BjLk")
            .contains("ChIJWa8GmgbJvJURxVwfjI7FwF8")
            .contains("ChIJSS4CeaHJvJURc7wYnWHVzuY");
    }

    /**
     * Las cadenas que se pueden apagar en Explorar.
     *
     * Las tres locales pesan más que las internacionales: entre Mr Tasty, Dean & Dennys
     * y Hamburguesas Extremas hay treinta y dos sucursales en la base. Van normalizadas,
     * sin mayúsculas ni puntuación, porque es como se compara el nombre.
     */
    @Test
    void conoceLasCadenasQueSePuedenApagar() {
        assertThat(properties.getFastFoodBrands())
            .contains("burgerking", "mcdonalds", "wendys", "mostaza",
                "hamburguesasextremas", "deandennys", "mrtasty")
            .allSatisfy(marca -> assertThat(marca).matches("[a-z0-9]+"));
    }

    /** Un mismo local anotado de los dos lados sería una regla que se contradice. */
    @Test
    void ningunLocalEstaAnotadoParaEntrarYParaSalir() {
        assertThat(properties.getSync().getExcludedPlaceIds())
            .doesNotHaveDuplicates()
            .doesNotContainAnyElementsOf(properties.getSync().getIncludedPlaceIds());
    }

    /**
     * Ningún tope puede pasar del tramo gratuito de Google.
     *
     * Es el único renglón de la configuración que puede terminar en una factura. El
     * freno existe porque pasado el tope no se llama más, y un número de más acá adentro
     * lo desactiva sin que falle nada: las llamadas salen igual y se cobran.
     *
     * Los tramos son 5.000 búsquedas, 5.000 fichas, 1.000 fotos y 1.000 resúmenes por
     * mes. Los dos últimos son los chicos, y el resumen además es el caro: veinticinco
     * dólares cada mil pasado el tramo.
     */
    @Test
    void ningunTopeSePasaDelTramoGratuito() {
        var quota = properties.getQuota();

        assertThat(quota.getMonthlySearchCalls()).isLessThanOrEqualTo(5000);
        assertThat(quota.getMonthlyDetailsCalls()).isLessThanOrEqualTo(5000);
        assertThat(quota.getMonthlyPhotoCalls()).isLessThanOrEqualTo(1000);
        assertThat(quota.getMonthlyResumenCalls()).isLessThanOrEqualTo(1000);
    }

    /**
     * Y el de fichas tiene que dar para completar todas las fotos del mes.
     *
     * Cada portada cuesta una ficha para preguntar y una foto para bajar. Si las fichas
     * se terminan primero, el mes cierra con fotos sin usar —cuota gratis que no se
     * acumula y se pierde el día 1—, que es exactamente lo que pasó en octubre.
     */
    @Test
    void lasFichasAlcanzanParaGastarTodasLasFotos() {
        var quota = properties.getQuota();

        assertThat(quota.getMonthlyDetailsCalls())
            .isGreaterThanOrEqualTo(quota.getMonthlyPhotoCalls());
    }

    /**
     * Los que se agregaron a mano tienen que estar anotados para entrar.
     *
     * Se agregan justamente porque ninguna búsqueda los devuelve: Google no les pone el
     * rubro de hamburguesería. Esa misma razón hace que la limpieza los borre en la
     * corrida siguiente si no están acá, así que agregarlos sin anotarlos es trabajo que
     * se deshace solo.
     */
    @Test
    void losAgregadosAManoEstanAnotadosParaQueNoSeBorren() {
        assertThat(properties.getSync().getIncludedPlaceIds())
            // "Lucky Louis", Av. Olazábal 1615: figura como restaurant.
            .contains("ChIJH7tAfQC1vJURnF0yXSHnV74")
            // "Austin's Diner & Grill", Darwin 1111: figura como american_restaurant.
            .contains("ChIJm1VdjT-1vJUR-QBpgx3eg0I");
    }
}
