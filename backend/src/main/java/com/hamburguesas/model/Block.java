package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Que alguien bloqueó a alguien.
 *
 * Se guarda en una dirección y se aplica en las dos: después de esto ninguno de los
 * dos ve al otro. Guardarlo así —y no como dos filas— deja claro quién lo pidió, que
 * es el único que lo puede deshacer.
 */
@Entity
@Table(
    name = "blocks",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_block", columnNames = {"blocker_id", "blocked_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** El que bloqueó, y el único que puede deshacerlo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
