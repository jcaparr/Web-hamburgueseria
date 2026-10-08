package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * La portada: la primera de {@link #fotos}, o null si no tiene ninguna.
     *
     * Es una copia, y la mantiene {@link #ponerFotos}. Queda porque si un deploy sale
     * mal y se vuelve al código anterior, ese código busca la foto acá.
     *
     * Guarda la ruta pública y no los bytes: la base no es lugar para archivos, y
     * servirlos desde disco deja ponerles caché.
     */
    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    /**
     * Las fotos que sacó, en el orden en que se muestran (#185).
     *
     * En lote de a veinte, que es lo que trae una página de reseñas: pedirlas de a una
     * serían veinte consultas para dibujar una lista.
     */
    @ElementCollection
    @CollectionTable(name = "rating_photos", joinColumns = @JoinColumn(name = "rating_id"))
    @OrderColumn(name = "orden")
    @Column(name = "url", length = 500, nullable = false)
    @BatchSize(size = 20)
    @Builder.Default
    private List<String> fotos = new ArrayList<>();

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

    /** Cambia las fotos y la portada juntas, para que nunca digan cosas distintas. */
    public void ponerFotos(List<String> nuevas) {
        fotos.clear();
        fotos.addAll(nuevas);
        photoUrl = nuevas.isEmpty() ? null : nuevas.get(0);
    }

    /**
     * Todas sus fotos, también si es de antes de que hubiera varias.
     *
     * La migración ya pasó la de cada reseña a la lista. Esto cubre una reseña que se
     * armó en memoria con la portada sola, como las de los tests.
     */
    public List<String> todasLasFotos() {
        if (!fotos.isEmpty()) {
            return List.copyOf(fotos);
        }
        return photoUrl == null ? List.of() : List.of(photoUrl);
    }

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
