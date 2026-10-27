package com.hamburguesas.logros;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LogroDeUsuarioRepository extends JpaRepository<LogroDeUsuario, LogroDeUsuarioId> {

    /** Las cuentas que todavía no tienen ninguna fila: las que le faltan a la carga inicial. */
    @Query("select u.id from User u where not exists (select 1 from LogroDeUsuario l where l.id.userId = u.id)")
    List<Long> cuentasSinLogros();

    /** Los ganados hoy cuyo aviso falta, sin los de la carga inicial (#317). */
    @Query("select l from LogroDeUsuario l where l.id.userId = :userId and l.activo = true and l.vistoEl is null and l.retroactivo = false order by l.ganadoEl")
    List<LogroDeUsuario> sinVer(@Param("userId") Long userId);

    @Query("select count(l) from LogroDeUsuario l where l.id.userId = :userId and l.activo = true and l.vistoEl is null and l.retroactivo = true")
    long retroactivosSinVer(@Param("userId") Long userId);

    @Query("select count(l) from LogroDeUsuario l where l.id.userId = :userId and l.activo = true and l.vistoEl is null and l.retroactivo = false")
    long contarSinVer(@Param("userId") Long userId);

    @Query("select l from LogroDeUsuario l where l.id.userId = :userId and l.activo = true and l.vistoEl is null and l.retroactivo = true")
    List<LogroDeUsuario> retroactivosSinMarcar(@Param("userId") Long userId);

    /** Los activos ganados desde esa fecha, para la campana (#320). */
    @Query("select l from LogroDeUsuario l where l.id.userId = :userId and l.activo = true and l.ganadoEl >= :desde order by l.ganadoEl desc")
    List<LogroDeUsuario> ganadosDespuesDe(@Param("userId") Long userId, @Param("desde") java.time.Instant desde);

    @Query("select count(l) from LogroDeUsuario l where l.id.userId = :userId and l.activo = true and l.retroactivo = false and l.ganadoEl > :desde")
    long ganadosDesde(@Param("userId") Long userId, @Param("desde") java.time.Instant desde);

    @Query("select l from LogroDeUsuario l where l.id.userId = :userId")
    List<LogroDeUsuario> deLaCuenta(@Param("userId") Long userId);
}
