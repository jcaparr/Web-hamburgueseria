package com.hamburguesas.repository;

import com.hamburguesas.model.RefreshToken;
import com.hamburguesas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * The reaction to a token that was already used: whoever holds a revoked token
     * should not have it, and there is no way to tell which of the two parties is the
     * real user, so both are logged out.
     */
    // La marca de tiempo viaja como parámetro y no como CURRENT_TIMESTAMP: en HQL ese
    // valor se tipa según el dialecto, y contra H2 no se deja asignar a un Instant.
    // Una consulta que compila en un motor y no en otro es una trampa para después.
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now "
        + "where t.user = :user and t.revokedAt is null")
    int revokeAllFor(@Param("user") User user, @Param("now") Instant now);

    /** Rows that can no longer authorise anything are only taking up space. */
    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
