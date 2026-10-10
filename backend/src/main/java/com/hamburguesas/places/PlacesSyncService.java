package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.BurgerJointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Lo que va detrás de cada zona de la lista en el barrido general: "hamburguesería en
     * Palermo, Buenos Aires". El barrido de una zona sola usa la zona tal cual se la
     * escribe, que puede ser de cualquier provincia.
     */
    static final String PROVINCIA_DEL_BARRIDO_GENERAL = ", Buenos Aires";

    /** Hasta dónde llega un barrido: lo que cae afuera se descarta sin mirarlo. */
    @FunctionalInterface
    private interface Alcance {
        boolean llegaA(PlacesSearchResult.Place place);
    }

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
     * —los que no tienen una dirección de Argentina, los repetidos y los que Google no tiene
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
     * cualquier regla nuestra. Entra esté donde esté, mientras sea de Argentina: si se
     * lo pidió es porque se lo quiere (#222).
     *
     * Cuesta una búsqueda. No baja la foto: la cuota de fotos es el tramo más chico y
     * esto se usa de a uno, así que la portada la completa la próxima pasada de fotos.
     */
    public LocalAgregado agregar(String texto) {
        return agregar(texto, null);
    }

    /**
     * Lo mismo, asegurando que se guarda el local que se revisó (#224).
     *
     * Entre mostrar qué local devuelve Google y agregarlo pasan dos búsquedas, y Google no
     * promete contestar igual las dos veces. Con el identificador esperado, si la segunda
     * trae otro local no se guarda nada: agregar uno por otro es peor que no agregar.
     *
     * @param placeIdEsperado el que se revisó, o null para guardar el primero que venga
     */
    public LocalAgregado agregar(String texto, String placeIdEsperado) {
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
        if (placeIdEsperado != null && !placeIdEsperado.isBlank()
            && !placeIdEsperado.equals(place.placeId())) {
            return new LocalAgregado(place.placeId(), place.name(), place.address(), null,
                "Google devolvió otro local, no se agregó nada");
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
                "La dirección no es de Argentina, o no se le puede sacar una zona");
        }

        create(place, zona.get(), false, Veredicto.Prueba.A_MANO);
        fastFoodMarker.marcar();

        log.info("Agregado a mano: {} ({}) — {}", place.name(), zona.get(), place.placeId());
        return new LocalAgregado(place.placeId(), place.name(), place.address(), zona.get(),
            "Agregado");
    }

    /**
     * La portada de un solo local (#224): para agregarlo con foto, o para ponerle la que
     * se eligió a mano en la configuración.
     *
     * @param pagar si se acabaron las fotos gratis del mes, pagar una. Una sola, y nunca
     *              por encima de {@link #FOTOS_PAGAS_POR_MES}.
     */
    public FotoDeUnLocal portadaDeUnLocal(String placeId, boolean pagar) {
        if (!properties.hasApiKey()) {
            return new FotoDeUnLocal(placeId, null, SIN_CLAVE, null, null, false);
        }
        var joint = burgerJointRepository.findByPlaceId(placeId);
        if (joint.isEmpty()) {
            return new FotoDeUnLocal(placeId, null, "No está en la base", null, null, false);
        }

        // El permiso de pagar se cuenta sobre el mes (ver LlamadasAGoogle.quedanFotos):
        // las pagadas hasta ahora más una. Si ya se llegó al tope del mes, ninguna.
        int pagadas = google.fotosPagasEsteMes();
        int pagas = pagar && pagadas < FOTOS_PAGAS_POR_MES ? pagadas + 1 : 0;

        FotoDeUnLocal resultado = fotos.portadaDe(joint.get(), pagas);
        log.info("Portada de {}: {}", joint.get().getName(), resultado.resultado());
        return resultado;
    }

    /**
     * Barre una zona sin guardar nada, para revisar a mano qué se agrega (#225).
     *
     * Hace las mismas búsquedas que {@link #barrerZona}, y de cada local que todavía no
     * está en la web pide fotos, opiniones, puntaje y resumen de reseñas. Con eso se ven los
     * falsos: el kiosco que vende hamburguesas sueltas, la fábrica de medallones, el local
     * que cerró. Los que se aprueban se agregan después con {@link #agregar(String, String)}.
     *
     * Cuesta hasta 21 búsquedas y una ficha de resumen por candidato. Si se acaba la cuota
     * de resúmenes, los que faltan vienen sin esos datos, y el aviso lo dice.
     */
    public CandidatosDeZona candidatosDeZona(String lugar, Circulo circulo) {
        if (!properties.hasApiKey()) {
            return CandidatosDeZona.salteado(lugar, SIN_CLAVE);
        }
        if (lugar == null || lugar.isBlank()) {
            return CandidatosDeZona.salteado(lugar, "Falta decir qué zona barrer");
        }
        if (circulo != null && circulo.radioKm() <= 0) {
            return CandidatosDeZona.salteado(lugar, "El radio tiene que ser mayor que cero");
        }

        List<BurgerJoint> yaEstan = new ArrayList<>(burgerJointRepository.findAll());
        Map<String, PlacesSearchResult.Place> vistos = new LinkedHashMap<>();
        Map<String, String> zonaDe = new HashMap<>();
        int busquedas = 0;
        int yaEstaban = 0;
        int fueraDeLaZona = 0;
        String aviso = null;

        buscar:
        for (String plantilla : properties.getSync().getQueryTemplates()) {
            String consulta = plantilla.replace("{barrio}", lugar.trim());
            String pageToken = null;
            for (int page = 0; page < properties.getSync().getMaxPagesPerArea(); page++) {
                if (!google.quedan(PlacesCallType.SEARCH)) {
                    aviso = "Se acabó la cuota mensual de búsquedas a mitad del barrido";
                    break buscar;
                }
                PlacesSearchResult result;
                try {
                    result = google.buscar(consulta, pageToken, circulo);
                    busquedas++;
                } catch (RestClientResponseException ex) {
                    busquedas++;
                    int codigo = ex.getStatusCode().value();
                    if (esDefinitivo(codigo)) {
                        aviso = "Google respondió " + codigo + ", se frenó el barrido";
                        break buscar;
                    }
                    aviso = "Algunas búsquedas quedaron sin respuesta de Google";
                    break;
                }

                for (PlacesSearchResult.Place place : result.places()) {
                    if (place.placeId() == null || place.name() == null
                        || vistos.containsKey(place.placeId()) || zonaDe.containsKey(place.placeId())) {
                        continue;
                    }
                    var zona = zonas.zonaDe(place.latitude(), place.longitude(), place.address());
                    if (zona.isEmpty()
                        || (circulo != null && !circulo.contiene(place.latitude(), place.longitude()))) {
                        zonaDe.put(place.placeId(), "");
                        fueraDeLaZona++;
                        continue;
                    }
                    if (burgerJointRepository.findByPlaceId(place.placeId()).isPresent()
                        || Duplicados.elMismoLocalEntre(yaEstan, comoJoint(place, zona.get())) != null) {
                        zonaDe.put(place.placeId(), "");
                        yaEstaban++;
                        continue;
                    }
                    vistos.put(place.placeId(), place);
                    zonaDe.put(place.placeId(), zona.get());
                }

                pageToken = result.nextPageToken();
                if (pageToken == null || pageToken.isBlank()) {
                    break;
                }
            }
        }

        Set<String> cadenas = clasificador.cadenasDeHamburguesas();
        List<CandidatosDeZona.Candidato> candidatos = new ArrayList<>();
        for (PlacesSearchResult.Place place : vistos.values()) {
            RevisionDeGoogle revision = null;
            if (google.quedan(PlacesCallType.RESUMEN)) {
                try {
                    revision = google.revisionDe(place.placeId());
                } catch (RestClientException ex) {
                    log.warn("No se pudo revisar {}: {}", place.placeId(), ex.getMessage());
                }
            } else if (aviso == null) {
                aviso = "Se acabó la cuota de resúmenes: algunos vienen sin fotos, opiniones ni resumen";
            }

            Veredicto veredicto = clasificador.evaluarConResumen(place, cadenas,
                revision == null ? null : revision.resumen());
            candidatos.add(new CandidatosDeZona.Candidato(
                place.placeId(), place.name(), place.address(), zonaDe.get(place.placeId()),
                place.primaryType(),
                revision == null ? null : revision.fotos(),
                revision == null ? null : revision.opiniones(),
                revision == null ? null : revision.puntaje(),
                revision == null ? null : revision.resumen(),
                veredicto.vendeHamburguesas(), veredicto.prueba().name(),
                "https://www.google.com/maps/place/?q=place_id:" + place.placeId()));
        }

        log.info("Candidatos de {}: {} para revisar, {} ya estaban, {} fuera de la zona, {} búsquedas",
            lugar, candidatos.size(), yaEstaban, fueraDeLaZona, busquedas);
        return new CandidatosDeZona(lugar, busquedas, yaEstaban, fueraDeLaZona, candidatos, aviso);
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

                String consulta = plantilla.replace("{barrio}", area + PROVINCIA_DEL_BARRIDO_GENERAL);
                String motivoParaFrenar = recorrer(consulta, null, cercaDelObelisco(),
                    cadenas, yaEstan, conFotos, recuento);
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

    /**
     * Barre una zona sola, la que se pida: un barrio, una ciudad, una provincia, o un radio
     * alrededor de un punto (#222).
     *
     * Es como se suman locales ahora que la página está poblada: el barrido general era
     * para llenarla, y repetirlo trae casi siempre lo mismo. Pregunta de las mismas siete
     * formas, con hasta tres páginas cada una: 21 búsquedas como mucho.
     *
     * No pasa por la limpieza general ni por el radio de 75 km. Para sumar un barrio no
     * hace falta revisar toda la base, y si se pide Mar del Plata es porque se quiere
     * llegar ahí.
     *
     * @param lugar   lo que se le pregunta a Google, tal cual: "Mar del Plata, Buenos
     *                Aires", "Córdoba, Argentina", "Palermo, Buenos Aires"
     * @param circulo si no es null, solo entra lo que cae adentro
     */
    public PlacesSyncReport barrerZona(String lugar, Circulo circulo, boolean conFotos) {
        if (!properties.hasApiKey()) {
            return PlacesSyncReport.skipped(SIN_CLAVE);
        }
        if (lugar == null || lugar.isBlank()) {
            return PlacesSyncReport.skipped("Falta decir qué zona barrer");
        }
        if (circulo != null && circulo.radioKm() <= 0) {
            return PlacesSyncReport.skipped("El radio tiene que ser mayor que cero");
        }

        Recuento recuento = new Recuento();
        Set<String> cadenas = clasificador.cadenasDeHamburguesas();
        List<BurgerJoint> yaEstan = new ArrayList<>(burgerJointRepository.findAll());
        Alcance alcance = circulo == null
            ? place -> true
            : place -> circulo.contiene(place.latitude(), place.longitude());

        for (String plantilla : properties.getSync().getQueryTemplates()) {
            if (!google.quedan(PlacesCallType.SEARCH)) {
                log.warn("Se acabó la cuota de búsquedas barriendo {}", lugar);
                return new PlacesSyncReport(recuento.creados, recuento.actualizados,
                    recuento.fotos, 0, "Se acabó la cuota mensual de búsquedas a mitad del barrido");
            }
            String consulta = plantilla.replace("{barrio}", lugar.trim());
            String motivoParaFrenar = recorrer(consulta, circulo, alcance, cadenas, yaEstan,
                conFotos, recuento);
            if (motivoParaFrenar != null) {
                return new PlacesSyncReport(recuento.creados, recuento.actualizados,
                    recuento.fotos, 0, motivoParaFrenar);
            }
        }

        fastFoodMarker.marcar();

        log.info("Barrido de {}: {} nuevos, {} actualizados, {} fotos, {} descartados, {} consultas salteadas",
            lugar, recuento.creados, recuento.actualizados, recuento.fotos, recuento.descartados,
            recuento.saltadas);

        String aviso = recuento.saltadas == 0 ? null
            : recuento.saltadas + " consultas quedaron sin respuesta de Google y se saltearon";
        return new PlacesSyncReport(recuento.creados, recuento.actualizados, recuento.fotos, 0, aviso);
    }

    /** El barrido general llega hasta el radio de siempre desde el Obelisco. */
    private Alcance cercaDelObelisco() {
        return place -> zonas.estaCercaDelObelisco(place.latitude(), place.longitude());
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
    private String recorrer(String consulta, Circulo circulo, Alcance alcance, Set<String> cadenas,
                            List<BurgerJoint> yaEstan, boolean conFotos, Recuento recuento) {
        String pageToken = null;
        for (int page = 0; page < properties.getSync().getMaxPagesPerArea(); page++) {
            PlacesSearchResult result;
            try {
                result = google.buscar(consulta, pageToken, circulo);
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
                incorporar(place, alcance, cadenas, yaEstan, conFotos, recuento);
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
    private void incorporar(PlacesSearchResult.Place place, Alcance alcance, Set<String> cadenas,
                            List<BurgerJoint> yaEstan, boolean conFotos, Recuento recuento) {
        if (place.placeId() == null || place.name() == null) {
            return;
        }

        // Lo que Google trae de más lejos de lo que se pidió no entra. Se mira antes que
        // nada porque es gratis, y el clasificador puede tener que pagar un resumen.
        if (!alcance.llegaA(place)) {
            log.debug("{} queda fuera de lo que se pidió barrer ({}), se descarta",
                place.name(), place.address());
            recuento.descartados++;
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
        // Sin zona es que no es de Argentina: "Versalles" trae uno de Colombia.
        var barrio = zonas.zonaDe(place.latitude(), place.longitude(), place.address());
        if (barrio.isEmpty()) {
            log.debug("{} no tiene una dirección de Argentina ({}), se descarta",
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
