package com.hamburguesas.logros;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LogroDeUsuarioRepository extends JpaRepository<LogroDeUsuario, LogroDeUsuarioId> {

    /** Las cuentas que todavía no tienen ninguna fila: las que le faltan a la carga inicial. */
    @Query("select u.id from User u where not exists (select 1 from LogroDeUsuario l where l.id.userId = u.id)")
    List<Long> cuentasSinLogros();

    @Query("select l from LogroDeUsuario l where l.id.userId = :userId")
    List<LogroDeUsuario> deLaCuenta(@Param("userId") Long userId);
}
