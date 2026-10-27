package com.hamburguesas.logros;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

/** La clave de un logro de una cuenta: de quién es y cuál (#314). */
@Embeddable
public record LogroDeUsuarioId(
    @Column(name = "user_id") Long userId,
    @Column(name = "logro", length = 40) String logro
) implements Serializable {
}
