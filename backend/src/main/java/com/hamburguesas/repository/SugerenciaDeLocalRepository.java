package com.hamburguesas.repository;

import com.hamburguesas.model.SugerenciaDeLocal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface SugerenciaDeLocalRepository extends JpaRepository<SugerenciaDeLocal, Long> {

    long countByUser_IdAndCreatedAtAfter(Long userId, Instant desde);
}
