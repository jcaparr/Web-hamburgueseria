package com.hamburguesas.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.util.unit.DataSize;

import java.time.Duration;

/**
 * Los topes de "Sugerir un local" (#257), para que no lo revienten con un bot ni con
 * muchas cuentas. Los de "por día" cuentan las últimas 24 horas.
 */
@Data
@ConfigurationProperties(prefix = "app.sugerencias")
public class SugerenciasProperties {

    /** A dónde llega el aviso de cada una. Cloudflare reenvía hola@ a la casilla de Juan. */
    private String mailA = "hola@burgometro.com.ar";

    /** Cuántas puede mandar una misma persona por día. */
    private int topePorPersona = 5;

    /** Lo que tiene que esperar una persona entre una y otra. */
    private Duration minimoEntreUnaYOtra = Duration.ofMinutes(1);

    /**
     * Cuántas avisan por mail por día, entre todos. Las que pasan se guardan igual, sin
     * mail: así nunca se comen los 300 diarios de Brevo que necesitan los códigos.
     */
    private int topeDeMails = 20;

    /** Dónde se guardan las fotos opcionales (#283). Ninguna ruta de la web las sirve. */
    private String directorioDeFotos = "./data/sugerencias-fotos";

    /** Lo que puede pesar la foto que se sube. Lo que se guarda pesa mucho menos. */
    private DataSize pesoMaximoDeFoto = DataSize.ofMegabytes(8);

    /** Cuántas se aceptan por día, entre todos. Pasado eso se rechazan. */
    private int topeTotal = 100;
}
