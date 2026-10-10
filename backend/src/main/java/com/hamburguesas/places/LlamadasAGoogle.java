package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Optional;

/**
 * Por acá pasa cada llamada a Google: espaciada y anotada en la cuota.
 *
 * Las dos cosas iban escritas a mano en cada lugar que llamaba —seis—, y alcanzaba con
 * que uno se olvidara de anotar para gastar cuota sin que el contador se enterara. El
 * presupuesto de este proyecto es cero, así que el contador es lo único que separa el
 * tramo gratuito de una factura.
 *
 * Una llamada que Google rechaza no se anota, igual que antes: no la cobra.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LlamadasAGoogle {

    private final PlacesClient placesClient;
    private final PlacesQuotaGuard quotaGuard;
    private final PlacesProperties properties;

    /** Si todavía queda cuota de este tipo en el mes. */
    boolean quedan(PlacesCallType tipo) {
        return quotaGuard.canCall(tipo);
    }

    /**
     * Si queda alguna foto para bajar, contando además las pagas que se autorizaron.
     *
     * Primero el tramo gratuito, como siempre. Las pagas se suman arriba del tope y
     * cuentan sobre el mismo contador del mes, así que pedirlas dos veces no las duplica:
     * con un tope de mil y cuatrocientas pagas, la foto mil cuatrocientas uno no se baja
     * aunque se lo pida en otra corrida.
     *
     * @param pagas cuántas fotos se pueden pagar este mes por encima del tramo gratuito
     */
    boolean quedanFotos(int pagas) {
        if (quotaGuard.canCall(PlacesCallType.PHOTO)) {
            return true;
        }
        return pagas > 0
            && quotaGuard.used(PlacesCallType.PHOTO) < quotaGuard.limitFor(PlacesCallType.PHOTO) + pagas;
    }

    /** Cuántas fotos se pagaron este mes: las que pasaron el tramo gratuito. */
    int fotosPagasEsteMes() {
        return Math.max(0,
            quotaGuard.used(PlacesCallType.PHOTO) - quotaGuard.limitFor(PlacesCallType.PHOTO));
    }

    /** El tope mensual de este tipo, para los mensajes. */
    int limiteDe(PlacesCallType tipo) {
        return quotaGuard.limitFor(tipo);
    }

    /**
     * Una página de búsqueda, exigiendo el rubro de hamburguesería.
     *
     * Deja pasar el error de Google en vez de atraparlo: el barrido decide qué hacer
     * según el código, porque un 429 y un 503 no se resuelven igual.
     */
    PlacesSearchResult buscar(String consulta, String pageToken) {
        return buscar(consulta, pageToken, null);
    }

    /** Lo mismo, priorizando un círculo si lo hay: para barrer un radio (#222). */
    PlacesSearchResult buscar(String consulta, String pageToken, Circulo circulo) {
        pausa();
        PlacesSearchResult resultado = circulo == null
            ? placesClient.searchText(consulta, pageToken)
            : placesClient.searchText(consulta, pageToken, true, circulo);
        quotaGuard.record(PlacesCallType.SEARCH);
        return resultado;
    }

    /** Lo mismo pero sin exigir el rubro, para encontrar un local puntual por su nombre. */
    PlacesSearchResult buscarPorNombre(String texto) {
        pausa();
        PlacesSearchResult resultado = placesClient.searchText(texto, null, false);
        quotaGuard.record(PlacesCallType.SEARCH);
        return resultado;
    }

    /**
     * Las fotos que tiene el local en su ficha, de la mejor a la peor.
     *
     * @return vacío si Google no contestó: quien pregunta sigue con el próximo local
     */
    Optional<List<FotoElegida>> fotosDe(BurgerJoint joint) {
        try {
            pausa();
            List<FotoElegida> fotos = placesClient.fotosDe(joint.getPlaceId(), joint.getName());
            quotaGuard.record(PlacesCallType.LISTA_DE_FOTOS);
            return Optional.of(fotos);
        } catch (RestClientResponseException ex) {
            log.warn("No se pudo pedir la ficha de {} (HTTP {})",
                joint.getPlaceId(), ex.getStatusCode().value());
            return Optional.empty();
        }
    }

    /**
     * El resumen de reseñas, o null si el local no tiene.
     *
     * Deja pasar el error en vez de devolver null. Antes los dos casos salían iguales, y
     * no son lo mismo: "no tiene resumen" es una respuesta de Google, y "Google no
     * contestó" es no saber. Quien pregunta decide qué hacer con cada uno.
     */
    String resumenDe(String placeId) {
        pausa();
        String resumen = placesClient.resumenDeResenias(placeId);
        quotaGuard.record(PlacesCallType.RESUMEN);
        return resumen;
    }

    /**
     * Lo que hace falta para revisar a mano un local antes de agregarlo (#225): fotos,
     * opiniones, puntaje y resumen. Es la misma llamada que el resumen, al mismo precio,
     * así que se cuenta en esa cuota.
     */
    RevisionDeGoogle revisionDe(String placeId) {
        pausa();
        RevisionDeGoogle revision = placesClient.revisionDe(placeId);
        quotaGuard.record(PlacesCallType.RESUMEN);
        return revision;
    }

    /**
     * El horario de apertura del local, vacío si Google no lo tiene.
     *
     * Deja pasar el error: "no tiene horario" es una respuesta y se guarda, pero "Google
     * no contestó" no, y quien pregunta tiene que poder distinguirlos.
     */
    List<HorarioDeGoogle.Franja> horarioDe(String placeId) {
        pausa();
        List<HorarioDeGoogle.Franja> franjas = placesClient.horarioDe(placeId);
        quotaGuard.record(PlacesCallType.HORARIO);
        return franjas;
    }

    /**
     * Los bytes de una foto.
     *
     * Deja pasar el error: quien baja decide qué hacer con un local que se quedó sin la
     * suya, y sigue con la próxima candidata.
     */
    byte[] bajarFoto(String photoName) {
        pausa();
        byte[] bytes = placesClient.downloadPhoto(photoName);
        quotaGuard.record(PlacesCallType.PHOTO);
        return bytes;
    }

    private void pausa() {
        try {
            Thread.sleep(properties.getSync().getDelayBetweenCallsMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
