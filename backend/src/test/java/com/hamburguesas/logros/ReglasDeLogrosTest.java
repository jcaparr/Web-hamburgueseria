package com.hamburguesas.logros;

import com.hamburguesas.logros.CifrasDeLogros.Cifras;
import com.hamburguesas.logros.CifrasDeLogros.Resenia;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Una regla por test: qué cifras dan cada logro y cuáles no (#314). Sin base de datos. */
class ReglasDeLogrosTest {

    private static final String LARGO =
        "Muy buena la carne, el pan estaba tostado justo y la salsa me encantó bastante; las papas también salieron crocantes";

    private static Resenia resenia(long id, long local, String area, boolean cadena, String nombre, Instant cuando,
                                   String comentario, int fotos, boolean primera) {
        return new Resenia(id, local, area, cadena, nombre, cuando, comentario, fotos, primera);
    }

    private static Resenia en(long local, String area) {
        return resenia(local, local, area, false, "Local " + local, Instant.parse("2026-10-20T15:00:00Z"), "ok", 1, false);
    }

    private static Cifras sin() {
        return con(List.of());
    }

    private static Cifras con(List<Resenia> resenias) {
        return new Cifras(resenias, 0, 0, 0, 0, 0, false, 0, 0, 0, null, false, false);
    }

    private static Cifras con(java.util.function.UnaryOperator<Builder> cambio) {
        return cambio.apply(new Builder()).hacer();
    }

    /** Cifras con un campo cambiado, sin repetir los trece parámetros en cada test. */
    private static final class Builder {
        List<Resenia> resenias = List.of();
        long seguidores, guardadas, recorridos, reaccionesQueDio, masReacciones, risas, despues, conGuardada;
        boolean combo, pionero, hamburguesa;
        String intendente;

        Builder seguidores(long n) { seguidores = n; return this; }
        Builder guardadas(long n) { guardadas = n; return this; }
        Builder recorridos(long n) { recorridos = n; return this; }
        Builder reaccionesQueDio(long n) { reaccionesQueDio = n; return this; }
        Builder masReacciones(long n) { masReacciones = n; return this; }
        Builder risas(long n) { risas = n; return this; }
        Builder despues(long n) { despues = n; return this; }
        Builder conGuardada(long n) { conGuardada = n; return this; }
        Builder combo() { combo = true; return this; }
        Builder pionero() { pionero = true; return this; }
        Builder hamburguesa() { hamburguesa = true; return this; }
        Builder intendente(String barrio) { intendente = barrio; return this; }
        Builder resenias(List<Resenia> r) { resenias = r; return this; }

        Cifras hacer() {
            return new Cifras(resenias, seguidores, guardadas, recorridos, reaccionesQueDio, masReacciones, combo,
                risas, despues, conGuardada, intendente, pionero, hamburguesa);
        }
    }

    private static List<Resenia> resenias(int cuantas, java.util.function.IntFunction<Resenia> como) {
        return IntStream.range(0, cuantas).mapToObj(como).toList();
    }

    @Test
    void elCatalogoTieneTreintaYOchoLogrosYTreintaYDosSeVen() {
        assertThat(Logro.values()).hasSize(38);
        assertThat(Logro.visibles()).isEqualTo(32);
        assertThat(java.util.Arrays.stream(Logro.values()).filter(Logro::secreto)).hasSize(6);
    }

    @Test
    void elSliderEsParaTodaCuenta() {
        assertThat(Logros.cumple(Logro.NIVEL_1, sin())).isTrue();
    }

    @Test
    void losNivelesSalenDeLaXpDeLasResenias() {
        // 100 + 70 + 70 + 30 = 270 → nivel 3: Triplequé sí, Burger bro no.
        Cifras c = con(b -> b.resenias(List.of(
            resenia(1, 1, "Palermo", false, "A", Instant.now(), LARGO, 1, false),
            en(2, "Palermo"), en(3, "Palermo"))));
        assertThat(Logros.cumple(Logro.NIVEL_3, c)).isTrue();
        assertThat(Logros.cumple(Logro.NIVEL_5, c)).isFalse();
    }

    @Test
    void losSeguidoresPorTramo() {
        Cifras c = con(b -> b.seguidores(50));
        assertThat(Logros.cumple(Logro.SEGUIDORES_10, c)).isTrue();
        assertThat(Logros.cumple(Logro.SEGUIDORES_50, c)).isTrue();
        assertThat(Logros.cumple(Logro.SEGUIDORES_100, c)).isFalse();
        assertThat(Logros.cumple(Logro.SEGUIDORES_1000, con(b -> b.seguidores(1000)))).isTrue();
    }

    @Test
    void clienteDeLaEsquinaSonTresReseniasEnElMismoBarrio() {
        assertThat(Logros.cumple(Logro.BARRIO_ESQUINA, con(b -> b.resenias(List.of(en(1, "Palermo"), en(2, "Palermo")))))).isFalse();
        assertThat(Logros.cumple(Logro.BARRIO_ESQUINA,
            con(b -> b.resenias(List.of(en(1, "Palermo"), en(2, "Palermo"), en(3, "Palermo")))))).isTrue();
    }

    @Test
    void losBarriosCuentanDistintosYNoCuentanCadenasNiLocalesSinBarrio() {
        String[] barrios = {"A", "B", "C", "D", "E"};
        List<Resenia> cinco = resenias(5, i -> en(i + 1, barrios[i]));
        assertThat(Logros.cumple(Logro.BARRIOS_5, con(b -> b.resenias(cinco)))).isTrue();

        List<Resenia> conCadenaYSinBarrio = new ArrayList<>(resenias(3, i -> en(i + 1, barrios[i])));
        conCadenaYSinBarrio.add(resenia(10, 10, "D", true, "Burger King", Instant.now(), "ok", 1, false));
        conCadenaYSinBarrio.add(resenia(11, 11, null, false, "Sin barrio", Instant.now(), "ok", 1, false));
        assertThat(Logros.cumple(Logro.BARRIOS_5, con(b -> b.resenias(conCadenaYSinBarrio)))).isFalse();

        List<Resenia> quince = resenias(15, i -> en(i + 1, "Barrio " + i));
        assertThat(Logros.cumple(Logro.BARRIOS_15, con(b -> b.resenias(quince)))).isTrue();
        assertThat(Logros.cumple(Logro.BARRIOS_25, con(b -> b.resenias(quince)))).isFalse();
        List<Resenia> veinticinco = resenias(25, i -> en(i + 1, "Barrio " + i));
        assertThat(Logros.cumple(Logro.BARRIOS_25, con(b -> b.resenias(veinticinco)))).isTrue();
    }

    @Test
    void elIntendenteDependeDelBarrioQueCalculaLaConsulta() {
        assertThat(Logros.cumple(Logro.INTENDENTE, sin())).isFalse();
        assertThat(Logros.cumple(Logro.INTENDENTE, con(b -> b.intendente("Palermo")))).isTrue();
    }

    @Test
    void timidoEsLaPrimeraResenia() {
        assertThat(Logros.cumple(Logro.PRIMERA_RESENIA, sin())).isFalse();
        assertThat(Logros.cumple(Logro.PRIMERA_RESENIA, con(b -> b.resenias(List.of(en(1, "A")))))).isTrue();
    }

    @Test
    void picassoEsArmarLaHamburguesa() {
        assertThat(Logros.cumple(Logro.PICASSO, sin())).isFalse();
        assertThat(Logros.cumple(Logro.PICASSO, con(Builder::hamburguesa))).isTrue();
    }

    @Test
    void ansiosoEsTenerUnaReseniaMarcadaComoPrimeraConComentario() {
        assertThat(Logros.cumple(Logro.ANSIOSO, con(b -> b.resenias(List.of(en(1, "A")))))).isFalse();
        Resenia primera = resenia(1, 1, "A", false, "L", Instant.now(), LARGO, 1, true);
        assertThat(Logros.cumple(Logro.ANSIOSO, con(b -> b.resenias(List.of(primera))))).isTrue();
    }

    @Test
    void hambreAjenaSonDiezReaccionesEnUnaResenia() {
        assertThat(Logros.cumple(Logro.HAMBRE_AJENA, con(b -> b.masReacciones(9)))).isFalse();
        assertThat(Logros.cumple(Logro.HAMBRE_AJENA, con(b -> b.masReacciones(10)))).isTrue();
    }

    @Test
    void planificadorSonDiezGuardadas() {
        assertThat(Logros.cumple(Logro.PLANIFICADOR, con(b -> b.guardadas(9)))).isFalse();
        assertThat(Logros.cumple(Logro.PLANIFICADOR, con(b -> b.guardadas(10)))).isTrue();
    }

    @Test
    void stalkerSonTreintaReaccionesDadas() {
        assertThat(Logros.cumple(Logro.STALKER, con(b -> b.reaccionesQueDio(29)))).isFalse();
        assertThat(Logros.cumple(Logro.STALKER, con(b -> b.reaccionesQueDio(30)))).isTrue();
    }

    @Test
    void primerTourYGuiaTuristico() {
        assertThat(Logros.cumple(Logro.PRIMER_TOUR, sin())).isFalse();
        assertThat(Logros.cumple(Logro.PRIMER_TOUR, con(b -> b.recorridos(1)))).isTrue();
        assertThat(Logros.cumple(Logro.GUIA, con(b -> b.recorridos(4)))).isFalse();
        assertThat(Logros.cumple(Logro.GUIA, con(b -> b.recorridos(5)))).isTrue();
    }

    @Test
    void albumEsUnaReseniaConTresFotos() {
        Resenia dos = resenia(1, 1, "A", false, "L", Instant.now(), "ok", 2, false);
        Resenia tres = resenia(2, 2, "A", false, "L2", Instant.now(), "ok", 3, false);
        assertThat(Logros.cumple(Logro.ALBUM, con(b -> b.resenias(List.of(dos))))).isFalse();
        assertThat(Logros.cumple(Logro.ALBUM, con(b -> b.resenias(List.of(dos, tres))))).isTrue();
    }

    @Test
    void promesaCumplidaSonCincoLocalesGuardadosAntes() {
        assertThat(Logros.cumple(Logro.PROMESA, con(b -> b.conGuardada(4)))).isFalse();
        assertThat(Logros.cumple(Logro.PROMESA, con(b -> b.conGuardada(5)))).isTrue();
    }

    @Test
    void deAutorSonDiezLocalesQueNoSonCadena() {
        List<Resenia> nueve = resenias(9, i -> en(i + 1, "A"));
        assertThat(Logros.cumple(Logro.DE_AUTOR, con(b -> b.resenias(nueve)))).isFalse();
        List<Resenia> diez = new ArrayList<>(nueve);
        diez.add(en(10, "A"));
        assertThat(Logros.cumple(Logro.DE_AUTOR, con(b -> b.resenias(diez)))).isTrue();
        // Una cadena no suma.
        List<Resenia> conCadena = new ArrayList<>(nueve);
        conCadena.add(resenia(10, 10, "A", true, "Mostaza", Instant.now(), "ok", 1, false));
        assertThat(Logros.cumple(Logro.DE_AUTOR, con(b -> b.resenias(conCadena)))).isFalse();
    }

    @Test
    void pioneroEsDeLasPrimerasDiezCuentas() {
        assertThat(Logros.cumple(Logro.PIONERO, sin())).isFalse();
        assertThat(Logros.cumple(Logro.PIONERO, con(Builder::pionero))).isTrue();
    }

    @Test
    void losSecretosDeLosDemas() {
        assertThat(Logros.cumple(Logro.COMBO, con(Builder::combo))).isTrue();
        assertThat(Logros.cumple(Logro.COMBO, sin())).isFalse();
        assertThat(Logros.cumple(Logro.BOCA_EN_BOCA, con(b -> b.despues(2)))).isFalse();
        assertThat(Logros.cumple(Logro.BOCA_EN_BOCA, con(b -> b.despues(3)))).isTrue();
        assertThat(Logros.cumple(Logro.COMEDIANTE, con(b -> b.risas(9)))).isFalse();
        assertThat(Logros.cumple(Logro.COMEDIANTE, con(b -> b.risas(10)))).isTrue();
    }

    /** La 1 a las 7 de la mañana, hora de Buenos Aires: 04:00 UTC es 01:00 allá. */
    @Test
    void bajoneroEsPublicarEntreLa1YLas7EnBuenosAires() {
        Resenia a1 = resenia(1, 1, "A", false, "L", Instant.parse("2026-10-20T04:00:00Z"), "ok", 1, false);
        Resenia a6 = resenia(2, 2, "A", false, "L", Instant.parse("2026-10-20T09:59:00Z"), "ok", 1, false);
        Resenia a7 = resenia(3, 3, "A", false, "L", Instant.parse("2026-10-20T10:00:00Z"), "ok", 1, false);
        Resenia a0 = resenia(4, 4, "A", false, "L", Instant.parse("2026-10-20T03:59:00Z"), "ok", 1, false);
        assertThat(Logros.cumple(Logro.BAJONERO, con(b -> b.resenias(List.of(a1))))).isTrue();
        assertThat(Logros.cumple(Logro.BAJONERO, con(b -> b.resenias(List.of(a6))))).isTrue();
        assertThat(Logros.cumple(Logro.BAJONERO, con(b -> b.resenias(List.of(a7))))).isFalse();
        assertThat(Logros.cumple(Logro.BAJONERO, con(b -> b.resenias(List.of(a0))))).isFalse();
    }

    /** El 28 de mayo en Buenos Aires: a las 02:30 UTC del 29 todavía es 28 allá. */
    @Test
    void patriotaEsPublicarUn28DeMayoEnBuenosAires() {
        Resenia es28 = resenia(1, 1, "A", false, "L", Instant.parse("2026-05-29T02:30:00Z"), "ok", 1, false);
        Resenia es29 = resenia(2, 2, "A", false, "L", Instant.parse("2026-05-29T03:30:00Z"), "ok", 1, false);
        assertThat(Logros.cumple(Logro.PATRIOTA, con(b -> b.resenias(List.of(es28))))).isTrue();
        assertThat(Logros.cumple(Logro.PATRIOTA, con(b -> b.resenias(List.of(es29))))).isFalse();
    }

    @Test
    void paladarDePlasticoSonLasTresMarcasPorElPrincipioDelNombre() {
        Resenia bk = resenia(1, 1, "A", true, "Burger King - Sucursal P.Italia", Instant.now(), "ok", 1, false);
        Resenia mc = resenia(2, 2, "A", true, "McDonald's Abasto", Instant.now(), "ok", 1, false);
        Resenia wendys = resenia(3, 3, "A", true, "Wendy's Palermo", Instant.now(), "ok", 1, false);
        Resenia otra = resenia(4, 4, "A", false, "Casa de Burger King", Instant.now(), "ok", 1, false);
        assertThat(Logros.cumple(Logro.PALADAR, con(b -> b.resenias(List.of(bk, mc))))).isFalse();
        assertThat(Logros.cumple(Logro.PALADAR, con(b -> b.resenias(List.of(bk, mc, wendys))))).isTrue();
        // Una mención al medio del nombre no es la marca.
        assertThat(Logros.cumple(Logro.PALADAR, con(b -> b.resenias(List.of(otra, mc, wendys))))).isFalse();
    }
}
