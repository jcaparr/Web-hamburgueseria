package com.hamburguesas.podio;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ResultadoDePodioRepository extends JpaRepository<ResultadoDePodio, ResultadoDePodio.Id> {

    @Query("select r from ResultadoDePodio r where r.id.periodo = :periodo and r.id.desde = :desde order by r.id.puesto")
    List<ResultadoDePodio> de(@Param("periodo") String periodo, @Param("desde") LocalDate desde);

    /** Los inicios de los últimos períodos cerrados con alguien en el top, del más nuevo al más viejo. */
    @Query("select distinct r.id.desde from ResultadoDePodio r where r.id.periodo = :periodo order by r.id.desde desc")
    List<LocalDate> ultimosCierres(@Param("periodo") String periodo, Pageable pagina);

    @Query("select r from ResultadoDePodio r where r.id.periodo = :periodo and r.id.desde in :desdes "
        + "order by r.id.desde desc, r.id.puesto")
    List<ResultadoDePodio> deLosCierres(@Param("periodo") String periodo, @Param("desdes") List<LocalDate> desdes);
}
