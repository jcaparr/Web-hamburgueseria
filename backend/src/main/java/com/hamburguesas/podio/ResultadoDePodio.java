package com.hamburguesas.podio;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/** Quién salió en qué puesto al cerrar una semana o un mes (#318). No se recalcula nunca. */
@Entity
@Table(name = "resultados_de_podio")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoDePodio {

    @jakarta.persistence.Embeddable
    public record Id(
        @Column(name = "periodo", length = 10) String periodo,
        @Column(name = "desde") LocalDate desde,
        @Column(name = "puesto") int puesto
    ) implements Serializable {
    }

    @EmbeddedId
    private Id id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int xp;
}
