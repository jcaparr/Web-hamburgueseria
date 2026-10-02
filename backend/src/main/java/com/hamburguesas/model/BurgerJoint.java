package com.hamburguesas.model;

import com.hamburguesas.texto.Texto;
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

    /**
     * El nombre listo para buscar: sin acentos y en minúsculas.
     *
     * Nadie escribe los acentos en un buscador, y la mitad de los barrios de la Ciudad
     * los tienen: buscar "Atiko" no encontraba "Átiko" ni "Nunez" a los de Núñez.
     *
     * Se guarda en vez de resolverse en la consulta porque hacerlo ahí necesitaría la
     * extensión unaccent de Postgres, que no está garantizada en todos los hostings y
     * ataría el arranque de la app a haberla instalado a mano en el servidor.
     */
    @Column(name = "nombre_para_buscar", length = 255)
    private String nombreParaBuscar;

    /**
     * Mantiene el nombre de búsqueda al día.
     *
     * Acá y no en quien guarda: son cinco lugares distintos los que le cambian el nombre
     * a un local —la búsqueda, la limpieza, el arreglo de duplicados— y alcanzaría con
     * que uno se olvide para que ese local deje de encontrarse.
     */
    @PrePersist
    @PreUpdate
    void prepararElNombreParaBuscar() {
        nombreParaBuscar = sinAcentos(name);
    }

    /** Las mismas reglas que aplica el buscador a lo que se escribe, para que coincidan. */
    public static String sinAcentos(String valor) {
        return valor == null ? null : Texto.paraComparar(valor);
    }

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
     * Cómo reconocer la foto que tenemos bajada.
     *
     * El nombre que devuelve Google no sirve: cambia en cada pedido, así que comparar
     * por nombre daba siempre distinto y cada revisión volvía a bajar todas las fotos.
     * El porqué de esta huella, y qué la compone, está en FotoElegida.
     */
    @Column(name = "photo_fingerprint", length = 200)
    private String photoFingerprint;

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

    /**
     * Cuántas fotos tiene el local en Google.
     *
     * Hasta ahora se preguntaba y se tiraba: la respuesta trae el array entero —hasta
     * diez, que es el tope de Google— y nos quedábamos con las tres mejores. De un local
     * solo sabíamos si tenía alguna o ninguna, y tres no es lo mismo que diez: diez es un
     * local que la gente fotografía, tres es uno por el que nadie pasó.
     *
     * Nulo es "todavía no se preguntó", que no es cero. Cero es motivo para borrarlo;
     * nulo no es motivo de nada.
     */
    @Column(name = "fotos_en_google")
    private Integer fotosEnGoogle;

    @Column(name = "google_primary_type", length = 80)
    private String googlePrimaryType;

    private Double latitude;

    private Double longitude;

    /** Google Places identifier. Null for joints loaded by hand instead of by the sync job. */
    @Column(name = "place_id", length = 300)
    private String placeId;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    /**
     * La última vez que se le pidió el horario a Google.
     *
     * Nulo es "nunca se preguntó", que no es lo mismo que "no tiene horario": esos
     * quedan con la fecha puesta y ninguna franja. Las franjas viven en su propia tabla,
     * {@link FranjaHoraria}, y no acá: Explorar lista locales de a veinte y no necesita
     * el horario de ninguno.
     */
    @Column(name = "horario_consultado_el")
    private Instant horarioConsultadoEl;
}
