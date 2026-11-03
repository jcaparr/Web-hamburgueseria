package com.hamburguesas.service;

import com.hamburguesas.service.TemasDeLasResenias.Mencion;
import com.hamburguesas.service.TemasDeLasResenias.Tema;
import com.hamburguesas.service.TemasDeLasResenias.Tono;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lo que se prueba acá es qué cuenta como mención y qué no.
 *
 * Es la parte que puede equivocarse en silencio: un tema contado de más no rompe nada,
 * sale publicado como si fuera lo que la gente dijo.
 */
class TemasDeLasReseniasTest {

    private static Mencion buena(String comentario) {
        return new Mencion(5, comentario);
    }

    private static Mencion mala(String comentario) {
        return new Mencion(2, comentario);
    }

    @Test
    void cuentaDeQueHablanLasResenias() {
        List<Tema> temas = TemasDeLasResenias.de(List.of(
            buena("La carne espectacular"),
            buena("Muy buena la carne, y las papas también"),
            mala("La carne venía cruda")));

        assertThat(temas).hasSize(1);
        assertThat(temas.get(0).nombre()).isEqualTo("La carne");
        assertThat(temas.get(0).menciones()).isEqualTo(3);
        assertThat(temas.get(0).aFavor()).isEqualTo(2);
    }

    /** Con dos reseñas escritas no hay tendencia que resumir, hay dos personas. */
    @Test
    void conPocasReseniasEscritasNoResumeNada() {
        assertThat(TemasDeLasResenias.de(List.of(
            buena("La carne tremenda"),
            buena("Qué carne"))))
            .isEmpty();
    }

    /**
     * Una reseña sin texto no aporta ni cuenta para el mínimo: si contara, tres
     * personas que solo pusieron estrellas alcanzarían para publicar un resumen
     * armado con un solo comentario.
     */
    @Test
    void lasReseniasSinTextoNoCuentanParaElMinimo() {
        assertThat(TemasDeLasResenias.de(List.of(
            buena("La carne tremenda"),
            buena("Qué carne"),
            buena(null),
            buena("   "))))
            .isEmpty();
    }

    /** Un tema que nombró una sola persona no es un tema. */
    @Test
    void dejaAfueraLoQueNombroUnoSolo() {
        List<Tema> temas = TemasDeLasResenias.de(List.of(
            buena("La carne y el pan, impecables"),
            buena("La carne muy rica"),
            buena("Buenísima la carne")));

        assertThat(temas).extracting(Tema::nombre).containsExactly("La carne");
    }

    /**
     * El revés de la medalla: lo que se cuenta es de qué habla la gente, no si habla
     * bien. Un local donde todos se quejan de la espera tiene que poder decirlo.
     */
    @Test
    void unTemaDelQueTodosHablanMalTambienSeCuenta() {
        List<Tema> temas = TemasDeLasResenias.de(List.of(
            mala("Una demora eterna"),
            mala("La espera fue larga"),
            mala("Tardaron una hora")));

        assertThat(temas).hasSize(1);
        assertThat(temas.get(0).nombre()).isEqualTo("La espera");
        assertThat(temas.get(0).aFavor()).isZero();
    }

    /** Escrito desde el teléfono: sin acentos, con signos y en mayúsculas. */
    @Test
    void entiendeLoQueSeEscribeApurado() {
        List<Tema> temas = TemasDeLasResenias.de(List.of(
            buena("¡LAS PAPAS!"),
            buena("la atencion, muy buena"),
            buena("Buena atención y buenas papas")));

        assertThat(temas).extracting(Tema::nombre)
            .containsExactlyInAnyOrder("Las papas", "La atención");
    }

    /**
     * El caso que hacía falta atajar: "panceta" tiene "pan" adentro, y en una
     * hamburguesería se nombra seguido. Sin buscar la palabra entera, cada panceta
     * contaba como alguien hablando del pan.
     */
    @Test
    void noConfundeUnaPalabraConUnPedazoDeOtra() {
        assertThat(TemasDeLasResenias.de(List.of(
            buena("Con panceta"),
            buena("La panceta crocante"),
            buena("Mucha panceta"))))
            .extracting(Tema::nombre)
            .containsExactly("La panceta");
    }

    /** "Mayo" es la salsa y también el mes: se deja afuera para no contar "fui en mayo". */
    @Test
    void noCuentaPalabrasQueSignificanOtraCosa() {
        assertThat(TemasDeLasResenias.de(List.of(
            buena("Fui en mayo"),
            buena("Volví en mayo"),
            buena("Otra vez en mayo"),
            buena("Me gustó mucho"),
            buena("Me gusto todo"))))
            .isEmpty();
    }

    /** Las palabras nuevas de #392, escritas como se escriben: con eñe y con acento. */
    @Test
    void reconoceLosTemasNuevos() {
        List<Tema> temas = TemasDeLasResenias.de(List.of(
            buena("Muy sabrosa, buen tamaño y la salsa barbacoa un golazo"),
            buena("Rico sabor, porción abundante, salsas caseras"),
            buena("La cerveza fría y la cebolla caramelizada")));

        assertThat(temas).extracting(Tema::nombre)
            .containsExactlyInAnyOrder("La salsa", "El sabor", "El tamaño");
    }

    /** Quien escribe "la carne, qué carne" habló de la carne una vez, no dos. */
    @Test
    void unaReseniaCuentaUnaVezPorTemaAunqueInsista() {
        List<Tema> temas = TemasDeLasResenias.de(List.of(
            buena("La carne, qué carne, la mejor carne"),
            buena("Muy buena la carne"),
            buena("Carne impecable")));

        assertThat(temas.get(0).menciones()).isEqualTo(3);
    }

    /** De los ocho nombrados se muestran seis: la ficha no es un informe. */
    @Test
    void muestraComoMuchoSeisTemas() {
        List<Mencion> resenias = List.of(
            buena("La carne, el pan, las papas, el queso, el precio, la atención, la espera y el lugar"),
            buena("La carne, el pan, las papas, el queso, el precio, la atención, la espera y el lugar"),
            buena("La carne, el pan, las papas, el queso, el precio, la atención, la espera y el lugar"));

        assertThat(TemasDeLasResenias.de(resenias)).hasSize(6);
    }

    // ---- para qué lado juega cada tema ----

    @Test
    void dosTerciosABuenaNotaEsAFavor() {
        assertThat(new Tema("La carne", 3, 2).tono()).isEqualTo(Tono.A_FAVOR);
    }

    @Test
    void unTercioOMenosEsEnContra() {
        assertThat(new Tema("La espera", 3, 1).tono()).isEqualTo(Tono.EN_CONTRA);
        assertThat(new Tema("La espera", 4, 0).tono()).isEqualTo(Tono.EN_CONTRA);
    }

    /** El borde: la mitad no juega para ningún lado. */
    @Test
    void laMitadEsDividido() {
        assertThat(new Tema("El precio", 4, 2).tono()).isEqualTo(Tono.DIVIDIDO);
        assertThat(new Tema("El precio", 5, 3).tono()).isEqualTo(Tono.DIVIDIDO);
    }
}
