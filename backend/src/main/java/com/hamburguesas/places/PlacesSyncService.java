package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.BurgerJointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

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
                    return new PlacesSyncReport(created, updated, photosDownloaded,
                        "Google respondió " + ex.getStatusCode().value() + ", se frenó la sincronización");
                }

                for (PlacesSearchResult.Place place : result.places()) {
                    if (place.placeId() == null || place.name() == null) {
                        continue;
                    }

                    var existing = burgerJointRepository.findByPlaceId(place.placeId());
                    if (existing.isPresent()) {
                        refresh(existing.get(), place, area);
                        updated++;
                    } else {
                        boolean gotPhoto = create(place, area);
                        created++;
                        if (gotPhoto) {
                            photosDownloaded++;
                        }
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

        log.info("Places sync finished: {} created, {} updated, {} photos", created, updated, photosDownloaded);
        return new PlacesSyncReport(created, updated, photosDownloaded, null);
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

    /** Photos are only fetched for joints we have never seen, which is what keeps us inside the free tier. */
    private String downloadPhoto(PlacesSearchResult.Place place) {
        try {
            pause();
            byte[] bytes = placesClient.downloadPhoto(place.photoName());
            quotaGuard.record(PlacesCallType.PHOTO);
            return photoStorage.save(place.placeId(), bytes);
        } catch (RestClientResponseException ex) {
            log.warn("Could not download photo for {} (HTTP {})", place.placeId(), ex.getStatusCode().value());
            return null;
        }
    }

    private void refresh(BurgerJoint joint, PlacesSearchResult.Place place, String area) {
        joint.setName(place.name());
        if (place.address() != null) {
            joint.setAddress(place.address());
        }
        joint.setArea(area);
        joint.setLatitude(place.latitude());
        joint.setLongitude(place.longitude());
        joint.setLastSyncedAt(Instant.now());
        burgerJointRepository.save(joint);
    }

    private void pause() {
        try {
            Thread.sleep(properties.getSync().getDelayBetweenCallsMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
