package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
    name = "burger_joints",
    uniqueConstraints = @UniqueConstraint(columnNames = "place_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BurgerJoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 250)
    private String address;

    @Column(name = "area", length = 100)
    private String area;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    /**
     * Cuál de las fotos de Google es la que tenemos bajada. Permite saber si sigue
     * siendo la mejor sin volver a bajarla: se compara el nombre y listo.
     */
    @Column(name = "photo_name", length = 500)
    private String photoName;

    /**
     * El rubro principal del local según Google: "hamburger_restaurant", "bar",
     * "butcher_shop". Se guarda para poder revisar qué entró y por qué sin volver a
     * preguntarle a Google.
     */
    @Column(name = "google_primary_type", length = 80)
    private String googlePrimaryType;

    private Double latitude;

    private Double longitude;

    /** Google Places identifier. Null for joints loaded by hand instead of by the sync job. */
    @Column(name = "place_id", length = 300)
    private String placeId;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
}
