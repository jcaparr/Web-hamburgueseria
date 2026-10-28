package com.hamburguesas.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Una reseña como se lee en el feed.
 *
 * Trae junto lo de las tres partes —quién, dónde y qué dijo— porque una tarjeta del
 * feed las muestra todas, y pedirlas por separado sería una consulta por tarjeta.
 *
 * El orden y el cursor van por cuándo se escribió, no por la última edición ni por el día
 * de la visita: si se editó, se dice al lado, y la reseña sigue siendo de ese día. Lo que se
 * muestra es el día de la visita cuando la persona lo eligió (#368); si no, cuándo se escribió.
 */
public record ItemDeFeedDto(
    Long ratingId,
    Long autorId,
    String autorUsername,
    /** La hamburguesa de su avatar, o null si no eligió ninguna. */
    String autorHamburguesa,
    /** La XP del autor, para dibujar su nivel (#310). */
    int autorXp,
    Long burgerJointId,
    /** La dirección linda del local (#305), o null si aún no tiene. */
    String slug,
    String burgerJointName,
    String photoUrl,
    String area,
    /**
     * El promedio de la hamburguesería, que es otra cosa que la nota de esta reseña.
     *
     * Van las dos en la tarjeta a propósito: una dice qué le pareció a esta persona y
     * la otra qué le parece a todo el mundo, y verlas juntas es lo que deja saber si
     * estás leyendo una opinión que se sale de la norma.
     */
    Double promedioDelLocal,
    Integer score,
    String comment,
    /** Las fotos que sacó quien la escribió, en orden: la primera es la portada. */
    List<String> fotosDeLaResenia,
    Instant createdAt,
    /** El día que dijo haber ido, o null si no lo eligió (las de antes): ahí vale createdAt. */
    LocalDate visitDate,
    boolean editada,
    /** Las reacciones que le pusieron los demás, y la de quien mira (#186). */
    ReaccionesDto reacciones
) {

    /**
     * El que usa la consulta del feed, que trae solo la portada.
     *
     * Una consulta que arma objetos no puede traer una lista por fila. Las demás fotos
     * y las reacciones las agregan después {@link #conFotos} y {@link #conReacciones},
     * con una consulta para toda la página.
     */
    public ItemDeFeedDto(Long ratingId, Long autorId, String autorUsername, String autorHamburguesa,
                         int autorXp, Long burgerJointId, String slug, String burgerJointName, String photoUrl,
                         String area,
                         Double promedioDelLocal, Integer score, String comment, String portada,
                         Instant createdAt, LocalDate visitDate, boolean editada) {
        this(ratingId, autorId, autorUsername, autorHamburguesa, autorXp, burgerJointId, slug,
            burgerJointName, photoUrl, area, promedioDelLocal, score, comment,
            portada == null ? List.of() : List.of(portada), createdAt, visitDate, editada,
            ReaccionesDto.NINGUNA);
    }

    public ItemDeFeedDto conFotos(List<String> fotos) {
        return new ItemDeFeedDto(ratingId, autorId, autorUsername, autorHamburguesa, autorXp, burgerJointId,
            slug, burgerJointName, photoUrl, area, promedioDelLocal, score, comment, fotos, createdAt,
            visitDate, editada, reacciones);
    }

    public ItemDeFeedDto conReacciones(ReaccionesDto nuevas) {
        return new ItemDeFeedDto(ratingId, autorId, autorUsername, autorHamburguesa, autorXp, burgerJointId,
            slug, burgerJointName, photoUrl, area, promedioDelLocal, score, comment, fotosDeLaResenia,
            createdAt, visitDate, editada, nuevas);
    }
}
