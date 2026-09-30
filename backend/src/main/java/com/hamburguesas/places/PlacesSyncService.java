package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlacesSyncService {

    private final PlacesProperties properties;
    private final PlacesClient placesClient;
    private final PlacesQuotaGuard quotaGuard;
    private final PhotoStorage photoStorage;
    private final BurgerJointRepository burgerJointRepository;
    private final Barrios barrios;
    private final RatingRepository ratingRepository;
    private final WishlistRepository wishlistRepository;
    private final SavedTourRepository savedTourRepository;
    private final FastFoodMarker fastFoodMarker;

    /**
     * Revisa solo las fotos: completa las que faltan y vuelve a elegir las que se
     * eligieron con una regla vieja. No busca locales nuevos.
     *
     * Existe porque cambiar la regla de elección no cambia ninguna foto por sí solo: lo
     * que hay guardado se queda como está hasta que alguien vuelva a mirarlo. Y la
     * sincronización completa, que es donde vivía esa revisión, gasta además hasta mil
     * búsquedas por los 48 barrios, que es la parte cara y la que acá no hace falta.
     *
     * Cuesta una ficha por local —el tramo gratis es de 5.000 por mes— y una foto solo
     * por los que efectivamente cambian.
     */
    public PlacesSyncReport revisarFotos() {
        if (!properties.hasApiKey()) {
            log.warn("Revisión de fotos salteada: falta la clave de Google");
            return PlacesSyncReport.skipped("Falta configurar GOOGLE_MAPS_API_KEY");
        }

        MissingPhotosResult faltantes = fillMissingPhotos();
        int recambiadas = repickOldPhotos();

        log.info("Revisión de fotos: {} bajadas, {} prestadas de otra sucursal, {} recambiadas",
            faltantes.downloaded(), faltantes.reused(), recambiadas);

        return PlacesSyncReport.soloFotos(
            faltantes.downloaded() + recambiadas, faltantes.reused());
    }

    public PlacesSyncReport sync() {
        if (!properties.hasApiKey()) {
            log.warn("Places sync skipped: no API key configured");
            return PlacesSyncReport.skipped("Falta configurar GOOGLE_MAPS_API_KEY");
        }

        int created = 0;
        int updated = 0;
        int photosDownloaded = 0;
        int descartados = 0;

        // Se arma una vez: son las marcas que Google reconoce como hamburgueserías, y
        // sirven para rescatar a las sucursales que clasificó distinto al resto.
        Set<String> cadenas = cadenasDeHamburguesas();

        LimpiezaResult limpieza = revisarLoGuardado(cadenas);

        // Los que ya están, para no volver a crear una ficha repetida. Borrarlas en la
        // limpieza no alcanzaría: la búsqueda las trae igual, con otro identificador, y
        // volverían a entrar en la misma pasada.
        List<BurgerJoint> yaEstan = new ArrayList<>(burgerJointRepository.findAll());

        for (String area : properties.getSync().getAreas()) {
          for (String plantilla : properties.getSync().getQueryTemplates()) {
            if (!quotaGuard.canCall(PlacesCallType.SEARCH)) {
                log.warn("Monthly search quota reached ({}), stopping sync", quotaGuard.limitFor(PlacesCallType.SEARCH));
                break;
            }

            String consulta = plantilla.replace("{barrio}", area);
            String pageToken = null;
            for (int page = 0; page < properties.getSync().getMaxPagesPerArea(); page++) {
                PlacesSearchResult result;
                try {
                    result = search(consulta, pageToken);
                } catch (RestClientResponseException ex) {
                    log.warn("Places search failed for {} (HTTP {}), stopping sync: {}",
                        consulta, ex.getStatusCode().value(), ex.getMessage());
                    return new PlacesSyncReport(created, updated, photosDownloaded, 0,
                        "Google respondió " + ex.getStatusCode().value() + ", se frenó la sincronización");
                }

                for (PlacesSearchResult.Place place : result.places()) {
                    if (place.placeId() == null || place.name() == null) {
                        continue;
                    }

                    if (noEsUnaHamburgueseria(place, cadenas)) {
                        log.debug("{} no es un lugar donde comer ({}), se descarta",
                            place.name(), place.primaryType());
                        descartados++;
                        continue;
                    }

                    // El barrio sale de las coordenadas, no de la búsqueda que lo trajo:
                    // Google devuelve lo que le parece cerca y se pasa de largo del barrio
                    // que se le pidió. Y si el punto no está en la Ciudad, el local no va:
                    // "hamburguesería en San Nicolás" trae San Nicolás de los Arroyos y
                    // "Versalles" trae uno de Colombia.
                    var barrio = barrios.barrioDe(place.latitude(), place.longitude());
                    if (barrio.isEmpty()) {
                        log.debug("{} queda fuera de la Ciudad ({}), se descarta",
                            place.name(), place.address());
                        descartados++;
                        continue;
                    }

                    var existing = burgerJointRepository.findByPlaceId(place.placeId());
                    boolean gotPhoto;
                    if (existing.isPresent()) {
                        gotPhoto = refresh(existing.get(), place, barrio.get());
                        updated++;
                    } else {
                        // Google tiene dos fichas para algunos negocios, con
                        // identificadores distintos. Sin esto entran las dos.
                        BurgerJoint nuevo = comoJoint(place, barrio.get());
                        BurgerJoint repetido = elMismoLocalEntre(yaEstan, nuevo);
                        if (repetido != null) {
                            log.debug("{} ya está como {}, se descarta la ficha repetida",
                                place.name(), repetido.getName());
                            descartados++;
                            continue;
                        }

                        gotPhoto = create(place, barrio.get());
                        yaEstan.add(nuevo);
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
        }

        MissingPhotosResult missing = fillMissingPhotos();
        photosDownloaded += missing.downloaded();
        photosDownloaded += repickOldPhotos();

        // Los locales que entraron recién no tienen marcado si son de una cadena, y el
        // filtro de Explorar mira esa marca. Sin esto, un McDonald's nuevo se vería
        // igual con las cadenas apagadas hasta el próximo arranque.
        fastFoodMarker.marcar();

        log.info("Places sync finished: {} created, {} updated, {} photos, {} reused, {} descartados, "
            + "{} barrios corregidos, {} borrados",
            created, updated, photosDownloaded, missing.reused(), descartados,
            limpieza.corregidos(), limpieza.borrados());
        return new PlacesSyncReport(created, updated, photosDownloaded, missing.reused(), null);
    }


    /**
     * Revisa lo que ya está guardado: le corrige el barrio y saca lo que no va.
     *
     * Hace falta porque los errores viejos no se arreglan solos. El barrio equivocado
     * sí se corrige al volver a encontrar el local, pero un local que ninguna búsqueda
     * devuelve —los de otras provincias, justamente— se quedaría para siempre. En la
     * base había 29 de Mar del Plata, San Nicolás de los Arroyos, Las Flores, el
     * conurbano, Colombia y México, y 136 con el barrio cambiado.
     *
     * No cuesta ninguna llamada: los límites están en el disco y las coordenadas ya
     * estaban guardadas.
     */
    private LimpiezaResult revisarLoGuardado(Set<String> cadenas) {
        int corregidos = 0;
        int borrados = 0;

        // Los que van quedando, para reconocer al que ya vimos dos veces. Se arma acá y
        // no con una consulta porque comparar nombres y distancias no se escribe bien
        // en una consulta, y son cuatrocientos.
        List<BurgerJoint> quedan = new ArrayList<>();

        for (BurgerJoint joint : burgerJointRepository.findAll()) {
            var barrio = barrios.barrioDe(joint.getLatitude(), joint.getLongitude());
            boolean sobra = barrio.isEmpty() || noEsUnaHamburgueseria(comoLugar(joint), cadenas);

            if (sobra) {
                // Si alguien lo puntuó, lo anotó para ir o lo tiene en un recorrido
                // guardado, se queda: su decisión vale más que nuestra idea de qué
                // locales corresponden.
                if (fueTocadaPorAlguien(joint)) {
                    log.info("{} no corresponde pero alguien lo tiene guardado, se deja", joint.getName());
                    continue;
                }
                log.info("Se borra {} ({}): {}", joint.getName(), joint.getAddress(),
                    barrio.isEmpty() ? "fuera de la Ciudad" : "no es una hamburguesería");
                burgerJointRepository.delete(joint);
                borrados++;
                continue;
            }

            BurgerJoint repetido = elMismoLocalEntre(quedan, joint);
            if (repetido != null) {
                BurgerJoint sobrante = cualSobra(repetido, joint);
                if (sobrante == null) {
                    log.info("{} está dos veces y las dos tienen reseñas, se dejan", joint.getName());
                } else {
                    log.info("Se borra {} ({}): está repetido", sobrante.getName(), sobrante.getAddress());
                    quedan.remove(sobrante);
                    burgerJointRepository.delete(sobrante);
                    borrados++;
                    if (sobrante == repetido) {
                        quedan.add(joint);
                    }
                    continue;
                }
            }
            quedan.add(joint);

            if (!barrio.get().equals(joint.getArea())) {
                joint.setArea(barrio.get());
                burgerJointRepository.save(joint);
                corregidos++;
            }
        }

        return new LimpiezaResult(corregidos, borrados);
    }

    private record LimpiezaResult(int corregidos, int borrados) {}

    /**
     * El local de la lista que es el mismo que este, o null si no está.
     *
     * Google a veces tiene dos fichas para un mismo negocio, con identificadores
     * distintos, y para nosotros son dos locales: lo que evita repetidos es el
     * identificador, y acá son dos. "24th Street Burger, Av. Triunvirato 4375" aparecía
     * dos veces seguidas en la lista, una con foto y la otra sin.
     */
    private static BurgerJoint elMismoLocalEntre(List<BurgerJoint> locales, BurgerJoint joint) {
        return locales.stream()
            .filter(otro -> Duplicados.sonElMismoLocal(otro, joint))
            .findFirst()
            .orElse(null);
    }

    /**
     * Cuál de las dos fichas repetidas hay que borrar, o null si no se puede borrar
     * ninguna porque las dos tienen reseñas. Juntar dos conjuntos de reseñas es una
     * decisión que no corresponde tomar acá, así que en ese caso quedan las dos y se
     * avisa en el registro.
     */
    private BurgerJoint cualSobra(BurgerJoint una, BurgerJoint otra) {
        boolean tocadaUna = fueTocadaPorAlguien(una);
        boolean tocadaOtra = fueTocadaPorAlguien(otra);

        if (tocadaUna && tocadaOtra) {
            return null;
        }
        if (tocadaUna) {
            return otra;
        }
        if (tocadaOtra) {
            return una;
        }
        // Ninguna tiene reseñas: se queda la que tiene foto, que es lo que se ve.
        BurgerJoint mejor = Duplicados.mejorDeLasDos(una, otra);
        return mejor == una ? otra : una;
    }

    /**
     * Si alguien la puntuó, la anotó para ir o la tiene en un recorrido guardado.
     *
     * Nada de eso se borra por decisión nuestra. Un tour guardado al que le falta una
     * parada no se puede rehacer, y quien lo guardó no tiene forma de saber qué le pasó.
     */
    private boolean fueTocadaPorAlguien(BurgerJoint joint) {
        return ratingRepository.existsByBurgerJoint_Id(joint.getId())
            || wishlistRepository.existsByBurgerJoint_Id(joint.getId())
            || savedTourRepository.estaEnAlgunTour(joint.getId());
    }

    /**
     * Un local ya guardado, visto como lo que devolvería una búsqueda, para poder
     * pasarlo por el mismo filtro y que la regla sea una sola.
     */
    private static PlacesSearchResult.Place comoLugar(BurgerJoint joint) {
        return new PlacesSearchResult.Place(
            joint.getPlaceId(), joint.getName(), joint.getAddress(),
            joint.getLatitude(), joint.getLongitude(), null, null, joint.getGooglePrimaryType());
    }
    /**
     * Si lo que devolvió Google no es una hamburguesería.
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
     */
    private boolean noEsUnaHamburgueseria(PlacesSearchResult.Place place, Set<String> cadenas) {
        var sync = properties.getSync();

        if (sync.getExcludedPlaceIds().contains(place.placeId())) {
            return true;
        }
        if (sync.getIncludedPlaceIds().contains(place.placeId())) {
            return false;
        }
        // Rubros que directamente no dan de comer. Va antes que el nombre porque la
        // fábrica de salchichas y el mayorista de medallones tienen "burger" en el
        // nombre, y si no entrarían por esa puerta.
        if (place.primaryType() != null && sync.getExcludedPrimaryTypes().contains(place.primaryType())) {
            return true;
        }

        return !esRubroDeHamburguesas(place.primaryType())
            && !elNombreDiceHamburguesas(place.name())
            && !esSucursalDeUnaCadena(place.name(), cadenas);
    }

    /** Lo que Google llama hamburguesería, y la comida rápida, que son McDonald's y Burger King. */
    private static boolean esRubroDeHamburguesas(String primaryType) {
        return "hamburger_restaurant".equals(primaryType)
            || "fast_food_restaurant".equals(primaryType);
    }

    /** Un local que se llama "algo Burger" u "Hamburguesas algo" está diciendo a qué se dedica. */
    static boolean elNombreDiceHamburguesas(String name) {
        String limpio = sinAcentos(name);
        return limpio.contains("burger") || limpio.contains("hamburgues") || limpio.contains("smash");
    }

    /**
     * Si el nombre empieza con el de una cadena que Google sí marca como hamburguesería.
     *
     * Se exige que sea el principio y que siga un espacio —"La Birra Bar Colegiales"
     * empieza con "La Birra Bar"— y que la marca tenga al menos ocho caracteres, para
     * que un nombre corto no se lleve puesto a cualquiera que empiece parecido.
     */
    static boolean esSucursalDeUnaCadena(String name, Set<String> cadenas) {
        String limpio = sinAcentos(name);
        return cadenas.stream().anyMatch(cadena -> limpio.startsWith(cadena + " "));
    }

    /** Las marcas que Google sí reconoce como hamburgueserías, para el arreglo de arriba. */
    private Set<String> cadenasDeHamburguesas() {
        Set<String> cadenas = new HashSet<>();
        for (String nombre : burgerJointRepository.nombresDeRubro("hamburger_restaurant")) {
            String limpio = sinAcentos(nombre);
            if (limpio.length() >= 8) {
                cadenas.add(limpio);
            }
        }
        return cadenas;
    }

    private static String sinAcentos(String valor) {
        return Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ")
            .trim();
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
            // Solo la ficha, que es lo que hace falta siempre. La foto se pide más
            // abajo y únicamente si Google tiene alguna: son dos cuotas distintas, y
            // pedir las dos acá arriba frenaba todo el trabajo cuando se agotaba la de
            // fotos, incluso averiguar de qué locales no hay ninguna, que no cuesta una
            // sola foto y es lo que decide si se los esconde.
            if (!quotaGuard.canCall(PlacesCallType.DETAILS)) {
                log.warn("Cuota mensual de fichas alcanzada, quedan locales sin revisar");
                break;
            }

            FotoElegida foto;
            try {
                pause();
                foto = placesClient.fotoDe(joint.getPlaceId());
                quotaGuard.record(PlacesCallType.DETAILS);
            } catch (RestClientResponseException ex) {
                log.warn("No se pudo pedir la ficha de {} (HTTP {})",
                    joint.getPlaceId(), ex.getStatusCode().value());
                continue;
            }

            if (foto != null) {
                // Tenía fotos: si venía anotado como que no, se corrige. Un local que
                // recién abrió y todavía no tiene ninguna va a tenerlas más adelante.
                if (joint.isSinFotosEnGoogle()) {
                    joint.setSinFotosEnGoogle(false);
                    burgerJointRepository.save(joint);
                }

                // Tiene fotos pero no hay cuota para bajarlas. No se le presta la de
                // otra sucursal: tener la propia es mejor, y va a estar el mes que viene.
                if (!quotaGuard.canCall(PlacesCallType.PHOTO)) {
                    continue;
                }

                String photoUrl = downloadPhoto(joint.getPlaceId(), foto.name());
                if (photoUrl != null) {
                    joint.setPhotoUrl(photoUrl);
                    joint.setPhotoName(foto.name());
                    joint.setPhotoFingerprint(foto.huella());
                    joint.setPhotoRule(PlacesClient.REGLA_DE_FOTO);
                    burgerJointRepository.save(joint);
                    fotoPorCadena.putIfAbsent(chainKey(joint.getName()), photoUrl);
                    downloaded++;
                    continue;
                }
            } else {
                // Google no tiene ni una foto de este local. Queda anotado porque es lo
                // único que distingue "no hay nada" de "no fuimos a buscarlo", y la
                // diferencia decide si se lo esconde o se lo completa.
                joint.setSinFotosEnGoogle(true);
                burgerJointRepository.save(joint);
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

    /**
      * Revisa las fotos que se bajaron con la regla de elección vieja.
      *
      * Esa regla se quedaba con la primera foto del local y, si no había ninguna, con
      * la primera de todas. Sobre una muestra de 150 locales, la regla nueva elige
      * otra foto en 47: casi siempre una apaisada en lugar de una vertical, que en las
      * tarjetas —recortadas a 4:3— se veía cortada al medio.
      *
      * Va al final, después de las que no tienen ninguna foto: conseguir la primera
      * foto de un local importa más que mejorar una que ya está. Y solo mira las que
      * no tienen nombre anotado, así el trabajo se hace una vez y no en cada
      * sincronización.
      */
     private int repickOldPhotos() {
         int cambiadas = 0;

         for (BurgerJoint joint : burgerJointRepository.conFotoElegidaConUnaReglaVieja(PlacesClient.REGLA_DE_FOTO)) {
             // Igual que arriba: acá solo hace falta la ficha. La mayoría de las fotos
             // no cambia de una regla a la otra, y esas se revisan sin bajar nada; pedir
             // también la cuota de fotos frenaba a todas por las pocas que sí cambian.
             if (!quotaGuard.canCall(PlacesCallType.DETAILS)) {
                 log.info("Cuota mensual de fichas alcanzada, quedan fotos por revisar");
                 break;
             }

             FotoElegida mejor;
             try {
                 pause();
                 mejor = placesClient.fotoDe(joint.getPlaceId());
                 quotaGuard.record(PlacesCallType.DETAILS);
             } catch (RestClientResponseException ex) {
                 log.warn("No se pudo revisar la foto de {} (HTTP {})",
                     joint.getPlaceId(), ex.getStatusCode().value());
                 continue;
             }

             if (mejor == null) {
                 continue;
             }

             // La regla nueva eligió la misma foto que ya tenemos: alcanza con anotar
             // que está revisada. Bajarla de nuevo sería pagarle a Google por el mismo
             // archivo, y la mayoría de las fotos no cambia de una regla a la otra.
             //
             // Se compara por la huella y no por el nombre. El nombre cambia en cada
             // pedido, así que esta comparación nunca daba verdadero y la revisión se
             // bajaba las cuatrocientas fotos siempre, gastando el tramo gratuito de un
             // mes entero en archivos que ya estaban en disco.
             //
             // Los locales que no tienen huella guardada son los de antes de esta
             // columna: caen del lado de bajarla, una única vez, y quedan con huella.
             if (mejor.huella().equals(joint.getPhotoFingerprint())) {
                 joint.setPhotoRule(PlacesClient.REGLA_DE_FOTO);
                 burgerJointRepository.save(joint);
                 continue;
             }

             // La regla nueva eligió otra, pero no hay cuota para bajarla. Se deja sin
             // anotar la regla, así vuelve a caer en esta lista el mes que viene.
             if (!quotaGuard.canCall(PlacesCallType.PHOTO)) {
                 continue;
             }

             String photoUrl = downloadPhoto(joint.getPlaceId(), mejor.name());
             if (photoUrl != null) {
                 joint.setPhotoUrl(photoUrl);
                 joint.setPhotoName(mejor.name());
                 joint.setPhotoFingerprint(mejor.huella());
                 joint.setPhotoRule(PlacesClient.REGLA_DE_FOTO);
                 burgerJointRepository.save(joint);
                 cambiadas++;
             }
         }

         return cambiadas;
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

    private PlacesSearchResult search(String consulta, String pageToken) {
        pause();
        PlacesSearchResult result = placesClient.searchText(consulta, pageToken);
        quotaGuard.record(PlacesCallType.SEARCH);
        return result;
    }

    /** El local tal como quedaría guardado, para poder compararlo antes de guardarlo. */
    private static BurgerJoint comoJoint(PlacesSearchResult.Place place, String area) {
        return BurgerJoint.builder()
            .placeId(place.placeId())
            .name(place.name())
            .googlePrimaryType(place.primaryType())
            .address(place.address() != null ? place.address() : area)
            .area(area)
            .latitude(place.latitude())
            .longitude(place.longitude())
            .lastSyncedAt(Instant.now())
            .build();
    }

    private boolean create(PlacesSearchResult.Place place, String area) {
        BurgerJoint joint = comoJoint(place, area);

        boolean gotPhoto = false;
        if (place.photoName() != null && quotaGuard.canCall(PlacesCallType.PHOTO)) {
            String photoUrl = downloadPhoto(place);
            if (photoUrl != null) {
                joint.setPhotoUrl(photoUrl);
                joint.setPhotoName(place.photoName());
                joint.setPhotoFingerprint(place.photoFingerprint());
                joint.setPhotoRule(PlacesClient.REGLA_DE_FOTO);
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
        joint.setGooglePrimaryType(place.primaryType());
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
                joint.setPhotoName(place.photoName());
                joint.setPhotoFingerprint(place.photoFingerprint());
                joint.setPhotoRule(PlacesClient.REGLA_DE_FOTO);
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
