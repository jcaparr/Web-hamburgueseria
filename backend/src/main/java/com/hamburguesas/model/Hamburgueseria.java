package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hamburgueserias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hamburgueseria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 250)
    private String direccion;

    @Column(name = "zona", length = 100)
    private String zona;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    private Double latitud;

    private Double longitud;
}
