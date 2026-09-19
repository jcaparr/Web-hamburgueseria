package com.hamburguesas.auth;

import com.hamburguesas.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Counts a guess against a code, in its own transaction.
 *
 * A wrong code makes the caller throw, which rolls back the surrounding transaction.
 * If the counter lived in that transaction it would roll back too, and the limit on
 * attempts would quietly do nothing: a 6-digit code with unlimited guesses is no
 * protection at all. The attempt happened, so it has to be recorded either way.
 *
 * Same reasoning as PlacesQuotaGuard, which records a Google call that was already
 * billed even when the sync around it fails.
 */
@Component
@RequiredArgsConstructor
public class VerificationAttemptRecorder {

    private final VerificationCodeRepository codeRepository;

    /**
     * @return the new attempt count, which the caller writes back onto its own copy of
     *         the entity. Without that, the outer transaction would later flush a stale
     *         copy and undo this increment.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int record(Long codeId) {
        return codeRepository.findById(codeId)
            .map(code -> {
                code.setAttempts(code.getAttempts() + 1);
                codeRepository.save(code);
                return code.getAttempts();
            })
            .orElse(0);
    }
}
