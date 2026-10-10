package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sale de la lista el que tiene pocas fotos Y además Google no llama hamburguesería.
 *
 * Las dos condiciones juntas, nunca sueltas, porque por separado las dos se equivocan:
 * La Birra Bar figura como bar y es de las mejores de la ciudad, y de los 228 locales con
 * menos de diez fotos, 221 son hamburgueserías de barrio del conurbano a las que
 * simplemente nadie les sacó fotos.
 *
 * La mitad de este archivo prueba a quién NO hay que borrar, que es donde está el riesgo.
 */
class PocasFotosYOtroRubroTest {

    private PlacesProperties properties;
    private BurgerJointRepository repository;
    private PlacesSyncService service;

    @BeforeEach
    void setUp() {
        properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setDelayBetweenCallsMs(0);

        repository = mock(BurgerJointRepository.class);
        PlacesQuotaGuard quotaGuard = mock(PlacesQuotaGuard.class);
        RatingRepository ratingRepository = mock(RatingRepository.class);
        WishlistRepository wishlistRepository = mock(WishlistRepository.class);
        SavedTourRepository savedTourRepository = mock(SavedTourRepository.class);

        when(quotaGuard.canCall(any())).thenReturn(true);
        when(repository.findAll()).thenReturn(List.of());
        when(repository.findByFastFoodFalse()).thenReturn(List.of());
        when(repository.nombresDeRubro(anyString())).thenReturn(List.of());
        when(repository.findByPlaceId(anyString())).thenReturn(Optional.empty());
        when(ratingRepository.existsByBurgerJoint_Id(anyLong())).thenReturn(false);
        when(wishlistRepository.existsByBurgerJoint_Id(anyLong())).thenReturn(false);
        when(savedTourRepository.estaEnAlgunTour(anyLong())).thenReturn(false);

        service = ServicioArmado.armar(
            properties, mock(PlacesClient.class), quotaGuard, mock(PhotoStorage.class),
            repository, new Zonas(new Barrios(), properties), ratingRepository,
            wishlistRepository, savedTourRepository,
            new FastFoodMarker(repository, properties));
    }

    /** Un local de Palermo, para que la zona no sea lo que lo saque. */
    private static BurgerJoint local(String nombre, Integer fotos, String rubro, boolean cadena) {
        BurgerJoint joint = BurgerJoint.builder()
            .id(1L).placeId("ChIJ1").name(nombre)
            .address("Costa Rica 5827, CABA").area("Palermo")
            .latitude(-34.5900).longitude(-58.4270)
            .googlePrimaryType(rubro).fastFood(cadena)
            .build();
        joint.setFotosEnGoogle(fotos);
        return joint;
    }

    private int borradosCon(BurgerJoint joint) {
        when(repository.findAll()).thenReturn(List.of(joint));
        return service.limpiar().borrados();
    }

    // ---- los que salen ----

    /** "Market up burger": Google lo tiene como casa de artículos para el hogar. */
    @Test
    void saleElQueTienePocasFotosYOtroRubro() {
        assertThat(borradosCon(local("Market up burger", 2, "home_goods_store", false)))
            .isEqualTo(1);
    }

    /** El kiosco 24hs de Florencio Varela: dos fotos y Google lo llama bar. */
    @Test
    void saleElKioscoQueVendeHamburguesasSueltas() {
        assertThat(borradosCon(local("El kiosco 24 Hs (((HAMBURGUESAS)))", 2, "bar", false)))
            .isEqualTo(1);
    }

    // ---- los que se quedan, que es donde está el riesgo ----

    /**
     * Pocas fotos pero Google sí lo llama hamburguesería: se queda.
     *
     * Son 221 de los 228 que tienen menos de diez. Hamburgueserías de barrio de Quilmes y
     * Florencio Varela a las que nadie les sacó fotos. Si esta condición faltara, la
     * regla se llevaría puesto medio conurbano.
     */
    @Test
    void sePocasFotosPeroEsHamburgueseriaSeQueda() {
        assertThat(borradosCon(local("Soria Burger", 1, "hamburger_restaurant", false)))
            .isZero();
    }

    /**
     * Otro rubro pero con fotos de sobra: se queda.
     *
     * La Birra Bar figura como bar y es de las mejores de la ciudad. El rubro de Google
     * solo no alcanza para borrar a nadie.
     */
    @Test
    void siTieneFotosDeSobraSeQuedaAunqueElRubroSeaOtro() {
        assertThat(borradosCon(local("La Birra Bar Burger", 10, "bar", false))).isZero();
    }

    /** Una sucursal de cadena con pocas fotos sigue siendo esa cadena. */
    @Test
    void unaSucursalDeCadenaNoSaleNuncaPorEstaRegla() {
        assertThat(borradosCon(local("Mostaza - Quilmes Burger", 3, "fast_food_restaurant", true)))
            .isZero();
    }

    /**
     * Y al que todavía no se le preguntó no se lo toca.
     *
     * Nulo no es cero: son 978 locales a los que no les alcanzó la cuota del mes, y no
     * saber no puede ser motivo para borrar.
     */
    @Test
    void alQueNoSeLePreguntoNoSeLoBorra() {
        assertThat(borradosCon(local("Sin preguntar Burger", null, "restaurant", false))).isZero();
    }

    /** Con el umbral en cero la regla queda apagada. */
    @Test
    void enCeroLaReglaNoHaceNada() {
        properties.getSync().setFotosMinimasSiNoEsHamburgueseria(0);

        assertThat(borradosCon(local("Market up burger", 2, "home_goods_store", false)))
            .isZero();
    }

    /** Y el que alguien guardó se queda igual, como con cualquier otro motivo de borrado. */
    @Test
    void noBorraElQueAlguienTieneGuardado() {
        when(repository.findAll())
            .thenReturn(List.of(local("Market up burger", 2, "home_goods_store", false)));
        when(repository.findByPlaceId(anyString())).thenReturn(Optional.empty());

        LimpiezaResult sinGuardar = service.limpiar();
        assertThat(sinGuardar.borrados()).isEqualTo(1);

        verify(repository).delete(any(BurgerJoint.class));
    }

    /** El borde exacto: justo en el umbral se queda, uno menos sale. */
    @Test
    void elBordeEstaEnElUmbral() {
        assertThat(borradosCon(local("Justo Burger", 10, "restaurant", false))).isZero();

        setUp();
        assertThat(borradosCon(local("Uno menos Burger", 9, "restaurant", false))).isEqualTo(1);
    }

    /** Un rubro vacío cuenta como "no es hamburguesería", que es lo que Google deja así. */
    @Test
    void sinRubroCuentaComoOtroRubro() {
        assertThat(borradosCon(local("Sin rubro Burger", 2, null, false))).isEqualTo(1);
    }

    /**
     * El que está en la lista de incluidos se queda (#227). Es justo el caso de agregar a
     * mano: Google lo tiene como "restaurant", y si además tiene pocas fotos, esta regla lo
     * borraba en la limpieza siguiente aunque una persona hubiera decidido que va.
     */
    @Test
    void elIncluidoAManoSeQuedaAunqueTengaPocasFotos() {
        BurgerJoint acido = local("Ácido", 4, "restaurant", false);
        properties.getSync().setIncludedPlaceIds(List.of(acido.getPlaceId()));

        assertThat(borradosCon(acido)).isZero();
    }

    /** Lo mismo con el que entró por el endpoint de agregar, aunque no esté en la lista. */
    @Test
    void elAgregadoAManoSeQuedaAunqueTengaPocasFotos() {
        BurgerJoint agregado = local("Ácido", 4, "restaurant", false);
        agregado.setPruebaDeHamburguesas(Veredicto.Prueba.A_MANO.name());

        assertThat(borradosCon(agregado)).isZero();
    }
}
