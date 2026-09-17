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

    private Double latitude;

    private Double longitude;

    /** Google Places identifier. Null for joints loaded by hand instead of by the sync job. */
    @Column(name = "place_id", length = 300)
    private String placeId;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
}
