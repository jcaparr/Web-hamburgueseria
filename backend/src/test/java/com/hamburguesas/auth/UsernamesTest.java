package com.hamburguesas.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Las reglas del nombre de usuario, que valen igual en los tres lugares donde se pide. */
class UsernamesTest {

    @Test
    void loQueSeEscribeSeGuardaEnMinusculas() {
        assertThat(Usernames.normalizar("  JuanCa  ")).isEqualTo("juanca");
    }

    @Test
    void sinNadaEscritoNoSeRompe() {
        assertThat(Usernames.normalizar(null)).isEmpty();
    }

    @Test
    void aceptaLetrasNumerosYGuionBajo() {
        assertThat(Usernames.tieneFormaValida("juan_ca2")).isTrue();
    }

    /**
     * Sin acentos ni puntos a propósito: "josé" y "jose" se leen igual de lejos, y dos
     * nombres que se confunden es justo lo que hace falta para hacerse pasar por otro.
     */
    @Test
    void rechazaLoQueSeConfundeConOtroNombre() {
        assertThat(Usernames.tieneFormaValida("josé")).isFalse();
        assertThat(Usernames.tieneFormaValida("juan.ca")).isFalse();
        assertThat(Usernames.tieneFormaValida("juan ca")).isFalse();
        assertThat(Usernames.tieneFormaValida("juan-ca")).isFalse();
    }

    @Test
    void rechazaLosDemasiadoCortosYLosDemasiadoLargos() {
        assertThat(Usernames.tieneFormaValida("ab")).isFalse();
        assertThat(Usernames.tieneFormaValida("a".repeat(21))).isFalse();
        assertThat(Usernames.tieneFormaValida("abc")).isTrue();
        assertThat(Usernames.tieneFormaValida("a".repeat(20))).isTrue();
    }

    @Test
    void nadiePuedeLlamarseComoLaAppNiComoQuienLaAtiende() {
        assertThat(Usernames.esReservado("soporte")).isTrue();
        assertThat(Usernames.esReservado("admin")).isTrue();
        assertThat(Usernames.esReservado("juanca")).isFalse();
    }

    @Test
    void proponeAlgoSacadoDelEmail() {
        assertThat(Usernames.baseDesdeEmail("juan.caparros@gmail.com")).isEqualTo("juancaparros");
    }

    /** Un email que no deja nada usable no puede terminar en un nombre inválido. */
    @Test
    void siDelEmailNoSaleNadaUsableProponeAlgoValido() {
        String propuesto = Usernames.baseDesdeEmail("é.ñ@gmail.com");

        assertThat(Usernames.tieneFormaValida(propuesto)).isTrue();
    }

    /** Con el margen justo para agregarle un número si ya está tomado. */
    @Test
    void loQueProponeDejaLugarParaElSufijo() {
        String propuesto = Usernames.baseDesdeEmail("a".repeat(40) + "@gmail.com");

        assertThat(propuesto).hasSize(Usernames.MAXIMO - 5);
        assertThat(Usernames.tieneFormaValida(propuesto + "20")).isTrue();
    }
}
