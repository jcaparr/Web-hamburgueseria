package com.hamburguesas.repository;

import com.hamburguesas.model.SugerenciaDeLocal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface SugerenciaDeLocalRepository extends JpaRepository<SugerenciaDeLocal, Long> {

    /** Las de una persona desde una fecha: para su tope por día y la espera entre una y otra. */
    long countByUser_IdAndCreatedAtAfter(Long userId, Instant desde);

    /** Las de todos desde una fecha: para el tope total y el de mails. */
    long countByCreatedAtAfter(Instant desde);
}
