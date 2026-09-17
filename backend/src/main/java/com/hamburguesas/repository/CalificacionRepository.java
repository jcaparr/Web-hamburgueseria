package com.hamburguesas.repository;

import com.hamburguesas.dto.RankingItemDto;
import com.hamburguesas.model.Calificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {

    Page<Calificacion> findByHamburgueseria_IdOrderByFechaDesc(Long hamburgueseriaId, Pageable pageable);

    Optional<Calificacion> findByUsuario_IdAndHamburgueseria_Id(Long usuarioId, Long hamburgueseriaId);

    @Query("""
        select new com.hamburguesas.dto.RankingItemDto(
            h.id, h.nombre, h.direccion, h.zona, h.fotoUrl,
            avg(c.puntaje), count(c), null)
        from Calificacion c join c.hamburgueseria h
        where (:zona is null or h.zona = :zona)
        group by h.id, h.nombre, h.direccion, h.zona, h.fotoUrl
        order by avg(c.puntaje) desc
        """)
    Page<RankingItemDto> rankingPorPuntaje(@Param("zona") String zona, Pageable pageable);

    @Query("""
        select new com.hamburguesas.dto.RankingItemDto(
            h.id, h.nombre, h.direccion, h.zona, h.fotoUrl,
            avg(c.puntaje), count(c), null)
        from Calificacion c join c.hamburgueseria h
        where (:zona is null or h.zona = :zona)
        group by h.id, h.nombre, h.direccion, h.zona, h.fotoUrl
        order by count(c) desc
        """)
    Page<RankingItemDto> rankingPorPopularidad(@Param("zona") String zona, Pageable pageable);

    @Query("""
        select new com.hamburguesas.dto.RankingItemDto(
            h.id, h.nombre, h.direccion, h.zona, h.fotoUrl,
            avg(c.puntaje), count(c), max(case when c.usuario.id = :usuarioId then c.puntaje else null end))
        from Calificacion c join c.hamburgueseria h
        where h.id in (
            select c2.hamburgueseria.id from Calificacion c2 where c2.usuario.id = :usuarioId
        )
        group by h.id, h.nombre, h.direccion, h.zona, h.fotoUrl
        """)
    Page<RankingItemDto> rankingPersonal(@Param("usuarioId") Long usuarioId, Pageable pageable);

    @Query("select avg(c.puntaje) from Calificacion c where c.hamburgueseria.id = :hamburgueseriaId")
    Double promedioPorHamburgueseria(@Param("hamburgueseriaId") Long hamburgueseriaId);

    long countByHamburgueseria_Id(Long hamburgueseriaId);
}
