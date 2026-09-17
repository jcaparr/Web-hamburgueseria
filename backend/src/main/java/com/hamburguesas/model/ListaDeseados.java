package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
    name = "lista_deseados",
    uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "hamburgueseria_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListaDeseados {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hamburgueseria_id", nullable = false)
    private Hamburgueseria hamburgueseria;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    @PrePersist
    void prePersist() {
        if (fecha == null) {
            fecha = Instant.now();
        }
    }
}
