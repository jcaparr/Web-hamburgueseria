package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.BurgerJointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Las portadas de los locales: conseguirlas, mejorarlas y compartirlas entre sucursales.
 *
 * Todo lo que toca la cuota de fotos pasa por acá, que es el tramo gratuito más chico de
 * la API: mil por mes. Por eso el orden importa en cada método: primero lo que no cuesta
 * nada —prestar entre sucursales, preguntar qué fotos tiene un local (#199)— y recién al
 * final lo que cuesta una foto.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FotosDeLocales {

    private final PlacesProperties properties;
    private final LlamadasAGoogle google;
    private final PhotoStorage photoStorage;
    private final BurgerJointRepository burgerJointRepository;

    /** Cuántas portadas se bajaron y cuántas se prestaron de otra sucursal. */
    record Completadas(int bajadas, int prestadas) {}

    /**
     * Pregunta, sin bajar nada, de qué locales Google no tiene ninguna foto.
     *
     * Preguntar es gratis (#199) y bajar sale una foto, de las que hay mil. Con mil doscientos locales sin
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
    CensoDeFichas censar() {
        int preguntados = 0;
        int sinNingunaFoto = 0;
        int sinPreguntar = 0;

        // Los que nunca preguntamos, y no solo los que no tienen portada. Un local puede
        // tener una foto puesta y tener apenas tres en Google, que es justamente lo que
        // hay que poder ver para decidir si corresponde que esté en la lista.
        //
        // Vienen ordenados por sospecha: primero los que Google no clasifica como
        // hamburguesería, al final las cadenas. Preguntar ya no se paga, pero el tope del
        // mes sigue estando, así que el orden decide qué se alcanza a saber.
        //
        // Y como se eligen por no tener el número anotado, cada corrida sigue donde quedó
        // la anterior sin volver a preguntar por los mismos.
        List<BurgerJoint> sinFoto = burgerJointRepository.sinSaberCuantasFotosTiene();
        for (BurgerJoint joint : sinFoto) {
            if (!google.quedan(PlacesCallType.LISTA_DE_FOTOS)) {
                sinPreguntar = sinFoto.size() - preguntados;
                log.warn("Cuota mensual de fichas alcanzada, quedan {} locales sin preguntar",
                    sinPreguntar);
                break;
            }

            Optional<List<FotoElegida>> ficha = google.fotosDe(joint);
            if (ficha.isEmpty()) {
                continue;
            }
            List<FotoElegida> candidatas = ficha.get();

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
     * Completa las fotos de los locales que las búsquedas por barrio no devuelven.
     *
     * Hasta acá la foto llegaba de arriba: se bajaba la de los locales que aparecían
     * en una búsqueda. Pero un local puede estar en la base y no aparecer en ninguna:
     * los que Google no clasifica como hamburguesería —Burger King, por ejemplo, que
     * figura como comida rápida— quedan afuera del filtro estricto, y también queda
     * afuera cualquiera que no entre en los 60 resultados de su barrio. Esos se
     * quedaban sin foto para siempre.
     *
     * Acá se les pide la ficha por su place_id, que es una llamada aparte y gratuita
     * (#199).
     */
    Completadas completarFaltantes() {
        return completarFaltantes(0);
    }

    /**
     * Lo mismo, pudiendo pagar algunas fotos más allá del tramo gratuito del mes.
     *
     * Para no esperar al mes siguiente cuando faltan pocas: en octubre de 2026 quedaron
     * 302 locales sin portada con la cuota gastada, y completarlos salía unos dos
     * dólares. Las pagas sirven solo para esto, que es poner la primera foto a un local
     * que no tiene ninguna. Recambiar una foto que ya está no las usa nunca.
     *
     * @param pagas cuántas fotos se pueden pagar este mes por encima del tramo gratuito;
     *              cero es lo de siempre
     */
    Completadas completarFaltantes(int pagas) {
        int bajadas = 0;
        int prestadas = 0;
        List<String> marcas = marcasQueComparten();
        Map<String, String> fotoPorMarca = fotosPorMarca(marcas);

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
            //
            // Solo entre sucursales de una marca anotada a mano. También se prestaba entre
            // locales que se llamaban igual, pero hay nombres que comparten locales sin
            // ninguna relación —"Big Burger" en González Catán, Merlo y Pontevedra;
            // "Burger House" en Barracas y en Grand Bourg—, y el que perdiera su portada
            // recibía la de un desconocido: la tarjeta mostraba un local por otro (#99).
            String deLaMarca = fotoPorMarca.get(FastFoodMarker.marcaDe(joint.getName(), marcas));
            if (deLaMarca != null) {
                ponerPrestada(joint, deLaMarca);
                prestadas++;
                continue;
            }

            // Solo la ficha, que es lo que hace falta siempre. La foto se pide más
            // abajo y únicamente si Google tiene alguna: son dos cuotas distintas, y
            // pedir las dos acá arriba frenaba todo el trabajo cuando se agotaba la de
            // fotos, incluso averiguar de qué locales no hay ninguna, que no cuesta una
            // sola foto y es lo que decide si se los esconde.
            if (!google.quedan(PlacesCallType.LISTA_DE_FOTOS)) {
                log.warn("Tope mensual de fichas de fotos alcanzado, quedan locales sin revisar");
                break;
            }

            Optional<List<FotoElegida>> ficha = google.fotosDe(joint);
            if (ficha.isEmpty()) {
                continue;
            }
            List<FotoElegida> candidatas = ficha.get();

            // Ya que se preguntó, se anota cuántas tiene y si tiene alguna, y se guarda
            // enseguida. Es el mismo dato que acaba de llegar, y antes solo quedaba si
            // después pasaba algo más que guardara el local: cuando no quedaba cuota de
            // fotos, o ninguna candidata servía y no había hermana de quien prestar, se
            // perdía, y el próximo censo gastaba otra ficha en volver a preguntarlo (#98).
            //
            // Sin fotos queda anotado porque es lo único que distingue "no hay nada" de
            // "no fuimos a buscarlo", y la diferencia decide si se lo esconde o se lo
            // completa. Con fotos se corrige si venía anotado que no: un local que recién
            // abrió y todavía no tenía ninguna las tiene ahora.
            joint.setFotosEnGoogle(candidatas.size());
            joint.setSinFotosEnGoogle(candidatas.isEmpty());
            burgerJointRepository.save(joint);

            if (!candidatas.isEmpty()) {
                // Tiene fotos pero no hay cuota para bajarlas. No se le presta la de
                // otra sucursal: tener la propia es mejor, y va a estar el mes que viene.
                if (!google.quedanFotos(pagas)) {
                    continue;
                }

                String puesta = probarHastaQueUnaSirva(joint, candidatas, pagas);
                if (puesta != null) {
                    // Las sucursales que vienen después ya la pueden usar.
                    String marca = FastFoodMarker.marcaDe(joint.getName(), marcas);
                    if (marca != null) {
                        fotoPorMarca.putIfAbsent(marca, puesta);
                    }
                    bajadas++;
                    continue;
                }
            }
        }

        return new Completadas(bajadas, prestadas);
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
    int recambiarViejas() {
        int cambiadas = 0;

        for (BurgerJoint joint : aRevisar()) {
            // Igual que arriba: acá solo hace falta la ficha. La mayoría de las fotos
            // no cambia de una regla a la otra, y esas se revisan sin bajar nada; pedir
            // también la cuota de fotos frenaba a todas por las pocas que sí cambian.
            if (!google.quedan(PlacesCallType.LISTA_DE_FOTOS)) {
                log.info("Tope mensual de fichas de fotos alcanzado, quedan fotos por revisar");
                break;
            }

            Optional<List<FotoElegida>> ficha = google.fotosDe(joint);
            if (ficha.isEmpty() || ficha.get().isEmpty()) {
                continue;
            }
            List<FotoElegida> candidatas = ficha.get();
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
                joint.setPhotoRule(EleccionDeFoto.REGLA_DE_FOTO);
                burgerJointRepository.save(joint);
                continue;
            }

            // La regla nueva eligió otra, pero no hay cuota para bajarla. Se deja sin
            // anotar la regla, así vuelve a caer en esta lista el mes que viene.
            //
            // Nunca con fotos pagas: el local ya tiene una portada, y mejorarla puede
            // esperar al tramo gratuito del mes que viene.
            if (!google.quedan(PlacesCallType.PHOTO)) {
                continue;
            }

            if (probarHastaQueUnaSirva(joint, candidatas, 0) != null) {
                cambiadas++;
            }
        }

        return cambiadas;
    }

    /**
     * Solo el préstamo entre sucursales: completa las que faltan y no toca nada más.
     *
     * Aparte de la revisión de fotos porque son dos cosas de costo muy distinto. Esto no
     * le pide nada a Google —la foto ya está bajada, se le apunta la misma a la hermana—
     * y la revisión gasta una foto por cada local que cambie.
     *
     * No pisa ninguna portada: solo mira los locales que no tienen. Una sucursal con
     * foto propia se la queda, que para eso se la bajamos.
     */
    FotosPrestadas prestarEntreSucursales() {
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
            ponerPrestada(joint, deLaMarca);
            prestadas++;
        }

        log.info("Fotos prestadas entre sucursales: {}, siguen sin portada {}", prestadas, sinPortada);
        return new FotosPrestadas(prestadas, sinPortada);
    }

    /**
     * Baja la portada que ya vino en la búsqueda y se la pone al local, sin guardarlo.
     *
     * Para los locales que entran o se actualizan en el barrido: la búsqueda ya trae cuál
     * es la mejor foto, así que no hace falta pedir la ficha. Una foto por local y para
     * siempre: se baja una sola vez, se guarda en disco y no se vuelve a pedir.
     *
     * @return true si quedó puesta
     */
    boolean ponerLaDeLaBusqueda(BurgerJoint joint, PlacesSearchResult.Place place) {
        if (place.photoName() == null || !google.quedan(PlacesCallType.PHOTO)) {
            return false;
        }
        String ruta = bajar(place.placeId(), place.photoName(),
            laEligieronAMano(place.placeId(), place.photoFingerprint()));
        if (ruta == null) {
            return false;
        }
        ponerFoto(joint, ruta, place.photoName(), place.photoFingerprint());
        return true;
    }

    /**
     * Prueba las candidatas de la mejor a la peor, y se queda con la primera que sirve.
     *
     * Si la primera resulta ser el logo, la siguiente suele ser una foto de producto.
     * Cada intento cuesta una llamada, así que son pocas y solo se llega a la segunda
     * cuando hace falta.
     *
     * @param pagas las fotos que se pueden pagar este mes encima del tramo gratuito
     * @return la ruta de la que quedó puesta, o null si ninguna sirvió o se acabó la cuota
     */
    private String probarHastaQueUnaSirva(BurgerJoint joint, List<FotoElegida> candidatas, int pagas) {
        for (FotoElegida candidata : EleccionDeFoto.aProbar(candidatas)) {
            if (!google.quedanFotos(pagas)) {
                return null;
            }
            String ruta = bajar(joint.getPlaceId(), candidata.name(),
                laEligieronAMano(joint.getPlaceId(), candidata.huella()));
            if (ruta != null) {
                ponerFoto(joint, ruta, candidata.name(), candidata.huella());
                burgerJointRepository.save(joint);
                return ruta;
            }
        }
        return null;
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
    private String bajar(String placeId, String photoName, boolean elegidaAMano) {
        try {
            byte[] bytes = google.bajarFoto(photoName);

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

    /** Una foto propia, con todo lo que hace falta para reconocerla la próxima vez. */
    private static void ponerFoto(BurgerJoint joint, String ruta, String nombre, String huella) {
        joint.setPhotoUrl(ruta);
        joint.setPhotoName(nombre);
        joint.setPhotoFingerprint(huella);
        joint.setPhotoRule(EleccionDeFoto.REGLA_DE_FOTO);
    }

    /** La foto de otra sucursal: se le apunta la misma ruta, sin bajar nada. */
    private void ponerPrestada(BurgerJoint joint, String ruta) {
        joint.setPhotoUrl(ruta);
        burgerJointRepository.save(joint);
    }

    /** Si esta foto es la que está anotada a mano en la configuración para este local. */
    private boolean laEligieronAMano(String placeId, String huella) {
        return huella != null
            && huella.equals(properties.getSync().getFotosElegidas().get(placeId));
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
            : burgerJointRepository.conFotoElegidaConUnaReglaVieja(EleccionDeFoto.REGLA_DE_FOTO)) {
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

}
