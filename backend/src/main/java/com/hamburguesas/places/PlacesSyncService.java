package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.BurgerJointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lo que se le puede pedir a la sincronización con Google, de a un trabajo por vez.
 *
 * Coordina y nada más: cada trabajo vive en su propia clase —{@link LimpiezaDeLocales},
 * {@link FotosDeLocales}, {@link ClasificadorDeLocales}, {@link HorariosDeLocales}— y toda llamada a Google pasa por
 * {@link LlamadasAGoogle}, que es la que la espacia y la anota en la cuota. Acá quedan
 * el barrido, que es lo que junta a las demás, y agregar un local a mano.
 *
 * Los trabajos están separados porque cuestan muy distinto: el barrido gasta mil
 * novecientas búsquedas, la limpieza ninguna, y las fotos el tramo más chico de la API.
 * Poder correr cada uno suelto es lo que permite gastar solo lo que hace falta.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlacesSyncService {

    /**
     * Cuántas consultas seguidas puede fallar Google antes de que dejemos de insistir.
     *
     * Una suelta es un hipo del servicio y saltearla cuesta una zona incompleta. Diez
     * seguidas es que Google no está, y seguir recorriendo las 89 zonas para recibir 89
     * errores no ayuda a nadie.
     */
    private static final int FALLAS_PARA_RENDIRSE = 10;

    private static final String SIN_CLAVE = "Falta configurar GOOGLE_MAPS_API_KEY";

    /**
     * Cuántas fotos se pueden pagar en un mes, como mucho, por encima del tramo gratuito.
     *
     * Google cobra siete dólares cada mil, así que quinientas son tres dólares y medio.
     * Está fijo en el código y no en la configuración a propósito: el presupuesto es
     * cero, y gastar más que esto tiene que ser un cambio que alguien revise.
     */
    static final int FOTOS_PAGAS_POR_MES = 500;

    private final PlacesProperties properties;
    private final LlamadasAGoogle google;
    private final ClasificadorDeLocales clasificador;
    private final LimpiezaDeLocales limpieza;
    private final FotosDeLocales fotos;
    private final HorariosDeLocales horarios;
    private final Zonas zonas;
    private final BurgerJointRepository burgerJointRepository;
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
     * Preguntar qué fotos tiene cada local no se paga (#199). Cuesta una foto por cada
     * local que no tenía y por cada uno que cambia.
     */
    public PlacesSyncReport revisarFotos() {
        return revisarFotos(0);
    }

    /**
     * Lo mismo, pudiendo pagar fotos para los locales que no tienen ninguna.
     *
     * Lo que se pide se recorta a {@link #FOTOS_PAGAS_POR_MES}, y como se cuenta sobre el
     * contador del mes, pedirlo de nuevo no suma: en un mes nunca se pagan más que esas.
     *
     * @param pagas cuántas fotos se pueden pagar este mes por encima del tramo gratuito
     */
    public PlacesSyncReport revisarFotos(int pagas) {
        if (!properties.hasApiKey()) {
            log.warn("Revisión de fotos salteada: falta la clave de Google");
            return PlacesSyncReport.skipped(SIN_CLAVE);
        }

        int autorizadas = Math.max(0, Math.min(pagas, FOTOS_PAGAS_POR_MES));
        FotosDeLocales.Completadas faltantes = fotos.completarFaltantes(autorizadas);
        int recambiadas = fotos.recambiarViejas();

        log.info("Revisión de fotos: {} bajadas, {} prestadas de otra sucursal, {} recambiadas, {} pagas autorizadas",
            faltantes.bajadas(), faltantes.prestadas(), recambiadas, autorizadas);

        return PlacesSyncReport.soloFotos(
            faltantes.bajadas() + recambiadas, faltantes.prestadas());
    }

    /** Pregunta de qué locales Google no tiene ninguna foto, sin bajar nada. */
    public CensoDeFichas revisarFichas() {
        if (!properties.hasApiKey()) {
            log.warn("Censo de fichas salteado: falta la clave de Google");
            return CensoDeFichas.skipped(SIN_CLAVE);
        }
        return fotos.censar();
    }

    /**
     * Pide el horario de los locales que no lo tienen, o que lo tienen hace más de un mes.
     *
     * Aparte del barrido porque sale de otra cuota —el horario es un campo Enterprise, con
     * mil gratis por mes— y no busca nada: pregunta por los locales que ya están.
     */
    public HorariosPedidos completarHorarios() {
        if (!properties.hasApiKey()) {
            log.warn("Horarios salteados: falta la clave de Google");
            return HorariosPedidos.skipped(SIN_CLAVE);
        }
        return horarios.completar();
    }

    /** Presta la portada entre sucursales de la misma marca, sin pedirle nada a Google. */
    public FotosPrestadas prestarFotos() {
        return fotos.prestarEntreSucursales();
    }

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
     */
    public LimpiezaResult limpiar() {
        LimpiezaResult resultado = limpieza.revisarLoGuardado(clasificador.cadenasDeHamburguesas());

        // Igual que al final del barrido: los que quedan tienen que estar bien marcados
        // como cadena o no, que es lo que mira el filtro de Explorar.
        fastFoodMarker.marcar();

        log.info("Limpieza: {} borrados, {} zonas corregidas",
            resultado.borrados(), resultado.corregidos());
        return resultado;
    }

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
            return new LocalAgregado(null, texto, null, null, SIN_CLAVE);
        }
        if (!google.quedan(PlacesCallType.SEARCH)) {
            return new LocalAgregado(null, texto, null, null,
                "Se acabó la cuota mensual de búsquedas");
        }

        PlacesSearchResult resultado;
        try {
            // Sin exigir el rubro de hamburguesería. El barrido sí lo exige, y por eso
            // mismo no encuentra estos locales: buscando "Austin's Diner & Grill" con el
            // filtro puesto, Google contesta con otro restaurante de Palermo.
            resultado = google.buscarPorNombre(texto);
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

        create(place, zona.get(), false, Veredicto.Prueba.A_MANO);
        fastFoodMarker.marcar();

        log.info("Agregado a mano: {} ({}) — {}", place.name(), zona.get(), place.placeId());
        return new LocalAgregado(place.placeId(), place.name(), place.address(), zona.get(),
            "Agregado");
    }

    public PlacesSyncReport sync() {
        return sync(true);
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
            return PlacesSyncReport.skipped(SIN_CLAVE);
        }

        Recuento recuento = new Recuento();

        // Se arma una vez: son las marcas que Google reconoce como hamburgueserías, y
        // sirven para rescatar a las sucursales que clasificó distinto al resto.
        Set<String> cadenas = clasificador.cadenasDeHamburguesas();

        LimpiezaResult limpiezaHecha = limpieza.revisarLoGuardado(cadenas);

        // Los que ya están, para no volver a crear una ficha repetida. Borrarlas en la
        // limpieza no alcanzaría: la búsqueda las trae igual, con otro identificador, y
        // volverían a entrar en la misma pasada.
        List<BurgerJoint> yaEstan = new ArrayList<>(burgerJointRepository.findAll());

        for (String area : properties.getSync().getAreas()) {
            for (String plantilla : properties.getSync().getQueryTemplates()) {
                if (!google.quedan(PlacesCallType.SEARCH)) {
                    log.warn("Monthly search quota reached ({}), stopping sync",
                        google.limiteDe(PlacesCallType.SEARCH));
                    break;
                }

                String consulta = plantilla.replace("{barrio}", area);
                String motivoParaFrenar = recorrer(consulta, cadenas, yaEstan, conFotos, recuento);
                if (motivoParaFrenar != null) {
                    return new PlacesSyncReport(recuento.creados, recuento.actualizados,
                        recuento.fotos, 0, motivoParaFrenar);
                }
            }
        }

        // Las dos pasadas de fotos van juntas en el interruptor: completar las que faltan
        // y recambiar las viejas gastan la misma cuota chica, y la idea de apagarlas es
        // no gastarla hasta saber a quién conviene.
        FotosDeLocales.Completadas faltantes = conFotos
            ? fotos.completarFaltantes()
            : new FotosDeLocales.Completadas(0, 0);
        recuento.fotos += faltantes.bajadas();
        if (conFotos) {
            recuento.fotos += fotos.recambiarViejas();
        }

        // Los locales que entraron recién no tienen marcado si son de una cadena, y el
        // filtro de Explorar mira esa marca. Sin esto, un McDonald's nuevo se vería
        // igual con las cadenas apagadas hasta el próximo arranque.
        fastFoodMarker.marcar();

        log.info("Places sync finished: {} created, {} updated, {} photos, {} reused, {} descartados, "
            + "{} barrios corregidos, {} borrados, {} consultas salteadas",
            recuento.creados, recuento.actualizados, recuento.fotos, faltantes.prestadas(),
            recuento.descartados, limpiezaHecha.corregidos(), limpiezaHecha.borrados(),
            recuento.saltadas);

        // Terminó de recorrer todo, pero avisando si algo quedó sin preguntar: una zona
        // con menos locales de los que debería tener se explica por acá.
        String aviso = recuento.saltadas == 0 ? null
            : recuento.saltadas + " consultas quedaron sin respuesta de Google y se saltearon";
        return new PlacesSyncReport(recuento.creados, recuento.actualizados, recuento.fotos,
            faltantes.prestadas(), aviso);
    }

    /** Lo que va sumando el barrido mientras recorre las zonas. */
    private static final class Recuento {
        int creados;
        int actualizados;
        int fotos;
        int descartados;
        // Consultas que Google no contestó y se saltearon, y cuántas van seguidas. Si se
        // encadenan es que Google está caído y no tiene sentido recorrer lo que falta.
        int saltadas;
        int fallasSeguidas;
    }

    /**
     * Las páginas de una consulta, una por una.
     *
     * @return null para seguir con la próxima consulta, o el motivo para frenar el
     *         barrido entero
     */
    private String recorrer(String consulta, Set<String> cadenas, List<BurgerJoint> yaEstan,
                            boolean conFotos, Recuento recuento) {
        String pageToken = null;
        for (int page = 0; page < properties.getSync().getMaxPagesPerArea(); page++) {
            PlacesSearchResult result;
            try {
                result = google.buscar(consulta, pageToken);
                recuento.fallasSeguidas = 0;
            } catch (RestClientResponseException ex) {
                int codigo = ex.getStatusCode().value();

                // Un 429 o un 403 no se arreglan reintentando: o nos pasamos del
                // ritmo o la clave no sirve, y en los dos casos seguir es gastar
                // llamadas que van a fallar igual.
                if (esDefinitivo(codigo)) {
                    log.warn("Google respondió {} en {}, se frena el barrido", codigo, consulta);
                    return "Google respondió " + codigo + ", se frenó la sincronización";
                }

                // Un 503 sí: es Google que no está disponible por un rato. Antes se
                // llevaba puesto el barrido entero, y como las zonas se recorren en
                // orden, siempre moría en la misma mitad: las últimas —todo el
                // corredor norte y La Plata— no entraron nunca.
                recuento.fallasSeguidas++;
                if (recuento.fallasSeguidas >= FALLAS_PARA_RENDIRSE) {
                    log.warn("Google falló {} veces seguidas, se frena el barrido",
                        recuento.fallasSeguidas);
                    return "Google falló " + recuento.fallasSeguidas
                        + " veces seguidas, se frenó la sincronización";
                }

                log.warn("Google respondió {} en {}, se saltea esta consulta ({} seguidas)",
                    codigo, consulta, recuento.fallasSeguidas);
                recuento.saltadas++;
                return null;
            }

            for (PlacesSearchResult.Place place : result.places()) {
                incorporar(place, cadenas, yaEstan, conFotos, recuento);
            }

            pageToken = result.nextPageToken();
            if (pageToken == null || pageToken.isBlank()) {
                return null;
            }
            if (!google.quedan(PlacesCallType.SEARCH)) {
                return null;
            }
        }
        return null;
    }

    /** Lo que no se arregla reintentando: nos pasamos del ritmo, o la clave no sirve. */
    private static boolean esDefinitivo(int codigo) {
        return codigo == 429 || codigo == 401 || codigo == 403;
    }

    /** Un local que devolvió la búsqueda: se descarta, se actualiza o entra. */
    private void incorporar(PlacesSearchResult.Place place, Set<String> cadenas,
                            List<BurgerJoint> yaEstan, boolean conFotos, Recuento recuento) {
        if (place.placeId() == null || place.name() == null) {
            return;
        }

        // Si ya está guardado, con la prueba con la que entró: así un local que entró por
        // su resumen de reseñas no lo vuelve a pagar en cada barrido (#97).
        var existing = burgerJointRepository.findByPlaceId(place.placeId());
        Veredicto veredicto = clasificador.evaluar(
            place, cadenas, existing.map(ClasificadorDeLocales::pruebaAnotada).orElse(null));
        if (!veredicto.vendeHamburguesas()) {
            log.debug("{} no es un lugar donde comer ({}: {}), se descarta",
                place.name(), place.primaryType(), veredicto.prueba());
            recuento.descartados++;
            return;
        }

        // El barrio sale de las coordenadas, no de la búsqueda que lo trajo: Google
        // devuelve lo que le parece cerca y se pasa de largo del barrio que se le pidió.
        // Y si el punto no está en la Ciudad, el local no va: "hamburguesería en San
        // Nicolás" trae San Nicolás de los Arroyos y "Versalles" trae uno de Colombia.
        var barrio = zonas.zonaDe(place.latitude(), place.longitude(), place.address());
        if (barrio.isEmpty()) {
            log.debug("{} queda fuera de la Ciudad ({}), se descarta",
                place.name(), place.address());
            recuento.descartados++;
            return;
        }

        boolean gotPhoto;
        if (existing.isPresent()) {
            gotPhoto = refresh(existing.get(), place, barrio.get(), conFotos, veredicto.prueba());
            recuento.actualizados++;
        } else {
            // Google tiene dos fichas para algunos negocios, con identificadores
            // distintos. Sin esto entran las dos.
            BurgerJoint nuevo = comoJoint(place, barrio.get());
            BurgerJoint repetido = Duplicados.elMismoLocalEntre(yaEstan, nuevo);
            if (repetido != null) {
                log.debug("{} ya está como {}, se descarta la ficha repetida",
                    place.name(), repetido.getName());
                recuento.descartados++;
                return;
            }

            gotPhoto = create(place, barrio.get(), conFotos, veredicto.prueba());
            yaEstan.add(nuevo);
            recuento.creados++;
        }
        if (gotPhoto) {
            recuento.fotos++;
        }
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

    /** @return true si en esta pasada se le consiguió la foto. */
    private boolean create(PlacesSearchResult.Place place, String area, boolean conFotos,
                           Veredicto.Prueba prueba) {
        BurgerJoint joint = comoJoint(place, area);
        joint.setPruebaDeHamburguesas(prueba.name());
        boolean gotPhoto = conFotos && fotos.ponerLaDeLaBusqueda(joint, place);
        burgerJointRepository.save(joint);
        return gotPhoto;
    }

    /** @return true si en esta pasada se le consiguió la foto que le faltaba. */
    private boolean refresh(BurgerJoint joint, PlacesSearchResult.Place place, String area,
                            boolean conFotos, Veredicto.Prueba prueba) {
        joint.setName(place.name());
        joint.setPruebaDeHamburguesas(prueba.name());
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
        boolean gotPhoto = conFotos && joint.getPhotoUrl() == null
            && fotos.ponerLaDeLaBusqueda(joint, place);

        burgerJointRepository.save(joint);
        return gotPhoto;
    }
}
