package com.hamburguesas.repository;

import com.hamburguesas.model.User;
import com.hamburguesas.model.VerificationCode;
import com.hamburguesas.model.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findFirstByUserAndPurposeOrderByCreatedAtDesc(
        User user, VerificationPurpose purpose);

    /**
     * Issuing a new code invalidates the previous ones, so an old email left in an
     * inbox stops working the moment the user asks for another.
     */
    @Modifying
    @Query("update VerificationCode c set c.consumedAt = CURRENT_TIMESTAMP "
        + "where c.user = :user and c.purpose = :purpose and c.consumedAt is null")
    void consumeAllFor(@Param("user") User user, @Param("purpose") VerificationPurpose purpose);
}
