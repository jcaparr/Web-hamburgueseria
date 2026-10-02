package com.hamburguesas.places;

import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.FranjaHorariaRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.WishlistRepository;

import static org.mockito.Mockito.mock;

/**
 * Arma la sincronización con sus partes, como lo haría Spring.
 *
 * Los tests le pasan las piezas de afuera —el cliente de Google, la cuota, la base— y
 * casi siempre de mentira. Las partes de adentro se arman acá, de verdad: lo que se
 * prueba es cómo trabajan juntas, y reemplazarlas también dejaría de probar eso.
 */
final class ServicioArmado {

    private ServicioArmado() {
    }

    static PlacesSyncService armar(
        PlacesProperties properties, PlacesClient placesClient, PlacesQuotaGuard quotaGuard,
        PhotoStorage photoStorage, BurgerJointRepository repository, Zonas zonas,
        RatingRepository ratings, WishlistRepository wishlist, SavedTourRepository tours,
        FastFoodMarker marcador
    ) {
        LlamadasAGoogle google = new LlamadasAGoogle(placesClient, quotaGuard, properties);
        ClasificadorDeLocales clasificador = new ClasificadorDeLocales(properties, google, repository);
        LimpiezaDeLocales limpieza = new LimpiezaDeLocales(
            repository, zonas, clasificador, ratings, wishlist, tours);
        FotosDeLocales fotos = new FotosDeLocales(properties, google, photoStorage, repository);
        // Ningún test de la sincronización mira los horarios: tienen los suyos.
        HorariosDeLocales horarios =
            new HorariosDeLocales(google, repository, mock(FranjaHorariaRepository.class));

        return new PlacesSyncService(properties, google, clasificador, limpieza, fotos,
            horarios, zonas, repository, marcador);
    }
}
