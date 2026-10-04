package com.hamburguesas.dto;

/** La nota promedio de un local y cuántas reseñas tiene, de la consulta agrupada de una página. */
public record NotaDeLocalDto(Long burgerJointId, Double promedio, Long cuantas) {}
