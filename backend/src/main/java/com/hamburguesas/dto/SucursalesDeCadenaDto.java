package com.hamburguesas.dto;

import java.util.List;

/**
 * La página de una cadena: todas sus sucursales, cada una con su ficha de siempre (#206).
 *
 * @param sucursales ordenadas por barrio y después por nombre. Cuáles van primero según
 *                   lo que se esté mirando lo decide la pantalla, que sabe qué barrios
 *                   hay elegidos.
 */
public record SucursalesDeCadenaDto(String marca, String nombre, List<BurgerJointDto> sucursales) {}
