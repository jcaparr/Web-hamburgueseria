package com.hamburguesas.repository;

import com.hamburguesas.model.Hamburgueseria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HamburgueseriaRepository extends JpaRepository<Hamburgueseria, Long> {
    Page<Hamburgueseria> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
