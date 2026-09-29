package com.hamburguesas.dto;

import com.hamburguesas.model.ModoDeViaje;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Lo que hace falta para guardar un recorrido: por dónde pasa y cómo se hace.
 *
 * Los kilómetros y los minutos no vienen de acá aunque la pantalla los tenga: los
 * recalcula el servidor con las paradas. Son números que se muestran como propios, y no
 * hay razón para creerle a lo que mande el navegador.
 */
public record GuardarTourRequest(
    @NotNull @Size(min = 2, max = 10, message = "Un recorrido tiene entre 2 y 10 paradas")
    List<Long> paradas,

    @NotNull ModoDeViaje modo
) {}
