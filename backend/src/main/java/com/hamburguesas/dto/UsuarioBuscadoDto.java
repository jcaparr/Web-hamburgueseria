package com.hamburguesas.dto;

/**
 * Una persona en la lista de resultados de la búsqueda.
 *
 * No lleva el email. Es la diferencia con {@link AuthResponse}, que habla de uno
 * mismo: acá se está mirando a otro, y de otro solo se muestra lo que eligió que se
 * vea. Cuántas reseñas tiene va porque es lo único que ayuda a decidir si vale la
 * pena seguirlo cuando hay dos nombres parecidos.
 */
public record UsuarioBuscadoDto(
    Long userId,
    String username,
    long resenias,
    boolean loSigo
) {}
