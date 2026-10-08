package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * Lo que alguien le puso a la reseña de otro (#186). Una por persona y por reseña.
 *
 * Se borra sola con la reseña o con la cuenta de quien reaccionó: la base lo hace en
 * cascada, y no queda una reacción apuntando a algo que ya no existe.
 */
@Entity
@Table(
    name = "reacciones",
    uniqueConstraints = @UniqueConstraint(name = "uk_reaccion", columnNames = {"rating_id", "user_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rating_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Rating rating;

    /** Quien reaccionó. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDeReaccion tipo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
