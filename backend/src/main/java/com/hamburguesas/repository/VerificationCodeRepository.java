package com.hamburguesas.repository;

import com.hamburguesas.model.User;
import com.hamburguesas.model.VerificationCode;
import com.hamburguesas.model.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findFirstByUserAndPurposeOrderByCreatedAtDesc(
        User user, VerificationPurpose purpose);

    /**
     * Issuing a new code invalidates the previous ones, so an old email left in an
     * inbox stops working the moment the user asks for another.
     */
    // Misma razón que en RefreshTokenRepository: CURRENT_TIMESTAMP se tipa según el
    // dialecto y no se deja asignar a un Instant en todos.
    @Modifying
    @Query("update VerificationCode c set c.consumedAt = :now "
        + "where c.user = :user and c.purpose = :purpose and c.consumedAt is null")
    void consumeAllFor(@Param("user") User user, @Param("purpose") VerificationPurpose purpose,
                       @Param("now") Instant now);
}
