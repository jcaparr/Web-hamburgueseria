package com.hamburguesas.dto;

/**
 * Si el nombre se puede usar y, si no, uno parecido que sí.
 *
 * La sugerencia viene vacía cuando está libre, y también cuando no se encontró ninguna
 * cerca: en los dos casos no hay nada que ofrecer, y el campo se arregla solo.
 */
public record UsernameAvailabilityResponse(
    boolean available,
    String suggestion
) {}
