package com.hamburguesas.places;

/**
 * Lo que dice Google de un local, para revisarlo a mano antes de agregarlo (#225).
 *
 * Un kiosco que vende hamburguesas sueltas, una fábrica de medallones o un local que ya
 * cerró se ven en estos datos: pocas fotos, pocas opiniones, un resumen que habla de
 * otra cosa.
 *
 * @param fotos     cuántas tiene, hasta diez, que es lo más que devuelve Google
 * @param opiniones cuántas reseñas tiene
 * @param puntaje   el promedio de esas reseñas, de 1 a 5
 * @param resumen   el resumen que arma Google, o null si no tiene: solo seis de cada
 *                  diez locales tienen uno
 */
public record RevisionDeGoogle(int fotos, Integer opiniones, Double puntaje, String resumen) {}
