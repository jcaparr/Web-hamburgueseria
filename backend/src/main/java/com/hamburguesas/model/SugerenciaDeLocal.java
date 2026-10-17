package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Un local que alguien avisó que falta (#257). Se revisa a mano en la base. */
@Entity
@Table(name = "sugerencias_de_locales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SugerenciaDeLocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 500)
    private String direccion;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 1000)
    private String comentario;

    /** El nombre del archivo de la foto opcional, en la carpeta que la web no sirve (#283). */
    @Column(name = "foto_archivo", length = 100)
    private String fotoArchivo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
