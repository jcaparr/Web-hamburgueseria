package com.hamburguesas.service;

import com.hamburguesas.dto.TourDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.ModoDeViaje;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.hamburguesas.model.ModoDeViaje.A_PIE;
import static com.hamburguesas.model.ModoDeViaje.EN_AUTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre cómo se arma un recorrido.
 *
 * Las coordenadas están puestas a mano sobre una misma avenida, separadas por cuadras
 * parejas, para que el orden correcto se pueda leer de un vistazo: un grado de latitud
 * son 111 km, así que 0,009 son un kilómetro justo.
 */
class TourServiceTest {

    /** Un kilómetro en grados de latitud, para poner los locales a distancias redondas. */
    private static final double UN_KILOMETRO = 0.009;

    private static final double LAT = -34.5900;
    private static final double LON = -58.4300;

    private BurgerJointRepository burgerJointRepository;
    private RatingRepository ratingRepository;
    private WishlistRepository wishlistRepository;
    private SavedTourRepository savedTourRepository;
    private TourService service;

    @BeforeEach
    void setUp() {
        burgerJointRepository = mock(BurgerJointRepository.class);
        ratingRepository = mock(RatingRepository.class);
        wishlistRepository = mock(WishlistRepository.class);
        savedTourRepository = mock(SavedTourRepository.class);
        service = new TourService(
            burgerJointRepository, ratingRepository, wishlistRepository, savedTourRepository);
    }

    /** Un local a tantos kilómetros al sur del punto de partida. */
    private BurgerJoint local(long id, String nombre, double kilometrosAlSur) {
        return local(id, nombre, kilometrosAlSur, "Palermo");
    }

    private BurgerJoint local(long id, String nombre, double kilometrosAlSur, String barrio) {
        return BurgerJoint.builder()
            .id(id).placeId("ChIJ-" + id).name(nombre).address("Una dirección").area(barrio)
            .latitude(LAT - kilometrosAlSur * UN_KILOMETRO).longitude(LON)
            .build();
    }

    private void hay(BurgerJoint... locales) {
        when(burgerJointRepository.findAll()).thenReturn(List.of(locales));
        when(burgerJointRepository.findByFastFoodFalse()).thenReturn(List.of(locales));
    }

    /** Un pedido corriente: cuatro paradas, sin tope de kilómetros, desde el punto de arriba. */
    private TourService.Pedido desdeElPunto(int cantidad, Double kilometrosMaximos) {
        return new TourService.Pedido(cantidad, kilometrosMaximos, List.of(),
            LAT, LON, true, true, A_PIE, false, List.of(), 1L);
    }

    @Test
    void devuelveLaCantidadDeParadasQueSePidieron() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3), local(4, "Cuatro", 4));

        TourDto tour = service.armar(desdeElPunto(3, null), null);

        assertThat(tour.paradas()).hasSize(3);
        assertThat(tour.paradas()).extracting(p -> p.local().name())
            .containsExactly("Una", "Dos", "Tres");
    }

    /**
     * El orden es el que conviene caminar, no el que vino de la base.
     *
     * Siempre se va a la más cercana de las que faltan. Sin esto, un recorrido de cuatro
     * paradas puede hacer zigzag y duplicar los kilómetros.
     */
    @Test
    void vaSiempreALaMasCercanaDeLasQueFaltan() {
        hay(local(1, "Lejos", 5), local(2, "Cerca", 1), local(3, "Media", 3));

        TourDto tour = service.armar(desdeElPunto(3, null), null);

        assertThat(tour.paradas()).extracting(p -> p.local().name())
            .containsExactly("Cerca", "Media", "Lejos");
    }

    /**
     * Los kilómetros son los caminados, no los de la línea recta.
     *
     * Entre dos puntos hay que hacer las cuadras, así que se camina alrededor de un
     * tercio más que la recta. Tres paradas a un kilómetro una de otra son tres
     * kilómetros de recta y 3,9 de vereda.
     */
    @Test
    void cuentaLosKilometrosComoSeCaminanYNoEnLineaRecta() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3));

        TourDto tour = service.armar(desdeElPunto(3, null), null);

        assertThat(tour.kilometros()).isEqualTo(3.9);
        assertThat(tour.paradas()).extracting(com.hamburguesas.dto.TourStopDto::kilometros)
            .containsExactly(1.3, 1.3, 1.3);
    }

    /** Y de los kilómetros sale cuánto lleva: a 4,5 por hora, 3,9 km son 52 minutos. */
    @Test
    void estimaCuantoLleva() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3));

        assertThat(service.armar(desdeElPunto(3, null), null).minutos()).isEqualTo(52);
    }

    /**
     * El tope de kilómetros corta el recorrido, aunque queden hamburgueserías.
     *
     * Es lo que hace que "quiero caminar 3 km" signifique algo: sin esto, pedir cuatro
     * paradas en un barrio grande devuelve cuatro sin importar cuánto haya entre ellas.
     */
    @Test
    void cortaCuandoLaProximaNoEntraEnLosKilometrosQueSeQuierenCaminar() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3), local(4, "Cuatro", 4));

        TourDto tour = service.armar(desdeElPunto(4, 3.0), null);

        // Cada tramo son 1,3 km caminados: dos entran en tres kilómetros y el tercero no.
        assertThat(tour.paradas()).hasSize(2);
        assertThat(tour.kilometros()).isEqualTo(2.6);
    }

    /** Y cuando corta, se dice por qué: pedir cuatro y recibir dos parece una falla. */
    @Test
    void avisaCuandoEntranMenosParadasDeLasPedidas() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3), local(4, "Cuatro", 4));

        assertThat(service.armar(desdeElPunto(4, 3.0), null).aviso())
            .isEqualTo("Caminando 3 km entran 2 paradas");
    }

    @Test
    void avisaCuandoNoHayTantasHamburgueseriasParaElegir() {
        hay(local(1, "Una", 1), local(2, "Dos", 2));

        TourDto tour = service.armar(desdeElPunto(5, null), null);

        assertThat(tour.paradas()).hasSize(2);
        assertThat(tour.aviso()).isEqualTo("Con esos filtros hay 2 hamburgueserías");
    }

    /** Cuando sale entero no hay nada que avisar, y la pantalla no muestra el cartel. */
    @Test
    void sinAvisoCuandoElRecorridoSaleEntero() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3));

        assertThat(service.armar(desdeElPunto(3, null), null).aviso()).isNull();
    }

    @Test
    void soloEntranLasDeLosBarriosPedidos() {
        hay(local(1, "Palermo cerca", 1, "Palermo"),
            local(2, "Boedo", 2, "Boedo"),
            local(3, "Palermo lejos", 3, "Palermo"));

        var pedido = new TourService.Pedido(3, null, List.of("Palermo"), LAT, LON, true, true, A_PIE, false, List.of(), 1L);
        TourDto tour = service.armar(pedido, null);

        assertThat(tour.paradas()).extracting(p -> p.local().name())
            .containsExactly("Palermo cerca", "Palermo lejos");
    }

    /** Sin barrios se toma todo: es el caso de salir desde donde uno está parado. */
    @Test
    void sinBarriosEntranTodas() {
        hay(local(1, "Palermo", 1, "Palermo"), local(2, "Boedo", 2, "Boedo"));

        assertThat(service.armar(desdeElPunto(2, null), null).paradas()).hasSize(2);
    }

    /**
     * Las que ya probó quien pide el tour se pueden dejar afuera.
     *
     * Visitada es la que esa persona puntuó, que es el único registro que hay de haber
     * ido. Sin sesión no hay visitadas y el filtro no cambia nada.
     */
    @Test
    void puedeDejarAfueraLasQueEsaPersonaYaPuntuo() {
        hay(local(1, "Ya fui", 1), local(2, "Nueva", 2));
        when(ratingRepository.idsPuntuadosPor(7L)).thenReturn(List.of(1L));

        var pedido = new TourService.Pedido(2, null, List.of(), LAT, LON, false, true, A_PIE, false, List.of(), 1L);
        TourDto tour = service.armar(pedido, 7L);

        assertThat(tour.paradas()).extracting(p -> p.local().name()).containsExactly("Nueva");
    }

    /** Y si se piden incluidas, la que ya se visitó entra marcada como tal. */
    @Test
    void marcaLasVisitadasCuandoSePidenIncluidas() {
        hay(local(1, "Ya fui", 1), local(2, "Nueva", 2));
        when(ratingRepository.idsPuntuadosPor(7L)).thenReturn(List.of(1L));

        TourDto tour = service.armar(desdeElPunto(2, null), 7L);

        assertThat(tour.paradas()).extracting(p -> p.local().name() + ":" + p.visitada())
            .containsExactly("Ya fui:true", "Nueva:false");
    }

    /** Sin sesión no hay a quién preguntarle qué visitó, y no se le pregunta a la base. */
    @Test
    void sinSesionNoHayVisitadas() {
        hay(local(1, "Una", 1));

        assertThat(service.armar(desdeElPunto(1, null), null).paradas())
            .allSatisfy(p -> assertThat(p.visitada()).isFalse());
    }

    /**
     * Un recorrido de cuatro McDonald's no es un recorrido, así que las cadenas quedan
     * afuera salvo que se pidan.
     */
    @Test
    void sinCadenasPideLosQueNoSonCadena() {
        when(burgerJointRepository.findByFastFoodFalse()).thenReturn(List.of(local(1, "De barrio", 1)));

        var pedido = new TourService.Pedido(1, null, List.of(), LAT, LON, true, false, A_PIE, false, List.of(), 1L);

        assertThat(service.armar(pedido, null).paradas())
            .extracting(p -> p.local().name()).containsExactly("De barrio");
    }

    /** Sin coordenadas no se puede ni ubicar ni medir, así que no puede ser una parada. */
    @Test
    void descartaLasQueNoTienenCoordenadas() {
        BurgerJoint sinPunto = BurgerJoint.builder()
            .id(9L).placeId("ChIJ-9").name("Sin punto").address("Una dirección").area("Palermo")
            .build();
        hay(local(1, "Con punto", 1), sinPunto);

        assertThat(service.armar(desdeElPunto(2, null), null).paradas())
            .extracting(p -> p.local().name()).containsExactly("Con punto");
    }

    @Test
    void sinCandidatosDevuelveUnRecorridoVacioYDiceQuePaso() {
        hay();

        TourDto tour = service.armar(desdeElPunto(4, null), null);

        assertThat(tour.paradas()).isEmpty();
        assertThat(tour.kilometros()).isZero();
        assertThat(tour.aviso()).isEqualTo("No hay hamburgueserías que cumplan con lo que pediste");
    }

    /**
     * Maps admite hasta nueve escalas además del destino, así que un recorrido de más de
     * diez paradas no se podría abrir.
     */
    @Test
    void noPasaDeDiezParadas() {
        BurgerJoint[] muchas = new BurgerJoint[15];
        for (int i = 0; i < muchas.length; i++) {
            muchas[i] = local(i + 1, "Parada " + (i + 1), i + 1);
        }
        hay(muchas);

        assertThat(service.armar(desdeElPunto(15, null), null).paradas()).hasSize(10);
    }

    /**
     * Veinte hamburgueserías: doce bien puntuadas y ocho que no, para poder afirmar de
     * cuál de los dos grupos sale la primera parada.
     */
    private void hayDoceBuenasYOchoMalas() {
        BurgerJoint[] todas = new BurgerJoint[20];
        List<Object[]> notas = new java.util.ArrayList<>();
        for (int i = 0; i < todas.length; i++) {
            long id = i + 1;
            boolean buena = i < 12;
            todas[i] = local(id, (buena ? "Buena " : "Mala ") + id, i + 1);
            notas.add(new Object[] { id, buena ? 4.5 : 1.5, 3L });
        }
        hay(todas);
        when(ratingRepository.promediosPorLocal()).thenReturn(notas);
    }

    /**
     * Sin saber dónde está quien camina, la primera parada sale de las mejor puntuadas.
     *
     * No es la mejor siempre: se sortea entre las de arriba para que volver a pedirlo
     * proponga otro recorrido. Lo que sí vale para cualquier sorteo es que la primera
     * nunca salga del montón de abajo, y eso es lo que se afirma acá.
     */
    @Test
    void sinUbicacionEmpiezaPorUnaDeLasMejorPuntuadas() {
        hayDoceBuenasYOchoMalas();

        for (long semilla = 0; semilla < 25; semilla++) {
            var pedido = new TourService.Pedido(1, null, List.of(), null, null, true, true, A_PIE, false, List.of(), semilla);

            assertThat(service.armar(pedido, null).paradas().get(0).local().name())
                .as("semilla %d", semilla)
                .startsWith("Buena");
        }
    }

    /**
     * Y volver a pedirlo propone otro recorrido.
     *
     * Es la razón de que la primera se sortee en vez de ser siempre la mejor puntuada:
     * un botón que devuelve siempre lo mismo no invita a apretarlo dos veces.
     */
    @Test
    void dosPedidosDistintosProponenRecorridosDistintos() {
        hayDoceBuenasYOchoMalas();

        List<String> primeras = new java.util.ArrayList<>();
        for (long semilla = 0; semilla < 25; semilla++) {
            var pedido = new TourService.Pedido(1, null, List.of(), null, null, true, true, A_PIE, false, List.of(), semilla);
            primeras.add(service.armar(pedido, null).paradas().get(0).local().name());
        }

        // Se repiten, claro: son veinticinco sorteos sobre doce opciones. Lo que importa
        // es que no salga siempre la misma, y que las que salen sean varias.
        assertThat(new java.util.HashSet<>(primeras)).hasSizeGreaterThanOrEqualTo(5);
    }

    /** Con la misma semilla, en cambio, sale el mismo: es lo que lo hace afirmable. */
    @Test
    void laMismaSemillaDaElMismoRecorrido() {
        hayDoceBuenasYOchoMalas();
        var pedido = new TourService.Pedido(3, null, List.of(), null, null, true, true, A_PIE, false, List.of(), 7L);

        assertThat(service.armar(pedido, null).paradas())
            .extracting(p -> p.local().name())
            .isEqualTo(service.armar(pedido, null).paradas().stream()
                .map(p -> p.local().name()).toList());
    }

    /**
     * Y ese primer tramo no cuenta kilómetros: no se sabe desde dónde se sale, así que
     * caminar hasta la primera no es parte del recorrido.
     */
    @Test
    void sinUbicacionElPrimerTramoNoSuma() {
        hay(local(1, "Una", 1), local(2, "Dos", 2));
        var pedido = new TourService.Pedido(2, null, List.of(), null, null, true, true, A_PIE, false, List.of(), 1L);

        TourDto tour = service.armar(pedido, null);

        assertThat(tour.paradas().get(0).kilometros()).isZero();
        assertThat(tour.kilometros()).isEqualTo(1.3);
    }

    /**
     * Con la ubicación, en cambio, caminar hasta la primera sí es parte del recorrido, y
     * cuenta para el tope de kilómetros.
     */
    @Test
    void conUbicacionElPrimerTramoSuma() {
        hay(local(1, "Una", 1), local(2, "Dos", 2));

        TourDto tour = service.armar(desdeElPunto(2, null), null);

        assertThat(tour.paradas().get(0).kilometros()).isEqualTo(1.3);
        assertThat(tour.kilometros()).isEqualTo(2.6);
    }

    /**
     * El tope nunca deja el recorrido vacío: si la primera ya queda lejos, se la acepta
     * igual. Un recorrido de una parada es una respuesta; uno de ninguna parece un error.
     */
    @Test
    void elTopeNoDejaElRecorridoEnCero() {
        hay(local(1, "Lejísimos", 20));

        TourDto tour = service.armar(desdeElPunto(3, 1.0), null);

        assertThat(tour.paradas()).hasSize(1);
    }

    /** Entre cuántas se eligió, para que la pantalla pueda decir si el filtro quedó chico. */
    @Test
    void cuentaEntreCuantasEligio() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3));

        assertThat(service.armar(desdeElPunto(1, null), null).candidatos()).isEqualTo(3);
    }

    private TourService.Pedido enAuto(int cantidad, Double kilometrosMaximos) {
        return new TourService.Pedido(cantidad, kilometrosMaximos, List.of(),
            LAT, LON, true, true, EN_AUTO, false, List.of(), 1L);
    }

    /**
     * En auto se dan más vueltas que a pie.
     *
     * Casi todas las calles de la Ciudad son de una sola mano: donde el que camina cruza
     * y sigue, el auto da la vuelta a la manzana. Los mismos tres tramos de un kilómetro
     * son 3,9 km caminando y 5,4 manejando.
     */
    @Test
    void enAutoSeRecorrenMasKilometrosQueAPie() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3));

        assertThat(service.armar(enAuto(3, null), null).kilometros()).isEqualTo(5.4);
        assertThat(service.armar(desdeElPunto(3, null), null).kilometros()).isEqualTo(3.9);
    }

    /** Y se tarda bastante menos, que es de lo que se trata elegir el auto. */
    @Test
    void enAutoSeTardaMenos() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3));

        assertThat(service.armar(enAuto(3, null), null).minutos()).isEqualTo(20);
        assertThat(service.armar(desdeElPunto(3, null), null).minutos()).isEqualTo(52);
    }

    /**
     * El tope de kilómetros se mide con las vueltas del modo elegido.
     *
     * Los mismos cuatro kilómetros dan para tres paradas a pie y para dos en auto: los
     * tramos son más largos, así que el tope corta antes.
     */
    @Test
    void elTopeCuentaLosKilometrosDelModoElegido() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3), local(4, "Cuatro", 4));

        // En auto cada tramo son 1,8 km: dos entran en cuatro y el tercero se pasa.
        TourDto tour = service.armar(enAuto(4, 4.0), null);
        assertThat(tour.paradas()).hasSize(2);
        assertThat(tour.kilometros()).isEqualTo(3.6);

        // A pie son 1,3, y entran tres.
        assertThat(service.armar(desdeElPunto(4, 4.0), null).paradas()).hasSize(3);
    }

    /** Y el aviso lo dice como corresponde, que no es "caminando" si se va en auto. */
    @Test
    void elAvisoHablaDelModoElegido() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3), local(4, "Cuatro", 4));

        assertThat(service.armar(enAuto(4, 4.0), null).aviso())
            .isEqualTo("En auto 4 km entran 2 paradas");
    }

    private List<Long> idsDe(TourDto tour) {
        return tour.paradas().stream().map(p -> p.local().id()).sorted().toList();
    }

    /** Cinco locales en fila, que dan para varios recorridos distintos de tres paradas. */
    private void hayCinco() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3),
            local(4, "Cuatro", 4), local(5, "Cinco", 5));
    }

    /**
     * Pedir otro, y otro, tiene que proponer uno nuevo cada vez.
     *
     * Con la ubicación puesta antes salía siempre el mismo: la primera parada era la más
     * cercana, y desde un punto fijo esa no cambia nunca, así que el resto venía detrás
     * igual. La variedad no sale de sortear —el recorrido que se propone sigue siendo el
     * mejor posible— sino de descartar lo ya propuesto: la pantalla manda lo que tiene, y
     * el que sale arranca en otro lado.
     */
    @Test
    void pedirOtroUnaYOtraVezProponeUnoNuevoCadaVez() {
        hayCinco();

        List<List<Long>> salieron = new java.util.ArrayList<>();

        for (int vuelta = 0; vuelta < 3; vuelta++) {
            var pedido = new TourService.Pedido(3, null, List.of(), LAT, LON, true, true,
                A_PIE, false, List.copyOf(salieron), 1L);
            salieron.add(idsDe(service.armar(pedido, null)));
        }

        assertThat(salieron).doesNotHaveDuplicates().hasSize(3);
    }

    /**
     * Y el que está en pantalla no puede volver a salir: la pantalla manda sus ids, y con
     * eso "armar otro" arma otro de verdad y no uno que tal vez sea el mismo.
     */
    @Test
    void noRepiteElRecorridoQueEstaEnPantalla() {
        hayCinco();
        var primero = new TourService.Pedido(3, null, List.of(), LAT, LON, true, true,
            A_PIE, false, List.of(), 1L);
        List<Long> enPantalla = idsDe(service.armar(primero, null));

        // La misma semilla: sin la exclusión saldría exactamente el mismo.
        var otro = new TourService.Pedido(3, null, List.of(), LAT, LON, true, true,
            A_PIE, false, List.of(enPantalla), 1L);

        assertThat(idsDe(service.armar(otro, null))).isNotEqualTo(enPantalla);
    }

    /** Un recorrido guardado tampoco se propone de nuevo, si se pidió no repetirlos. */
    @Test
    void noProponeUnRecorridoQueYaEstaGuardado() {
        hayCinco();
        var pedido = new TourService.Pedido(3, null, List.of(), LAT, LON, true, true,
            A_PIE, false, List.of(), 1L);
        List<Long> yaGuardado = idsDe(service.armar(pedido, null));
        guardadoCon(yaGuardado);

        var evitando = new TourService.Pedido(3, null, List.of(), LAT, LON, true, true,
            A_PIE, true, List.of(), 1L);

        assertThat(idsDe(service.armar(evitando, 7L))).isNotEqualTo(yaGuardado);
    }

    /** Sin pedirlo, en cambio, los guardados no molestan: puede repetirse uno a propósito. */
    @Test
    void sinPedirloLosGuardadosNoSeMiran() {
        hayCinco();
        var pedido = new TourService.Pedido(3, null, List.of(), LAT, LON, true, true,
            A_PIE, false, List.of(), 1L);
        List<Long> yaGuardado = idsDe(service.armar(pedido, null));
        guardadoCon(yaGuardado);

        assertThat(idsDe(service.armar(pedido, 7L))).isEqualTo(yaGuardado);
        verify(savedTourRepository, never()).findByUser_IdOrderByCreatedAtDesc(any());
    }

    /** Sin sesión no hay recorridos guardados que mirar, y no se le pregunta a la base. */
    @Test
    void sinSesionNoSeMiranLosGuardados() {
        hayCinco();
        var pedido = new TourService.Pedido(3, null, List.of(), LAT, LON, true, true,
            A_PIE, true, List.of(), 1L);

        service.armar(pedido, null);

        verify(savedTourRepository, never()).findByUser_IdOrderByCreatedAtDesc(any());
    }

    /**
     * Cuando ya no queda ninguno nuevo se devuelve uno repetido, pero diciéndolo: con tres
     * hamburgueserías y tres paradas hay un solo recorrido posible, y guardarlo agota el
     * barrio. Devolver vacío sería peor: la pantalla se quedaría sin nada que mostrar.
     */
    @Test
    void cuandoNoQuedaNingunoNuevoLoDice() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3));
        var pedido = new TourService.Pedido(3, null, List.of(), LAT, LON, true, true,
            A_PIE, true, List.of(), 1L);
        guardadoCon(List.of(1L, 2L, 3L));

        TourDto tour = service.armar(pedido, 7L);

        assertThat(tour.paradas()).hasSize(3);
        assertThat(tour.aviso())
            .isEqualTo("Ya recorriste todo lo que entra con estos filtros: este se repite");
    }

    /**
     * Dos recorridos son el mismo cuando pasan por los mismos locales, sin importar el
     * orden: las mismas tres paradas caminadas al revés son la misma salida.
     */
    @Test
    void elMismoRecorridoAlRevesCuentaComoElMismo() {
        hay(local(1, "Una", 1), local(2, "Dos", 2), local(3, "Tres", 3));
        guardadoCon(List.of(3L, 2L, 1L));

        var pedido = new TourService.Pedido(3, null, List.of(), LAT, LON, true, true,
            A_PIE, true, List.of(), 1L);

        assertThat(service.armar(pedido, 7L).aviso())
            .isEqualTo("Ya recorriste todo lo que entra con estos filtros: este se repite");
    }

    /** Un recorrido guardado con esos locales, como lo devolvería la base. */
    private void guardadoCon(List<Long> ids) {
        com.hamburguesas.model.SavedTour tour = com.hamburguesas.model.SavedTour.builder()
            .id(1L)
            .stops(ids.stream().map(id -> com.hamburguesas.model.SavedTourStop.builder()
                .burgerJoint(BurgerJoint.builder().id(id).build())
                .build()).toList())
            .build();

        when(savedTourRepository.findByUser_IdOrderByCreatedAtDesc(7L)).thenReturn(List.of(tour));
    }

    /**
     * Mandando solo el último, pedir otro alternaba entre dos recorridos.
     *
     * El nuevo excluía al que estaba en pantalla, y el siguiente volvía al primero porque
     * ya no estaba prohibido: A, B, A, B. Por eso la exclusión es acumulativa, y este test
     * afirma justamente lo que antes fallaba.
     */
    @Test
    void mandandoSoloElUltimoVolveriaAAlternarEntreDos() {
        hayCinco();

        var primera = new TourService.Pedido(3, null, List.of(), LAT, LON, true, true,
            A_PIE, false, List.of(), 1L);
        List<Long> a = idsDe(service.armar(primera, null));

        var segunda = new TourService.Pedido(3, null, List.of(), LAT, LON, true, true,
            A_PIE, false, List.of(a), 1L);
        List<Long> b = idsDe(service.armar(segunda, null));

        // Con los dos, el tercero tiene que ser uno nuevo y no volver a "a".
        var tercera = new TourService.Pedido(3, null, List.of(), LAT, LON, true, true,
            A_PIE, false, List.of(a, b), 1L);
        List<Long> c = idsDe(service.armar(tercera, null));

        assertThat(c).isNotEqualTo(a).isNotEqualTo(b);
    }
}
