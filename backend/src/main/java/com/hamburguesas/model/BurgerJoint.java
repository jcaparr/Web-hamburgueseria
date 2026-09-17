package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "burger_joints")
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
}
