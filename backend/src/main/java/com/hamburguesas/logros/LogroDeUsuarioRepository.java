package com.hamburguesas.logros;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LogroDeUsuarioRepository extends JpaRepository<LogroDeUsuario, LogroDeUsuarioId> {

    @Query("select l from LogroDeUsuario l where l.id.userId = :userId")
    List<LogroDeUsuario> deLaCuenta(@Param("userId") Long userId);
}
