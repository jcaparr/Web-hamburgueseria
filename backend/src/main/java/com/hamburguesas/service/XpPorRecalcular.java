package com.hamburguesas.service;

/**
 * Avisa que cambiaron las reseñas de esta persona y hay que recalcularle la XP (#310).
 *
 * Se publica adentro de la transacción de la reseña y se atiende cuando esa transacción
 * ya confirmó, así que un recálculo que falle no se lleva puesta la reseña.
 */
public record XpPorRecalcular(Long userId) {
}
