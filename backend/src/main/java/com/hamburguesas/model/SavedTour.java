package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Un recorrido que alguien guardó.
 *
 * Se guardan las paradas y no los filtros con que salió: lo que importa después es por
 * dónde se pasó, no qué se le pidió al generador.
 */
@Entity
@Table(name = "tours")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedTour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 160)
    private String name;

    /** Lo estimado el día que se guardó, no lo que daría el cálculo de hoy. */
    @Column(nullable = false)
    private double kilometers;

    @Column(nullable = false)
    private int minutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "travel_mode", nullable = false, length = 20)
    private ModoDeViaje travelMode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    @Builder.Default
    private List<SavedTourStop> stops = new ArrayList<>();

    /** Por dónde pasa, sin orden: es lo que decide si dos recorridos son el mismo. */
    public Set<Long> idsDeLasParadas() {
        return stops.stream()
            .map(stop -> stop.getBurgerJoint().getId())
            .collect(Collectors.toSet());
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
