package com.hamburguesas.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * La página que pidió el cliente, sin el orden que haya mandado (#201).
 *
 * Cada listado tiene su propio orden escrito en la consulta, y la web nunca manda uno.
 * Dejar pasar el "sort" del pedido solo servía para que alguien ordenara por campos de
 * otras tablas —"sort=user.email"— o mandara uno inválido, que Spring rechaza con un
 * 500 y una excepción entera en el log. Ignorarlo es más simple que validarlo: no hay
 * ningún orden del cliente que haga falta aceptar.
 *
 * El tamaño ya viene recortado a lo que dice spring.data.web.pageable.max-page-size.
 */
final class Paginas {

    private Paginas() {}

    static Pageable sinOrden(Pageable pedida) {
        return PageRequest.of(pedida.getPageNumber(), pedida.getPageSize());
    }
}
