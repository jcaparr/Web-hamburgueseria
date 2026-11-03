package com.hamburguesas.dto;

/**
 * Un tema del que hablan las reseñas de un local.
 *
 * Van las dos cantidades y no un porcentaje: "4 de 5" dice además cuánta gente lo
 * nombró, y con estos números —que suelen ser chicos— eso es la mitad de la
 * información. Un 80% construido sobre cinco reseñas y otro sobre doscientas se leen
 * igual, y no valen lo mismo.
 */
public record TemaDeReseniasDto(
    String tema,
    int menciones,
    int aFavor,
    /**
     * A_FAVOR, EN_CONTRA o DIVIDIDO: en qué grupo va (#392).
     *
     * Viene calculado para que el corte viva en un solo lugar, probado, y no en cada
     * pantalla que lo muestre.
     */
    String tono
) {}
