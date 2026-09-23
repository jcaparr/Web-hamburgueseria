package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre qué entra a la base y con qué barrio.
 *
 * Las búsquedas son texto libre —"hamburguesería en Palermo, Buenos Aires"— y Google
 * contesta lo que le parece: locales de otros barrios, de otras provincias, de otros
 * países, y negocios que no dan de comer. Hasta acá entraba todo, y el barrio que se
 * guardaba era el de la búsqueda.
 *
 * No sale a internet: el cliente de Google está simulado.
 */
class PlacesSyncFiltroTest {

    private PlacesProperties properties;
    private PlacesClient placesClient;
    private BurgerJointRepository repository;
    private RatingRepository ratingRepository;
    private WishlistRepository wishlistRepository;
    private PlacesSyncService service;

    @BeforeEach
    void setUp() {
        properties = new PlacesProperties();
        properties.setApiKey("clave-de-prueba");
        properties.getSync().setAreas(List.of("Villa Real"));
        properties.getSync().setMaxPagesPerArea(1);
        properties.getSync().setDelayBetweenCallsMs(0);

        placesClient = mock(PlacesClient.class);
        ratingRepository = mock(RatingRepository.class);
        wishlistRepository = mock(WishlistRepository.class);
        PlacesQuotaGuard quotaGuard = mock(PlacesQuotaGuard.class);
        PhotoStorage photoStorage = mock(PhotoStorage.class);
        repository = mock(BurgerJointRepository.class);

        when(quotaGuard.canCall(any())).thenReturn(true);
        when(repository.findByPhotoUrlIsNull()).thenReturn(List.of());
        when(repository.conFotoElegidaConUnaReglaVieja(anyInt())).thenReturn(List.of());
        when(repository.findByPlaceId(anyString())).thenReturn(Optional.empty());
        when(repository.nombresDeRubro(anyString())).thenReturn(List.of());

        service = new PlacesSyncService(
            properties, placesClient, quotaGuard, photoStorage, repository, new Barrios(),
            ratingRepository, wishlistRepository);
    }

    private void googleDevuelve(PlacesSearchResult.Place... lugares) {
        when(placesClient.searchText(anyString(), any()))
            .thenReturn(new PlacesSearchResult(List.of(lugares), null));
    }

    private static PlacesSearchResult.Place lugar(String nombre, double lat, double lon, String rubro) {
        return new PlacesSearchResult.Place(
            "ChIJ-" + nombre, nombre, "Una dirección", lat, lon, null, rubro);
    }

    /**
     * El barrio es el de la dirección, no el de la búsqueda. "The Burger Company", en
     * Honduras 4733, apareció buscando en Villa Real y quedó guardado en Villa Real,
     * a doce kilómetros de donde está.
     */
    @Test
    void guardaElBarrioDeLaDireccionYNoElDeLaBusqueda() {
        googleDevuelve(lugar("The Burger Company", -34.5900, -58.4270, "hamburger_restaurant"));

        PlacesSyncReport report = service.sync();

        ArgumentCaptor<BurgerJoint> guardado = ArgumentCaptor.forClass(BurgerJoint.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getArea()).isEqualTo("Palermo");
        assertThat(report.created()).isEqualTo(1);
    }

    /** Buscar "Versalles" trajo una hamburguesería de Floridablanca, Colombia. */
    @Test
    void noGuardaLoQueEstaFueraDeLaCiudad() {
        googleDevuelve(lugar("Versalles Burguer", 7.0625062, -73.0978239, "hamburger_restaurant"));

        PlacesSyncReport report = service.sync();

        verify(repository, never()).save(any());
        assertThat(report.created()).isZero();
    }

    /**
     * Google le cuelga el tipo hamburguesería a negocios que no dan de comer. En la
     * base había una fábrica de salchichas, un mayorista de medallones, una carnicería,
     * un pelotero y una distribuidora.
     *
     * Va antes que la regla del nombre a propósito: el mayorista se llama "MARKET UP
     * BURGER BELGRANO" y si no entraría por ahí.
     */
    @Test
    void noGuardaLosRubrosQueNoSonUnLugarDondeComer() {
        properties.getSync().setExcludedPrimaryTypes(List.of("butcher_shop", "wholesaler"));
        googleDevuelve(
            lugar("Tito & Gonza Carnicería", -34.5900, -58.4270, "butcher_shop"),
            lugar("MARKET UP BURGER BELGRANO", -34.5900, -58.4270, "wholesaler"),
            lugar("Voraz", -34.5900, -58.4270, "hamburger_restaurant"));

        PlacesSyncReport report = service.sync();

        ArgumentCaptor<BurgerJoint> guardado = ArgumentCaptor.forClass(BurgerJoint.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getName()).isEqualTo("Voraz");
        assertThat(report.created()).isEqualTo(1);
    }

    /**
     * La app es de hamburgueserías, no de lugares donde además hay hamburguesas. Una
     * pizzería, una parrilla, una panadería o un bar venden hamburguesas y Google se
     * las marca, pero no es lo que uno viene a buscar acá.
     */
    @Test
    void noGuardaLosLugaresDeOtraEspecialidad() {
        googleDevuelve(
            lugar("El Camba", -34.5900, -58.4270, "pizza_restaurant"),
            lugar("Paso a Paso", -34.5900, -58.4270, "argentinian_restaurant"),
            lugar("Ninina", -34.5900, -58.4270, "breakfast_restaurant"),
            lugar("Johnny B Good Casino", -34.5900, -58.4270, "bar_and_grill"));

        PlacesSyncReport report = service.sync();

        verify(repository, never()).save(any());
        assertThat(report.created()).isZero();
    }

    /** Las cadenas de comida rápida son hamburgueserías aunque Google las llame así. */
    @Test
    void guardaLasCadenasDeComidaRapida() {
        googleDevuelve(lugar("McDonald's", -34.5900, -58.4270, "fast_food_restaurant"));

        assertThat(service.sync().created()).isEqualTo(1);
    }

    /** Un local que se llama "algo Burger" está diciendo a qué se dedica. */
    @Test
    void guardaLosQueSeLlamanComoLoQueSon() {
        googleDevuelve(
            lugar("LUCKY BURGER BAR", -34.5900, -58.4270, "bar"),
            lugar("Hamburguesas y comidas shelby", -34.5900, -58.4270, "meal_delivery"));

        assertThat(service.sync().created()).isEqualTo(2);
    }

    /**
     * Google clasifica desparejo dentro de una misma cadena: de las cuatro sucursales
     * de "La Birra Bar" marca tres como hamburguesería y la de Colegiales como
     * restaurante. Si una sucursal es hamburguesería, las otras también.
     */
    @Test
    void guardaLaSucursalQueGoogleClasificoDistintoAlResto() {
        when(repository.nombresDeRubro("hamburger_restaurant")).thenReturn(List.of("La Birra Bar"));
        googleDevuelve(lugar("La Birra Bar Colegiales", -34.5900, -58.4270, "restaurant"));

        assertThat(service.sync().created()).isEqualTo(1);
    }

    /** Pero un nombre parecido no alcanza: tiene que empezar con el de la cadena. */
    @Test
    void noConfundeUnNombreParecidoConUnaSucursal() {
        when(repository.nombresDeRubro("hamburger_restaurant")).thenReturn(List.of("La Birra Bar"));
        googleDevuelve(lugar("Nueva Roma Birrería", -34.5900, -58.4270, "bar"));

        assertThat(service.sync().created()).isZero();
    }

    /**
     * Y para las que no hay forma de deducir —"Beggars" figura como bar y es una
     * hamburguesería— queda la lista a mano.
     */
    @Test
    void guardaLasHamburgueseriasAnotadasAMano() {
        properties.getSync().setIncludedPlaceIds(List.of("ChIJ-Beggars Caballito"));
        googleDevuelve(lugar("Beggars Caballito", -34.5900, -58.4270, "bar"));

        assertThat(service.sync().created()).isEqualTo(1);
    }

    /**
     * Cuando la ficha de Google está mal no hay regla que sirva: "Myc La dimensión 3d"
     * figura como gastropub con hamburguesas y su sitio es un local de impresión 3D.
     */
    @Test
    void noGuardaLasFichasAnotadasComoMalClasificadas() {
        properties.getSync().setExcludedPlaceIds(List.of("ChIJ-Myc La dimensión 3d"));
        googleDevuelve(lugar("Myc La dimensión 3d", -34.5900, -58.4270, "gastropub"));

        PlacesSyncReport report = service.sync();

        verify(repository, never()).save(any());
        assertThat(report.created()).isZero();
    }

    /** El rubro se guarda para poder revisar después qué entró sin volver a preguntar. */
    @Test
    void guardaElRubroQueDeclaraGoogle() {
        googleDevuelve(lugar("Burger King", -34.5900, -58.4270, "fast_food_restaurant"));

        service.sync();

        ArgumentCaptor<BurgerJoint> guardado = ArgumentCaptor.forClass(BurgerJoint.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getGooglePrimaryType()).isEqualTo("fast_food_restaurant");
    }

    private BurgerJoint guardado(String nombre, Double lat, Double lon, String barrio) {
        BurgerJoint joint = BurgerJoint.builder()
            .id(1L).placeId("ChIJ-" + nombre).name(nombre).address("Una dirección")
            .area(barrio).latitude(lat).longitude(lon)
            .googlePrimaryType("hamburger_restaurant")
            .build();
        when(repository.findAll()).thenReturn(List.of(joint));
        return joint;
    }

    /**
     * Los locales que ya estaban guardados con el barrio equivocado no se arreglan
     * solos: hay 136 que ninguna búsqueda vuelve a devolver, o que volverían recién
     * dentro de varios meses.
     */
    @Test
    void leCorrigeElBarrioALoQueYaEstabaGuardado() {
        googleDevuelve();
        BurgerJoint mal = guardado("The Burger Company", -34.5900, -58.4270, "Villa Real");

        service.sync();

        assertThat(mal.getArea()).isEqualTo("Palermo");
        verify(repository).save(mal);
    }

    /** Los 29 de Mar del Plata, San Nicolás y el conurbano tienen que salir de la base. */
    @Test
    void borraLoQueQuedoGuardadoFueraDeLaCiudad() {
        googleDevuelve();
        BurgerJoint lejos = guardado("Hamburgo", -37.9619722, -57.5602536, "Constitución");

        service.sync();

        verify(repository).delete(lejos);
    }

    /**
     * Si alguien lo puntuó o lo tiene anotado para ir, se queda igual: su reseña vale
     * más que nuestra idea de qué locales corresponden, y borrarlo se llevaría puesto
     * lo que escribió.
     */
    @Test
    void noBorraUnLocalQueAlguienPuntuo() {
        googleDevuelve();
        BurgerJoint lejos = guardado("Hamburgo", -37.9619722, -57.5602536, "Constitución");
        when(ratingRepository.existsByBurgerJoint_Id(1L)).thenReturn(true);

        service.sync();

        verify(repository, never()).delete(lejos);
    }

    /** Y lo mismo para lo que ya está guardado y resulta ser de otra especialidad. */
    @Test
    void borraLoQueQuedoGuardadoYEsDeOtraEspecialidad() {
        googleDevuelve();
        BurgerJoint pizzeria = guardado("El Camba", -34.5900, -58.4270, "Palermo");
        pizzeria.setGooglePrimaryType("pizza_restaurant");

        service.sync();

        verify(repository).delete(pizzeria);
    }

    private BurgerJoint guardadoEn(long id, String nombre, double lat, double lon, String foto) {
        return BurgerJoint.builder()
            .id(id).placeId("ChIJ-" + id).name(nombre).address("Una dirección").area("Villa Urquiza")
            .latitude(lat).longitude(lon).photoUrl(foto)
            .googlePrimaryType("hamburger_restaurant")
            .build();
    }

    /**
     * Google tiene dos fichas para algunos negocios, con identificadores distintos, y
     * para nosotros son dos locales: lo que evita repetidos es el identificador, y ahí
     * son dos. "24th Street Burger, Av. Triunvirato 4375" aparecía dos veces seguidas
     * en la lista, una con foto y la otra sin.
     */
    @Test
    void borraLaFichaRepetidaYSeQuedaConLaQueTieneFoto() {
        googleDevuelve();
        BurgerJoint conFoto = guardadoEn(469, "24th Street Burger", -34.5765644, -58.4822027,
            "/api/place-photos/a.jpg");
        BurgerJoint sinFoto = guardadoEn(970, "24th Street Burger", -34.5765644, -58.4822027, null);
        when(repository.findAll()).thenReturn(List.of(conFoto, sinFoto));

        service.sync();

        verify(repository).delete(sinFoto);
        verify(repository, never()).delete(conFoto);
    }

    /**
     * Dos locales distintos comparten dirección más seguido de lo que parece: las
     * cocinas que alquilan el mismo espacio son comunes, y en la base hay tres pares
     * así. Sin mirar el nombre nos llevaríamos uno de cada par.
     */
    @Test
    void noBorraDosLocalesDistintosQueCompartenDireccion() {
        googleDevuelve();
        BurgerJoint uno = guardadoEn(226, "Shark Burgers", -34.5765644, -58.4822027, null);
        BurgerJoint otro = guardadoEn(216, "Tempo The Burger shop", -34.5765644, -58.4822027, null);
        when(repository.findAll()).thenReturn(List.of(uno, otro));

        service.sync();

        verify(repository, never()).delete(any());
    }

    /** Juntar dos conjuntos de reseñas no es una decisión que corresponda tomar acá. */
    @Test
    void noBorraUnRepetidoSiLasDosFichasTienenResenias() {
        googleDevuelve();
        BurgerJoint una = guardadoEn(469, "24th Street Burger", -34.5765644, -58.4822027, null);
        BurgerJoint otra = guardadoEn(970, "24th Street Burger", -34.5765644, -58.4822027, null);
        when(repository.findAll()).thenReturn(List.of(una, otra));
        when(ratingRepository.existsByBurgerJoint_Id(469L)).thenReturn(true);
        when(ratingRepository.existsByBurgerJoint_Id(970L)).thenReturn(true);

        service.sync();

        verify(repository, never()).delete(any());
    }

    /** Y si solo una tiene reseñas, se conserva esa aunque la otra tenga foto. */
    @Test
    void anteUnRepetidoConReseniasSeConservaEse() {
        googleDevuelve();
        BurgerJoint conResenias = guardadoEn(469, "24th Street Burger", -34.5765644, -58.4822027, null);
        BurgerJoint conFoto = guardadoEn(970, "24th Street Burger", -34.5765644, -58.4822027,
            "/api/place-photos/a.jpg");
        when(repository.findAll()).thenReturn(List.of(conResenias, conFoto));
        when(ratingRepository.existsByBurgerJoint_Id(469L)).thenReturn(true);

        service.sync();

        verify(repository).delete(conFoto);
        verify(repository, never()).delete(conResenias);
    }

    /**
     * Borrarlo en la limpieza no alcanza: la búsqueda lo trae igual, con el otro
     * identificador, y volvería a entrar en la misma pasada.
     */
    @Test
    void noVuelveACrearUnaFichaRepetidaQueTraeLaBusqueda() {
        BurgerJoint yaEsta = guardadoEn(469, "24th Street Burger", -34.5765644, -58.4822027, null);
        when(repository.findAll()).thenReturn(List.of(yaEsta));
        googleDevuelve(lugar("24th Street Burger", -34.5765644, -58.4822027, "hamburger_restaurant"));

        PlacesSyncReport report = service.sync();

        assertThat(report.created()).isZero();
        verify(repository, never()).save(any());
    }

    /** Pero una sucursal nueva de la misma cadena, en otro barrio, sí entra. */
    @Test
    void unaSucursalEnOtroBarrioSiEntra() {
        BurgerJoint urquiza = guardadoEn(469, "24th Street Burger", -34.5765644, -58.4822027, null);
        when(repository.findAll()).thenReturn(List.of(urquiza));
        googleDevuelve(lugar("24th Street Burger", -34.5946734, -58.4290779, "hamburger_restaurant"));

        assertThat(service.sync().created()).isEqualTo(1);
    }
}
