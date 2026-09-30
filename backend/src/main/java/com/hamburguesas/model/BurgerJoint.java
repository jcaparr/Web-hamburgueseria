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
    /**
     * Con qué versión de la regla de elección se eligió la foto que tenemos. Cuando la
     * regla mejora, este número dice cuáles hay que volver a mirar.
     */
    @Column(name = "photo_rule")
    private Integer photoRule;

    /**
     * Si es sucursal de una cadena de comida rápida. Lo completa FastFoodMarker al
     * arrancar, a partir de la lista de marcas de la configuración: el rubro que
     * declara Google no alcanza, porque clasifica desparejo dentro de una cadena.
     */
    @Column(name = "fast_food", nullable = false)
    private boolean fastFood;

    /**
     * Si Google contestó que no tiene ninguna foto de este local.
     *
     * Es distinto de no tener foto guardada: de los locales sin foto en la base, la
     * mitad sí las tiene en Google y lo que faltó fue ir a buscarlas. Un local del que
     * no hay una sola foto en ningún lado es otra cosa, y esa diferencia decide si se
     * lo esconde o se lo completa.
     */
    @Column(name = "sin_fotos_en_google", nullable = false)
    private boolean sinFotosEnGoogle;

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
