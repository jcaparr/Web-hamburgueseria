package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Una parada de un recorrido guardado, con el lugar que ocupa en él. */
@Entity
@Table(name = "tour_stops")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedTourStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private SavedTour tour;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "burger_joint_id", nullable = false)
    private BurgerJoint burgerJoint;

    /** Desde 1, como se numera en la pantalla. */
    @Column(nullable = false)
    private int position;

    /** Lo que había que recorrer desde la parada anterior, como se calculó ese día. */
    @Column(nullable = false)
    private double kilometers;
}
