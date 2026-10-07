package com.hamburguesas.auth;

import com.hamburguesas.mail.EmailService;
import com.hamburguesas.model.User;
import com.hamburguesas.model.VerificationPurpose;
import com.hamburguesas.repository.VerificationCodeRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lo que dice el mail con el código, que llegaba a no deseado en Hotmail (#172). */
class MailDelCodigoTest {

    @ParameterizedTest
    @EnumSource(VerificationPurpose.class)
    void llevaElCodigoYNoNombraAlSpam(VerificationPurpose motivo) {
        VerificationCodeRepository codigos = mock(VerificationCodeRepository.class);
        when(codigos.findFirstByUserAndPurposeOrderByCreatedAtDesc(any(), any())).thenReturn(Optional.empty());
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("hash");
        EmailService mails = mock(EmailService.class);
        VerificationService servicio = new VerificationService(
            codigos, mock(VerificationAttemptRecorder.class), encoder, mails, new AuthProperties());
        User juan = User.builder().id(1L).username("juanca").email("juanca@example.com").build();

        servicio.issue(juan, motivo);

        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(encoder).encode(codigo.capture());
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(mails).send(eq("juanca@example.com"), anyString(), cuerpo.capture());

        assertThat(cuerpo.getValue())
            .contains("@juanca", codigo.getValue(), "Burgómetro")
            .doesNotContainIgnoringCase("spam")
            .doesNotContainIgnoringCase("no deseado");
    }
}
