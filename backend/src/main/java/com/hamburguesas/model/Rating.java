package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
    name = "ratings",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "burger_joint_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "burger_joint_id", nullable = false)
    private BurgerJoint burgerJoint;

    @Column(nullable = false)
    private Integer score;

    @Column(length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Cuándo se editó, o null si nunca.
     *
     * No reemplaza a createdAt ni reordena nada: el feed sigue mostrando el día que se
     * escribió, y esto solo alcanza para poner un "editado" al lado. Que cambiar una
     * nota de 3 a 5 mande la reseña de nuevo arriba de todo sería tratar una corrección
     * como si fuera novedad.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
