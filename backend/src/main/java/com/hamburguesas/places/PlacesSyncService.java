package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.BurgerJointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.text.Normalizer;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlacesSyncService {

    private final PlacesProperties properties;
    private final PlacesClient placesClient;
    private final PlacesQuotaGuard quotaGuard;
    private final PhotoStorage photoStorage;
    private final BurgerJointRepository burgerJointRepository;

    public PlacesSyncReport sync() {
        if (!properties.hasApiKey()) {
            log.warn("Places sync skipped: no API key configured");
            return PlacesSyncReport.skipped("Falta configurar GOOGLE_MAPS_API_KEY");
        }

        int created = 0;
        int updated = 0;
        int photosDownloaded = 0;

        for (String area : properties.getSync().getAreas()) {
            if (!quotaGuard.canCall(PlacesCallType.SEARCH)) {
                log.warn("Monthly search quota reached ({}), stopping sync", quotaGuard.limitFor(PlacesCallType.SEARCH));
                break;
            }

            String pageToken = null;
            for (int page = 0; page < properties.getSync().getMaxPagesPerArea(); page++) {
                PlacesSearchResult result;
                try {
                    result = search(area, pageToken);
                } catch (RestClientResponseException ex) {
                    log.warn("Places search failed for {} (HTTP {}), stopping sync: {}",
                        area, ex.getStatusCode().value(), ex.getMessage());
                    return new PlacesSyncReport(created, updated, photosDownloaded, 0,
                        "Google respondió " + ex.getStatusCode().value() + ", se frenó la sincronización");
                }

                for (PlacesSearchResult.Place place : result.places()) {
                    if (place.placeId() == null || place.name() == null) {
                        continue;
                    }

                    var existing = burgerJointRepository.findByPlaceId(place.placeId());
                    boolean gotPhoto;
                    if (existing.isPresent()) {
                        gotPhoto = refresh(existing.get(), place, area);
                        updated++;
                    } else {
                        gotPhoto = create(place, area);
                        created++;
                    }
                    if (gotPhoto) {
                        photosDownloaded++;
                    }
                }

                pageToken = result.nextPageToken();
                if (pageToken == null || pageToken.isBlank()) {
                    break;
                }
                if (!quotaGuard.canCall(PlacesCallType.SEARCH)) {
                    break;
                }
            }
        }

        MissingPhotosResult missing = fillMissingPhotos();
        photosDownloaded += missing.downloaded();

        log.info("Places sync finished: {} created, {} updated, {} photos, {} reused",
            created, updated, photosDownloaded, missing.reused());
        return new PlacesSyncReport(created, updated, photosDownloaded, missing.reused(), null);
    }

    /**
     * Completa las fotos de los locales que las búsquedas por barrio no devuelven.
     *
     * Hasta acá la foto llegaba de arriba: se bajaba la de los locales que aparecían
     * en una búsqueda. Pero un local puede estar en la base y no aparecer en ninguna:
     * los que Google no clasifica como hamburguesería —Burger King, por ejemplo, que
     * figura como comida rápida— quedan afuera del filtro estricto, y también queda
     * afuera cualquiera que no entre en los 60 resultados de su barrio. Esos se
     * quedaban sin foto para siempre.
     *
     * Acá se les pide la ficha por su place_id, que es una llamada aparte y con su
     * propio límite gratuito.
     */
    private record MissingPhotosResult(int downloaded, int reused) {}

    private MissingPhotosResult fillMissingPhotos() {
        int downloaded = 0;
        int reused = 0;
        Map<String, String> fotoPorCadena = photosByChain();

        for (BurgerJoint joint : burgerJointRepository.findByPhotoUrlIsNull()) {
            if (!quotaGuard.canCall(PlacesCallType.PHOTO) || !quotaGuard.canCall(PlacesCallType.DETAILS)) {
                log.warn("Cuota mensual alcanzada, quedan locales sin foto para el mes que viene");
                break;
            }

            String photoName;
            try {
                pause();
                photoName = placesClient.photoNameFor(joint.getPlaceId());
                quotaGuard.record(PlacesCallType.DETAILS);
            } catch (RestClientResponseException ex) {
                log.warn("No se pudo pedir la ficha de {} (HTTP {})",
                    joint.getPlaceId(), ex.getStatusCode().value());
                continue;
            }

            if (photoName != null) {
                String photoUrl = downloadPhoto(joint.getPlaceId(), photoName);
                if (photoUrl != null) {
                    joint.setPhotoUrl(photoUrl);
                    burgerJointRepository.save(joint);
                    fotoPorCadena.putIfAbsent(chainKey(joint.getName()), photoUrl);
                    downloaded++;
                    continue;
                }
            }

            // Google no tiene fotos de esta dirección. Si es una sucursal de una cadena
            // que sí tiene, se usa la de la hermana: preferimos el frente de otro local
            // de la misma marca antes que un recuadro con iniciales. Se intenta recién
            // acá, después de Google, porque la foto propia de la sucursal siempre es
            // mejor que la prestada. No cuesta ninguna llamada.
            String prestada = fotoPorCadena.get(chainKey(joint.getName()));
            if (prestada != null) {
                joint.setPhotoUrl(prestada);
                burgerJointRepository.save(joint);
                reused++;
            }
        }

        return new MissingPhotosResult(downloaded, reused);
    }

    /** Una foto por cadena, para prestársela a las sucursales que no tengan. */
    private Map<String, String> photosByChain() {
        Map<String, String> porCadena = new HashMap<>();
        for (BurgerJoint joint : burgerJointRepository.findByPhotoUrlIsNotNull()) {
            porCadena.putIfAbsent(chainKey(joint.getName()), joint.getPhotoUrl());
        }
        return porCadena;
    }

    /**
     * El nombre de la cadena detrás del nombre del local.
     *
     * Las sucursales se escriben de varias formas: "Burger King" repetido tal cual,
     * o "Dean & Dennys - Palermo Soho" y "Dean & Dennys - Barrio Norte". Se corta en
     * el guión y se normaliza mayúsculas y acentos, porque la misma cadena aparece
     * escrita distinto según quién la cargó en Google.
     *
     * Se exige igualdad y no parecido: con nombres cortos y genéricos —"Heaven",
     * "Rubi"— cualquier coincidencia parcial terminaría pegándole la foto de un local
     * que no tiene nada que ver.
     */
    static String chainKey(String name) {
        String sinSucursal = name.split(" - ")[0];
        String sinAcentos = Normalizer.normalize(sinSucursal, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private PlacesSearchResult search(String area, String pageToken) {
        pause();
        PlacesSearchResult result = placesClient.searchText("hamburguesería en " + area + ", Buenos Aires", pageToken);
        quotaGuard.record(PlacesCallType.SEARCH);
        return result;
    }

    private boolean create(PlacesSearchResult.Place place, String area) {
        BurgerJoint joint = BurgerJoint.builder()
            .placeId(place.placeId())
            .name(place.name())
            .address(place.address() != null ? place.address() : area)
            .area(area)
            .latitude(place.latitude())
            .longitude(place.longitude())
            .lastSyncedAt(Instant.now())
            .build();

        boolean gotPhoto = false;
        if (place.photoName() != null && quotaGuard.canCall(PlacesCallType.PHOTO)) {
            String photoUrl = downloadPhoto(place);
            if (photoUrl != null) {
                joint.setPhotoUrl(photoUrl);
                gotPhoto = true;
            }
        }

        burgerJointRepository.save(joint);
        return gotPhoto;
    }

    /**
     * Una foto por local y para siempre: se baja una sola vez, se guarda en disco y
     * no se vuelve a pedir. Es lo que mantiene el gasto en una descarga por local en
     * vez de una por visita.
     */
    private String downloadPhoto(PlacesSearchResult.Place place) {
        return downloadPhoto(place.placeId(), place.photoName());
    }

    private String downloadPhoto(String placeId, String photoName) {
        try {
            pause();
            byte[] bytes = placesClient.downloadPhoto(photoName);
            quotaGuard.record(PlacesCallType.PHOTO);
            return photoStorage.save(placeId, bytes);
        } catch (RestClientResponseException ex) {
            log.warn("Could not download photo for {} (HTTP {})", placeId, ex.getStatusCode().value());
            return null;
        }
    }

    /** @return true si en esta pasada se le consiguió la foto que le faltaba. */
    private boolean refresh(BurgerJoint joint, PlacesSearchResult.Place place, String area) {
        joint.setName(place.name());
        if (place.address() != null) {
            joint.setAddress(place.address());
        }
        joint.setArea(area);
        joint.setLatitude(place.latitude());
        joint.setLongitude(place.longitude());
        joint.setLastSyncedAt(Instant.now());

        // Los locales cargados antes se quedaron sin foto: al principio Google no las
        // devolvía —el proyecto no tenía facturación y las omitía de la respuesta— y
        // además solo se pedían al crear el local, así que nadie volvía a intentarlo.
        // La búsqueda ya trae el dato, así que completarlas no cuesta llamadas extra
        // más allá de la descarga, y el tope mensual de fotos las reparte entre varias
        // sincronizaciones si hacen falta.
        boolean gotPhoto = false;
        if (joint.getPhotoUrl() == null && place.photoName() != null
            && quotaGuard.canCall(PlacesCallType.PHOTO)) {
            String photoUrl = downloadPhoto(place);
            if (photoUrl != null) {
                joint.setPhotoUrl(photoUrl);
                gotPhoto = true;
            }
        }

        burgerJointRepository.save(joint);
        return gotPhoto;
    }

    private void pause() {
        try {
            Thread.sleep(properties.getSync().getDelayBetweenCallsMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
