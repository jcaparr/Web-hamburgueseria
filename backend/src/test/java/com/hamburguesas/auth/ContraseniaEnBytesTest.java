package com.hamburguesas.auth;

import com.hamburguesas.dto.RegisterRequest;
import com.hamburguesas.dto.ResetPasswordRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Una contraseña de más de 72 bytes daba un 500, porque bcrypt no la acepta (#163).
 * Ahora se rechaza al validar, con un mensaje que dice qué pasó.
 */
class ContraseniaEnBytesTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    /** El caso del issue: 40 letras, pero 80 bytes. */
    @Test
    void cuarentaEnesNoEntranAunqueSeanPocasLetras() {
        String contrasenia = "ñ".repeat(40);

        assertThat(errores(registro(contrasenia))).containsExactly("La contraseña es demasiado larga");
        assertThat(errores(cambio(contrasenia))).containsExactly("La contraseña es demasiado larga");
    }

    /** El borde es el de bcrypt, no uno más estricto: lo que pasa la validación se puede guardar. */
    @Test
    void justoSetentaYDosBytesEntraYBcryptLaAcepta() {
        String conEnes = "ñ".repeat(36);
        String sinTildes = "a".repeat(72);

        assertThat(errores(registro(conEnes))).isEmpty();
        assertThat(errores(registro(sinTildes))).isEmpty();
        assertThatCode(() -> new BCryptPasswordEncoder().encode(conEnes)).doesNotThrowAnyException();
    }

    @Test
    void unByteMasYaNo() {
        assertThat(errores(registro("a".repeat(73)))).containsExactly("La contraseña es demasiado larga");
        assertThat(errores(registro("ñ".repeat(36) + "a"))).containsExactly("La contraseña es demasiado larga");
    }

    @Test
    void laCortaSigueRechazadaConSuPropioMensaje() {
        assertThat(errores(registro("corta1"))).containsExactly("La contraseña tiene que tener al menos 8 caracteres");
    }

    private static RegisterRequest registro(String contrasenia) {
        return new RegisterRequest("juanca", "juanca@example.com", contrasenia);
    }

    private static ResetPasswordRequest cambio(String contrasenia) {
        return new ResetPasswordRequest("juanca@example.com", "123456", contrasenia);
    }

    private static List<String> errores(Object pedido) {
        return VALIDADOR.validate(pedido).stream().map(ConstraintViolation::getMessage).toList();
    }
}
