package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Un tramo en que el local está abierto, tal como lo informa Google.
 *
 * El cierre se cuenta desde la misma medianoche que la apertura, así que puede pasar de
 * 1440: un viernes de 19 a 1 de la mañana es abre 1140 y cierra 1500. Así se lee como lo
 * dice el local —"el viernes abre de 19 a 1"— en vez de partirse en un viernes hasta
 * medianoche y un sábado de 0 a 1.
 */
@Entity
@Table(name = "franjas_horarias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FranjaHoraria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Si se borra el local, se van sus franjas: la limpieza borra locales todo el tiempo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "burger_joint_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BurgerJoint burgerJoint;

    /** 0 es domingo y 6 es sábado, como los numera Google. */
    @Column(nullable = false)
    private int dia;

    /** Minutos desde la medianoche del día. */
    @Column(nullable = false)
    private int abre;

    /** Minutos desde la misma medianoche que {@link #abre}: si pasa de 1440, cierra al día siguiente. */
    @Column(nullable = false)
    private int cierra;
}
