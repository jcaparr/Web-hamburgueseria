package com.hamburguesas.repository;

import com.hamburguesas.model.ListaDeseados;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ListaDeseadosRepository extends JpaRepository<ListaDeseados, Long> {
    List<ListaDeseados> findByUsuario_IdOrderByFechaDesc(Long usuarioId);
    Optional<ListaDeseados> findByUsuario_IdAndHamburgueseria_Id(Long usuarioId, Long hamburgueseriaId);
    boolean existsByUsuario_IdAndHamburgueseria_Id(Long usuarioId, Long hamburgueseriaId);
}
