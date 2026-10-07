package com.hamburguesas.auth;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Que una contraseña entre en bcrypt, que no acepta más de 72 bytes: con una más larga
 * el encoder tira una excepción y el pedido terminaba en un 500 sin explicación (#163).
 *
 * Se cuentan bytes y no letras, que es lo que cuenta bcrypt. En UTF-8 una ñ o una letra
 * con tilde ocupa dos y un emoji cuatro, así que 40 ñ ya no entran aunque sean menos de
 * 72 letras. Un {@code null} pasa: de eso se ocupa {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = EntraEnBcrypt.Validador.class)
@Target({FIELD, METHOD, PARAMETER})
@Retention(RUNTIME)
public @interface EntraEnBcrypt {

    int MAXIMO_DE_BYTES = 72;

    String message() default "La contraseña es demasiado larga";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<EntraEnBcrypt, String> {
        @Override
        public boolean isValid(String contrasenia, ConstraintValidatorContext context) {
            return contrasenia == null
                || contrasenia.getBytes(StandardCharsets.UTF_8).length <= MAXIMO_DE_BYTES;
        }
    }
}
