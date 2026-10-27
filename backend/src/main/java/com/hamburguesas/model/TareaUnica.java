package com.hamburguesas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Una tarea de una sola vez que ya se hizo (#344). Si la fila está, no se repite. */
@Entity
@Table(name = "tareas_unicas")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TareaUnica {

    @Id
    @Column(length = 80)
    private String nombre;

    @Column(name = "hecha_el", nullable = false)
    private Instant hechaEl;
}
