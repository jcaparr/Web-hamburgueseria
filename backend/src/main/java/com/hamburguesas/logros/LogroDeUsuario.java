package com.hamburguesas.logros;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Un logro de una cuenta (#314).
 *
 * El código es estable y los nombres viven en el frontend. {@code ganadoEl} se escribe
 * una vez y no cambia; lo que cambia es {@code activo}, que se apaga si deja de cumplirse
 * lo que lo dio (solo en los propios) y se vuelve a prender si se cumple de nuevo.
 *
 * {@code vistoEl} es nulo mientras falte mostrar el aviso, y no se vuelve a tocar: perder
 * un logro y volver a ganarlo no repite el aviso.
 */
@Entity
@Table(name = "logros_de_usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogroDeUsuario {

    @EmbeddedId
    private LogroDeUsuarioId id;

    @Column(name = "ganado_el", nullable = false, updatable = false)
    private Instant ganadoEl;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "cambio_el", nullable = false)
    private Instant cambioEl;

    @Column(name = "visto_el")
    private Instant vistoEl;

    /** Lo recibió en la carga inicial, por lo que ya merecía: se avisa de a uno solo, sin animación. */
    @Column(nullable = false)
    private boolean retroactivo;

    /** Un dato del logro, como cuál es el barrio del Intendente. */
    @Column(length = 100)
    private String detalle;

    public Logro logro() {
        return Logro.valueOf(id.logro());
    }
}
