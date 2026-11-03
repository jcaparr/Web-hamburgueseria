package com.hamburguesas.dto;

/** Una franja con el local al que pertenece, para armar el horario de todo el mapa de una. */
public record FranjaDeLocal(Long burgerJointId, int dia, int abre, int cierra) {}
