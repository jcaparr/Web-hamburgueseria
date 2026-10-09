package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Cómo la encuentran y la nombran los demás. Siempre en minúsculas: adentro y
     * afuera "Juan" y "juan" tienen que ser la misma persona.
     *
     * Es el único nombre que tiene una cuenta. No hay un "nombre para mostrar" aparte
     * porque al lado de un @juanca no agregaría nada y sí confundiría: dos personas
     * con el mismo nombre se verían igual sin serlo.
     */
    @Column(nullable = false, length = 20, unique = true)
    private String username;

    @Column(nullable = false, length = 180)
    private String email;

    /** Null for accounts that only ever sign in with Google. */
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * Google's stable id for the account. The link is anchored here and not on the
     * email, which a person can change on their Google account.
     */
    @Column(name = "google_sub", length = 64)
    private String googleSub;

    /** False until the user enters the code we emailed them. They cannot log in before that. */
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * La hamburguesa que eligió para su avatar, en cinco cifras (ver
     * {@link com.hamburguesas.dto.HamburguesaRequest}). Null mientras no elija ninguna:
     * el navegador dibuja la que sale de su nombre.
     */
    @Column(length = 5)
    private String hamburguesa;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Cuándo abrió el buzón de notificaciones por última vez (#210): lo que llegó después
     * es lo nuevo, y es lo que cuenta la campana. Nulo mientras no lo abra nunca, y
     * entonces todo cuenta como nuevo.
     */
    @Column(name = "notificaciones_vistas_el")
    private Instant notificacionesVistasEl;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
