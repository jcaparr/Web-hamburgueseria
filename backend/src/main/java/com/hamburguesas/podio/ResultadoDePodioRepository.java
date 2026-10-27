package com.hamburguesas.podio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ResultadoDePodioRepository extends JpaRepository<ResultadoDePodio, ResultadoDePodio.Id> {

    @Query("select r from ResultadoDePodio r where r.id.periodo = :periodo and r.id.desde = :desde order by r.id.puesto")
    List<ResultadoDePodio> de(@Param("periodo") String periodo, @Param("desde") LocalDate desde);
}
