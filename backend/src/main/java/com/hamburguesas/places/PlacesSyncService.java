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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
    private final Zonas zonas;
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

    /**
     * Pregunta, sin bajar nada, de qué locales Google no tiene ninguna foto.
     *
     * Las dos cuotas son muy desparejas: preguntar sale una ficha —cuatro mil gratis por
     * mes— y bajar sale una foto, de las que hay mil. Con mil doscientos locales sin
     * portada, bajar mientras se pregunta gasta la cuota chica entera en el orden en que
     * los locales aparecen en la base, y parte de esa cuota se va en locales que la
     * limpieza va a borrar igual porque Google no tiene ni una foto de ellos.
     *
     * Así que esto es la mitad barata de la revisión de fotos: deja a cada local anotado
     * con lo que Google contestó y dice cuántos quedan realmente en pie. Con ese número
     * se decide si las mil fotos del mes alcanzan.
     *
     * Tampoco presta la foto de otra sucursal de la cadena, por más que no cueste
     * llamadas: eso le pone portada a un local y lo saca de la lista de los que no
     * tienen, que es justo lo que se está tratando de contar.
     */
    public CensoDeFichas revisarFichas() {
        if (!properties.hasApiKey()) {
            log.warn("Censo de fichas salteado: falta la clave de Google");
            return CensoDeFichas.skipped("Falta configurar GOOGLE_MAPS_API_KEY");
        }

        int preguntados = 0;
        int sinNingunaFoto = 0;
        int sinPreguntar = 0;

        // Los que nunca preguntamos, y no solo los que no tienen portada. Un local puede
        // tener una foto puesta y tener apenas tres en Google, que es justamente lo que
        // hay que poder ver para decidir si corresponde que esté en la lista.
        //
        // Vienen ordenados por sospecha: primero los que Google no clasifica como
        // hamburguesería, al final las cadenas. Preguntar cuesta una ficha por local y el
        // tramo del mes es finito, así que el orden decide qué se alcanza a saber.
        //
        // Y como se eligen por no tener el número anotado, cada corrida sigue donde quedó
        // la anterior sin volver a preguntar por los mismos.
        List<BurgerJoint> sinFoto = burgerJointRepository.sinSaberCuantasFotosTiene();
        for (BurgerJoint joint : sinFoto) {
            if (!quotaGuard.canCall(PlacesCallType.DETAILS)) {
                sinPreguntar = sinFoto.size() - preguntados;
                log.warn("Cuota mensual de fichas alcanzada, quedan {} locales sin preguntar",
                    sinPreguntar);
                break;
            }

            List<FotoElegida> candidatas;
            try {
                pause();
                candidatas = placesClient.fotosDe(joint.getPlaceId());
                quotaGuard.record(PlacesCallType.DETAILS);
            } catch (RestClientResponseException ex) {
                log.warn("No se pudo pedir la ficha de {} (HTTP {})",
                    joint.getPlaceId(), ex.getStatusCode().value());
                continue;
            }

            preguntados++;
            boolean sinNada = candidatas.isEmpty();
            if (sinNada) {
                sinNingunaFoto++;
            }

            // Cuántas tiene, que es lo que distingue un local que la gente fotografía de
            // uno por el que nadie pasó. Viene gratis en la misma respuesta.
            boolean cambioLaCuenta = !Integer.valueOf(candidatas.size()).equals(joint.getFotosEnGoogle());

            // Se guarda en los dos sentidos: un local que recién abrió puede no tener
            // ninguna hoy y tener diez el mes que viene, y la anotación vieja lo borraría.
            if (joint.isSinFotosEnGoogle() != sinNada || cambioLaCuenta) {
                joint.setSinFotosEnGoogle(sinNada);
                joint.setFotosEnGoogle(candidatas.size());
                burgerJointRepository.save(joint);
            }
        }

        log.info("Censo de fichas: {} preguntados, {} sin ninguna foto, {} con fotos, {} sin preguntar",
            preguntados, sinNingunaFoto, preguntados - sinNingunaFoto, sinPreguntar);

        return new CensoDeFichas(
            preguntados, sinNingunaFoto, preguntados - sinNingunaFoto, sinPreguntar, null);
    }

    /**
     * Qué pasó al intentar agregar un local a mano.
     *
     * Lleva el identificador porque es lo que hay que anotar después en la configuración
     * para que la limpieza no lo borre: un local que se agrega a mano suele ser
     * justamente uno que ninguna regla reconoce.
     */
    public record LocalAgregado(String placeId, String nombre, String direccion,
                                String zona, String resultado) {}

    /**
     * Busca un local por su nombre y lo agrega, sin esperar al barrido.
     *
     * Hay hamburgueserías que el barrido no encuentra nunca. "Austin's Diner & Grill" es
     * el caso de manual: Google no le pone el rubro de hamburguesería, así que la
     * búsqueda estricta por barrio no lo devuelve, por más veces que se corra.
     *
     * Entra sin pasar por el clasificador, a propósito: lo agrega una persona que ya
     * sabe que el local existe y vende hamburguesas, y esa decisión vale más que
     * cualquier regla nuestra. Lo que sí se respeta es el radio: un local fuera de los
     * 75 km no entra, porque la app no lo podría ubicar en ninguna zona.
     *
     * Cuesta una búsqueda. No baja la foto: la cuota de fotos es el tramo más chico y
     * esto se usa de a uno, así que la portada la completa la próxima pasada de fotos.
     */
    public LocalAgregado agregar(String texto) {
        if (!properties.hasApiKey()) {
            return new LocalAgregado(null, texto, null, null,
                "Falta configurar GOOGLE_MAPS_API_KEY");
        }
        if (!quotaGuard.canCall(PlacesCallType.SEARCH)) {
            return new LocalAgregado(null, texto, null, null,
                "Se acabó la cuota mensual de búsquedas");
        }

        PlacesSearchResult resultado;
        try {
            // Sin exigir el rubro de hamburguesería. El barrido sí lo exige, y por eso
            // mismo no encuentra estos locales: buscando "Austin's Diner & Grill" con el
            // filtro puesto, Google contesta con otro restaurante de Palermo.
            resultado = buscarPorNombre(texto);
        } catch (RestClientResponseException ex) {
            return new LocalAgregado(null, texto, null, null,
                "Google respondió " + ex.getStatusCode().value());
        }

        if (resultado.places().isEmpty()) {
            return new LocalAgregado(null, texto, null, null, "Google no encontró nada");
        }

        PlacesSearchResult.Place place = resultado.places().get(0);
        if (place.placeId() == null || place.name() == null) {
            return new LocalAgregado(null, texto, null, null, "La ficha vino incompleta");
        }

        var yaEsta = burgerJointRepository.findByPlaceId(place.placeId());
        if (yaEsta.isPresent()) {
            BurgerJoint joint = yaEsta.get();
            return new LocalAgregado(joint.getPlaceId(), joint.getName(), joint.getAddress(),
                joint.getArea(), "Ya estaba");
        }

        var zona = zonas.zonaDe(place.latitude(), place.longitude(), place.address());
        if (zona.isEmpty()) {
            return new LocalAgregado(place.placeId(), place.name(), place.address(), null,
                "Queda fuera del radio de búsqueda");
        }

        create(place, zona.get(), false);
        fastFoodMarker.marcar();

        log.info("Agregado a mano: {} ({}) — {}", place.name(), zona.get(), place.placeId());
        return new LocalAgregado(place.placeId(), place.name(), place.address(), zona.get(),
            "Agregado");
    }

    public PlacesSyncReport sync() {
        return sync(true);
    }

    /**
     * Cuántas consultas seguidas puede fallar Google antes de que dejemos de insistir.
     *
     * Una suelta es un hipo del servicio y saltearla cuesta una zona incompleta. Diez
     * seguidas es que Google no está, y seguir recorriendo las 89 zonas para recibir 89
     * errores no ayuda a nadie.
     */
    private static final int FALLAS_PARA_RENDIRSE = 10;

    /** Lo que no se arregla reintentando: nos pasamos del ritmo, o la clave no sirve. */
    private static boolean esDefinitivo(int codigo) {
        return codigo == 429 || codigo == 401 || codigo == 403;
    }

    /**
     * El barrido completo, con la opción de no tocar ni una foto.
     *
     * Sin fotos existe porque las dos cuotas son muy desparejas —cuatro mil búsquedas por
     * mes contra mil fotos— y recorrer las 89 zonas sale unas mil doscientas búsquedas,
     * mientras que ponerle portada a todo lo que entra no alcanza ni de cerca. Separarlas
     * deja traer los locales ahora y decidir después a quién se le gasta una foto, con el
     * censo de fichas ya hecho y los que Google no tiene fotografiados ya borrados.
     *
     * @param conFotos false para no bajar ninguna: ni al crear, ni al completar, ni al
     *                 recambiar las elegidas con una regla vieja
     */
    public PlacesSyncReport sync(boolean conFotos) {
        if (!properties.hasApiKey()) {
            log.warn("Places sync skipped: no API key configured");
            return PlacesSyncReport.skipped("Falta configurar GOOGLE_MAPS_API_KEY");
        }

        int created = 0;
        int updated = 0;
        int photosDownloaded = 0;
        int descartados = 0;
        // Consultas que Google no contestó y se saltearon, y cuántas van seguidas. Si se
        // encadenan es que Google está caído y no tiene sentido recorrer lo que falta.
        int saltadas = 0;
        int fallasSeguidas = 0;

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
                    fallasSeguidas = 0;
                } catch (RestClientResponseException ex) {
                    int codigo = ex.getStatusCode().value();

                    // Un 429 o un 403 no se arreglan reintentando: o nos pasamos del
                    // ritmo o la clave no sirve, y en los dos casos seguir es gastar
                    // llamadas que van a fallar igual.
                    if (esDefinitivo(codigo)) {
                        log.warn("Google respondió {} en {}, se frena el barrido", codigo, consulta);
                        return new PlacesSyncReport(created, updated, photosDownloaded, 0,
                            "Google respondió " + codigo + ", se frenó la sincronización");
                    }

                    // Un 503 sí: es Google que no está disponible por un rato. Antes se
                    // llevaba puesto el barrido entero, y como las zonas se recorren en
                    // orden, siempre moría en la misma mitad: las últimas —todo el
                    // corredor norte y La Plata— no entraron nunca.
                    fallasSeguidas++;
                    if (fallasSeguidas >= FALLAS_PARA_RENDIRSE) {
                        log.warn("Google falló {} veces seguidas, se frena el barrido", fallasSeguidas);
                        return new PlacesSyncReport(created, updated, photosDownloaded, 0,
                            "Google falló " + fallasSeguidas + " veces seguidas, se frenó la sincronización");
                    }

                    log.warn("Google respondió {} en {}, se saltea esta consulta ({} seguidas)",
                        codigo, consulta, fallasSeguidas);
                    saltadas++;
                    break;
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
                    var barrio = zonas.zonaDe(place.latitude(), place.longitude(), place.address());
                    if (barrio.isEmpty()) {
                        log.debug("{} queda fuera de la Ciudad ({}), se descarta",
                            place.name(), place.address());
                        descartados++;
                        continue;
                    }

                    var existing = burgerJointRepository.findByPlaceId(place.placeId());
                    boolean gotPhoto;
                    if (existing.isPresent()) {
                        gotPhoto = refresh(existing.get(), place, barrio.get(), conFotos);
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

                        gotPhoto = create(place, barrio.get(), conFotos);
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

        // Las dos pasadas de fotos van juntas en el interruptor: completar las que faltan
        // y recambiar las viejas gastan la misma cuota chica, y la idea de apagarlas es
        // no gastarla hasta saber a quién conviene.
        MissingPhotosResult missing = conFotos ? fillMissingPhotos() : new MissingPhotosResult(0, 0);
        photosDownloaded += missing.downloaded();
        if (conFotos) {
            photosDownloaded += repickOldPhotos();
        }

        // Los locales que entraron recién no tienen marcado si son de una cadena, y el
        // filtro de Explorar mira esa marca. Sin esto, un McDonald's nuevo se vería
        // igual con las cadenas apagadas hasta el próximo arranque.
        fastFoodMarker.marcar();

        log.info("Places sync finished: {} created, {} updated, {} photos, {} reused, {} descartados, "
            + "{} barrios corregidos, {} borrados, {} consultas salteadas",
            created, updated, photosDownloaded, missing.reused(), descartados,
            limpieza.corregidos(), limpieza.borrados(), saltadas);

        // Terminó de recorrer todo, pero avisando si algo quedó sin preguntar: una zona
        // con menos locales de los que debería tener se explica por acá.
        String aviso = saltadas == 0 ? null
            : saltadas + " consultas quedaron sin respuesta de Google y se saltearon";
        return new PlacesSyncReport(created, updated, photosDownloaded, missing.reused(), aviso);
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
            var barrio = zonas.zonaDe(joint.getLatitude(), joint.getLongitude(), joint.getAddress());

            // De un local del que Google no tiene ni una foto no hay nada que mostrar:
            // la tarjeta queda con un recuadro de iniciales y nadie entra a mirarlo. Se
            // anota en la revisión de fotos, y solo cuando Google contestó que no tiene
            // ninguna: no tener la foto bajada es otra cosa, y es problema nuestro.
            boolean sinNadaQueMostrar = joint.isSinFotosEnGoogle();

            // Borrar es definitivo, así que no alcanza con que el veredicto diga que no:
            // tiene que decir por qué. Si no se pudo preguntar porque se acabó la cuota,
            // el local se queda. Un límite nuestro no puede terminar en borrarle la
            // hamburguesería a alguien, y el mes que viene se vuelve a mirar.
            Veredicto veredicto = evaluar(comoLugar(joint), cadenas);
            boolean noSabemos = veredicto.prueba() == Veredicto.Prueba.NO_SE_PUDO_PREGUNTAR;

            boolean sobra = barrio.isEmpty()
                || sinNadaQueMostrar
                || casiSinFotosYNoEsHamburgueseria(joint)
                || (!veredicto.vendeHamburguesas() && !noSabemos);

            if (sobra) {
                // Si alguien lo puntuó, lo anotó para ir o lo tiene en un recorrido
                // guardado, se queda: su decisión vale más que nuestra idea de qué
                // locales corresponden.
                if (fueTocadaPorAlguien(joint)) {
                    log.info("{} no corresponde pero alguien lo tiene guardado, se deja", joint.getName());
                    continue;
                }
                log.info("Se borra {} ({}): {}", joint.getName(), joint.getAddress(),
                    barrio.isEmpty() ? "fuera del radio de búsqueda"
                        : sinNadaQueMostrar ? "Google no tiene ninguna foto"
                        : "no es una hamburguesería");
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
    private boolean casiSinFotosYNoEsHamburgueseria(BurgerJoint joint) {
        int minimas = properties.getSync().getFotosMinimasSiNoEsHamburgueseria();
        return minimas > 0
            && !joint.isFastFood()
            && joint.getFotosEnGoogle() != null
            && joint.getFotosEnGoogle() < minimas
            && !"hamburger_restaurant".equals(joint.getGooglePrimaryType());
    }

    public record LimpiezaResult(int corregidos, int borrados) {}

    /**
     * Revisa lo guardado sin salir a buscar nada nuevo.
     *
     * Es la primera mitad del barrido, suelta. Borra los locales que ya no corresponden
     * —los que quedaron fuera del radio, los repetidos y los que Google no tiene
     * fotografiados— y les recalcula la zona a los demás.
     *
     * Existe porque esa mitad no cuesta ninguna búsqueda y la otra cuesta mil novecientas.
     * Cuando lo que hace falta es aplicar lo que un censo de fichas ya averiguó, correr
     * el barrido entero es pagar la parte cara para ejecutar la gratis.
     *
     * Lo único que puede gastar es algún resumen de reseñas, y solo por los locales que
     * ninguna prueba barata alcanza a resolver.
     */
    public LimpiezaResult limpiar() {
        LimpiezaResult resultado = revisarLoGuardado(cadenasDeHamburguesas());

        // Igual que al final del barrido: los que quedan tienen que estar bien marcados
        // como cadena o no, que es lo que mira el filtro de Explorar.
        fastFoodMarker.marcar();

        log.info("Limpieza: {} borrados, {} zonas corregidas",
            resultado.borrados(), resultado.corregidos());
        return resultado;
    }

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
            joint.getLatitude(), joint.getLongitude(), null, null, joint.getGooglePrimaryType(),
            joint.getGooglePrimaryType() == null ? java.util.Set.of() : java.util.Set.of(joint.getGooglePrimaryType()));
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
        return !evaluar(place, cadenas).vendeHamburguesas();
    }

    /**
     * Qué se decidió sobre un local y con qué prueba.
     *
     * Devuelve el veredicto entero y no un sí o un no porque la limpieza necesita
     * distinguir por qué: borrar es definitivo y no todas las razones alcanzan.
     */
    private Veredicto evaluar(PlacesSearchResult.Place place, Set<String> cadenas) {
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
            if (!quotaGuard.canCall(PlacesCallType.RESUMEN)) {
                // Se acabó la cuota. No sabemos, y no saber no es lo mismo que saber que
                // no: queda anotado para que la limpieza no lo borre por un límite
                // nuestro.
                return Veredicto.no(Veredicto.Prueba.NO_SE_PUDO_PREGUNTAR);
            }

            String resumen = resumenDeResenias(place.placeId());
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

    private String resumenDeResenias(String placeId) {
        try {
            pause();
            String resumen = placesClient.resumenDeResenias(placeId);
            quotaGuard.record(PlacesCallType.RESUMEN);
            return resumen;
        } catch (RestClientResponseException ex) {
            log.warn("No se pudo pedir el resumen de {} (HTTP {})",
                placeId, ex.getStatusCode().value());
            return null;
        }
    }

    /**
     * Si el nombre empieza con el de una cadena que Google sí marca como hamburguesería.
     *
     * Se exige que sea el principio y que siga un espacio —"La Birra Bar Colegiales"
     * empieza con "La Birra Bar"— y que la marca tenga al menos ocho caracteres, para
     * que un nombre corto no se lleve puesto a cualquiera que empiece parecido.
     */
    static boolean esSucursalDeUnaCadena(String name, Set<String> cadenas) {
        return VendeHamburguesas.esSucursalDeUnaCadena(name, cadenas);
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
        List<String> marcas = marcasQueComparten();
        Map<String, String> fotoPorMarca = fotosPorMarca(marcas);
        Map<String, String> fotoPorCadena = photosByChain();

        for (BurgerJoint joint : burgerJointRepository.findByPhotoUrlIsNull()) {
            // Una sucursal de cadena se queda con la foto de una hermana y no se le pide
            // nada a Google: ni ficha ni foto.
            //
            // Antes era al revés —se intentaba la propia y se prestaba solo si Google no
            // tenía ninguna—, con el argumento de que la foto propia de la sucursal es
            // mejor que la prestada. Sigue siendo cierto y ya no alcanza: hay 1.688
            // locales esperando portada y 911 fotos hasta fin de mes, así que cada foto
            // que se gasta en un McDonald's es una hamburguesería de barrio que se queda
            // con el recuadro de iniciales. Y entre dos sucursales de la misma cadena, la
            // foto es prácticamente la misma.
            //
            // Cuesta cero llamadas, así que no mira ninguna cuota.
            String deLaHermana = fotoPorCadena.get(chainKey(joint.getName()));
            String deLaMarca = fotoPorMarca.get(FastFoodMarker.marcaDe(joint.getName(), marcas));
            if (deLaMarca != null) {
                joint.setPhotoUrl(deLaMarca);
                burgerJointRepository.save(joint);
                reused++;
                continue;
            }

            // Solo la ficha, que es lo que hace falta siempre. La foto se pide más
            // abajo y únicamente si Google tiene alguna: son dos cuotas distintas, y
            // pedir las dos acá arriba frenaba todo el trabajo cuando se agotaba la de
            // fotos, incluso averiguar de qué locales no hay ninguna, que no cuesta una
            // sola foto y es lo que decide si se los esconde.
            if (!quotaGuard.canCall(PlacesCallType.DETAILS)) {
                log.warn("Cuota mensual de fichas alcanzada, quedan locales sin revisar");
                break;
            }

            List<FotoElegida> candidatas;
            try {
                pause();
                candidatas = placesClient.fotosDe(joint.getPlaceId());
                quotaGuard.record(PlacesCallType.DETAILS);
            } catch (RestClientResponseException ex) {
                log.warn("No se pudo pedir la ficha de {} (HTTP {})",
                    joint.getPlaceId(), ex.getStatusCode().value());
                continue;
            }

            // Ya que se preguntó, se anota cuántas tiene: es el mismo dato que acaba de
            // llegar y guardarlo no cuesta ninguna llamada más.
            joint.setFotosEnGoogle(candidatas.size());

            if (!candidatas.isEmpty()) {
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

                // Se prueban de la mejor a la peor: si la primera resulta ser el logo, la
                // siguiente suele ser una foto de producto. Cada intento cuesta una
                // llamada, así que son pocas y solo se llega a la segunda cuando hace
                // falta.
                boolean guardada = false;
                for (FotoElegida candidata : aProbar(candidatas)) {
                    if (!quotaGuard.canCall(PlacesCallType.PHOTO)) {
                        break;
                    }
                    String photoUrl = downloadPhoto(joint.getPlaceId(), candidata);
                    if (photoUrl != null) {
                        joint.setPhotoUrl(photoUrl);
                        joint.setPhotoName(candidata.name());
                        joint.setPhotoFingerprint(candidata.huella());
                        joint.setPhotoRule(PlacesClient.REGLA_DE_FOTO);
                        burgerJointRepository.save(joint);
                        fotoPorCadena.putIfAbsent(chainKey(joint.getName()), photoUrl);
                        String marca = FastFoodMarker.marcaDe(joint.getName(), marcas);
                        if (marca != null) {
                            fotoPorMarca.putIfAbsent(marca, photoUrl);
                        }
                        downloaded++;
                        guardada = true;
                        break;
                    }
                }
                if (guardada) {
                    continue;
                }
            } else {
                // Google no tiene ni una foto de este local. Queda anotado porque es lo
                // único que distingue "no hay nada" de "no fuimos a buscarlo", y la
                // diferencia decide si se lo esconde o se lo completa.
                joint.setSinFotosEnGoogle(true);
                burgerJointRepository.save(joint);
            }

            // Y acá caen los que no son de cadena pero igual tienen una hermana: un local
            // que se llama igual que otro sin ser una marca reconocida. Los de cadena ya
            // se resolvieron arriba, antes de gastarle una llamada a Google.
            if (deLaHermana != null) {
                joint.setPhotoUrl(deLaHermana);
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

         for (BurgerJoint joint : aRevisar()) {
             // Igual que arriba: acá solo hace falta la ficha. La mayoría de las fotos
             // no cambia de una regla a la otra, y esas se revisan sin bajar nada; pedir
             // también la cuota de fotos frenaba a todas por las pocas que sí cambian.
             if (!quotaGuard.canCall(PlacesCallType.DETAILS)) {
                 log.info("Cuota mensual de fichas alcanzada, quedan fotos por revisar");
                 break;
             }

             List<FotoElegida> candidatas;
             try {
                 pause();
                 candidatas = placesClient.fotosDe(joint.getPlaceId());
                 quotaGuard.record(PlacesCallType.DETAILS);
             } catch (RestClientResponseException ex) {
                 log.warn("No se pudo revisar la foto de {} (HTTP {})",
                     joint.getPlaceId(), ex.getStatusCode().value());
                 continue;
             }

             if (candidatas.isEmpty()) {
                 continue;
             }
             FotoElegida mejor = candidatas.get(0);

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

             // De la mejor a la peor, igual que al conseguir la primera foto: si la que
             // gana por puntaje resulta ser el logo, se prueba la siguiente.
             for (FotoElegida candidata : aProbar(candidatas)) {
                 if (!quotaGuard.canCall(PlacesCallType.PHOTO)) {
                     break;
                 }
                 String photoUrl = downloadPhoto(joint.getPlaceId(), candidata);
                 if (photoUrl != null) {
                     joint.setPhotoUrl(photoUrl);
                     joint.setPhotoName(candidata.name());
                     joint.setPhotoFingerprint(candidata.huella());
                     joint.setPhotoRule(PlacesClient.REGLA_DE_FOTO);
                     burgerJointRepository.save(joint);
                     cambiadas++;
                     break;
                 }
             }
         }

         return cambiadas;
     }

    /**
     * Qué locales hay que volver a mirar: los elegidos con una regla vieja, más los que
     * tienen una foto anotada a mano que todavía no es la que está puesta.
     *
     * Los segundos hacen falta porque anotar una foto en la configuración no cambia nada
     * por sí solo, y el local ya está marcado con la regla al día: sin esto, la elección
     * quedaría escrita y sin efecto hasta la próxima vez que cambie la regla.
     *
     * Se comparan las huellas para no volver a pedir la ficha de un local que ya tiene
     * puesta la foto elegida, que si no sería una llamada por local y por sincronización,
     * para siempre.
     */
    private List<BurgerJoint> aRevisar() {
        Map<Long, BurgerJoint> locales = new LinkedHashMap<>();
        for (BurgerJoint joint
            : burgerJointRepository.conFotoElegidaConUnaReglaVieja(PlacesClient.REGLA_DE_FOTO)) {
            locales.put(joint.getId(), joint);
        }

        Map<String, String> elegidas = properties.getSync().getFotosElegidas();
        if (!elegidas.isEmpty()) {
            for (BurgerJoint joint : burgerJointRepository.findByPlaceIdIn(elegidas.keySet())) {
                if (!elegidas.get(joint.getPlaceId()).equals(joint.getPhotoFingerprint())) {
                    locales.putIfAbsent(joint.getId(), joint);
                }
            }
        }

        return new ArrayList<>(locales.values());
    }

    /**
     * Las primeras que vale la pena probar de la lista entera.
     *
     * La lista viene completa porque su largo dice cuántas fotos tiene el local, y eso
     * no cuesta nada. Pero cada una que se prueba cuesta una llamada de foto, así que el
     * tope se aplica recién acá: si las tres mejores resultaron logos, el local
     * evidentemente no tiene una portada buena y seguir es gastar.
     */
    private static List<FotoElegida> aProbar(List<FotoElegida> candidatas) {
        return candidatas.size() <= PlacesClient.CANDIDATAS_A_PROBAR
            ? candidatas
            : candidatas.subList(0, PlacesClient.CANDIDATAS_A_PROBAR);
    }

    /**
     * Las marcas cuyas sucursales pueden compartir una portada.
     *
     * Son las cadenas de comida rápida más las otras marcas con sucursales. Las dos
     * listas están separadas en la configuración porque contestan preguntas distintas
     * —una decide qué se esconde al apagar las cadenas en Explorar— y acá se juntan
     * porque para la portada da lo mismo: dos sucursales de la misma marca tienen
     * prácticamente la misma foto, sea Burger King o Big Pons.
     */
    private List<String> marcasQueComparten() {
        return Stream.concat(
                properties.getFastFoodBrands().stream(),
                properties.getMarcasConSucursales().stream())
            .filter(marca -> marca != null && !marca.isBlank())
            .toList();
    }

    /**
     * Una foto por marca, para prestársela a las sucursales que no tengan.
     *
     * La clave es la marca y no el nombre entero, que es lo que hacía que no se
     * prestaran: "McDonald's" y "McDonald's Abasto Patio de Comidas" son la misma
     * cadena para el filtro de Explorar y eran dos distintas para la foto, así que
     * quedaban dieciocho sucursales con el recuadro de iniciales teniendo setenta y
     * nueve hermanas con portada.
     */
    private Map<String, String> fotosPorMarca(List<String> marcas) {
        Map<String, String> porMarca = new HashMap<>();
        for (BurgerJoint joint : burgerJointRepository.findByPhotoUrlIsNotNull()) {
            String marca = FastFoodMarker.marcaDe(joint.getName(), marcas);
            if (marca != null) {
                porMarca.putIfAbsent(marca, joint.getPhotoUrl());
            }
        }
        return porMarca;
    }

    public record FotosPrestadas(int prestadas, int siguenSinPortada) {}

    /**
     * Solo el préstamo entre sucursales: completa las que faltan y no toca nada más.
     *
     * Aparte de la revisión de fotos porque son dos cosas de costo muy distinto. Esto no
     * le pide nada a Google —la foto ya está bajada, se le apunta la misma a la hermana—
     * y la revisión gasta una ficha por local y una foto por cada uno que cambie.
     *
     * No pisa ninguna portada: solo mira los locales que no tienen. Una sucursal con
     * foto propia se la queda, que para eso se la bajamos.
     */
    public FotosPrestadas prestarFotos() {
        List<String> marcas = marcasQueComparten();
        Map<String, String> fotoPorMarca = fotosPorMarca(marcas);

        int prestadas = 0;
        int sinPortada = 0;
        for (BurgerJoint joint : burgerJointRepository.findByPhotoUrlIsNull()) {
            String deLaMarca = fotoPorMarca.get(FastFoodMarker.marcaDe(joint.getName(), marcas));
            if (deLaMarca == null) {
                sinPortada++;
                continue;
            }
            joint.setPhotoUrl(deLaMarca);
            burgerJointRepository.save(joint);
            prestadas++;
        }

        log.info("Fotos prestadas entre sucursales: {}, siguen sin portada {}", prestadas, sinPortada);
        return new FotosPrestadas(prestadas, sinPortada);
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

    /** Lo mismo pero sin exigir el rubro, para encontrar un local puntual por su nombre. */
    private PlacesSearchResult buscarPorNombre(String texto) {
        pause();
        PlacesSearchResult result = placesClient.searchText(texto, null, false);
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

    private boolean create(PlacesSearchResult.Place place, String area, boolean conFotos) {
        BurgerJoint joint = comoJoint(place, area);

        boolean gotPhoto = false;
        if (conFotos && place.photoName() != null && quotaGuard.canCall(PlacesCallType.PHOTO)) {
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
        return downloadPhoto(place.placeId(), place.photoName(),
            laEligieronAMano(place.placeId(), place.photoFingerprint()));
    }

    /**
     * Baja la foto, salvo que resulte ser un logo.
     *
     * El logo de un local lo sube el local, tiene buen tamaño y suele ser apaisado, así
     * que le gana por puntaje a cualquier fotografía: por eficaz que sea la regla de
     * elección, no hay forma de descartarlo sin mirar los píxeles, y para eso hay que
     * bajarlo. De las 422 que teníamos, seis eran logos.
     *
     * @return la ruta guardada, o null si no se pudo bajar o si es un logo
     */
    private String downloadPhoto(String placeId, FotoElegida foto) {
        return downloadPhoto(placeId, foto.name(), laEligieronAMano(placeId, foto.huella()));
    }

    private String downloadPhoto(String placeId, String photoName, boolean elegidaAMano) {
        try {
            pause();
            byte[] bytes = placesClient.downloadPhoto(photoName);
            quotaGuard.record(PlacesCallType.PHOTO);

            // Salvo que la haya elegido una persona. La detección de logos mira los
            // píxeles y acierta sobre lo que es, pero no sobre lo que se quiso: si
            // alguien eligió esa foto a mano, descartarla deshace la elección en
            // silencio y vuelve a poner la que la regla prefería.
            if (!elegidaAMano && EsUnaFotografia.pareceUnLogo(bytes)) {
                log.info("La foto elegida de {} es un logo, se descarta", placeId);
                return null;
            }

            return photoStorage.save(placeId, bytes);
        } catch (RestClientResponseException ex) {
            log.warn("Could not download photo for {} (HTTP {})", placeId, ex.getStatusCode().value());
            return null;
        }
    }

    /** Si esta foto es la que está anotada a mano en la configuración para este local. */
    private boolean laEligieronAMano(String placeId, String huella) {
        return huella != null
            && huella.equals(properties.getSync().getFotosElegidas().get(placeId));
    }

    /** @return true si en esta pasada se le consiguió la foto que le faltaba. */
    private boolean refresh(BurgerJoint joint, PlacesSearchResult.Place place, String area,
                            boolean conFotos) {
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
        if (conFotos && joint.getPhotoUrl() == null && place.photoName() != null
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
