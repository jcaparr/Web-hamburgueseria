package com.hamburguesas.repository;

import com.hamburguesas.dto.ItemDeFeedDto;
import com.hamburguesas.dto.RankingItemDto;
import com.hamburguesas.dto.ReseniaDePerfilDto;
import com.hamburguesas.dto.ReseniasPorUsuarioDto;
import com.hamburguesas.model.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Page<Rating> findByBurgerJoint_IdOrderByCreatedAtDesc(Long burgerJointId, Pageable pageable);

    Optional<Rating> findByUser_IdAndBurgerJoint_Id(Long userId, Long burgerJointId);

    @Query("""
        select new com.hamburguesas.dto.RankingItemDto(
            b.id, b.placeId, b.name, b.address, b.area, b.photoUrl, b.latitude, b.longitude,
            avg(r.score), count(r), null)
        from Rating r join r.burgerJoint b
        where (:area is null or b.area = :area)
        group by b.id, b.placeId, b.name, b.address, b.area, b.photoUrl, b.latitude, b.longitude
        order by avg(r.score) desc
        """)
    Page<RankingItemDto> rankingByScore(@Param("area") String area, Pageable pageable);

    @Query("""
        select new com.hamburguesas.dto.RankingItemDto(
            b.id, b.placeId, b.name, b.address, b.area, b.photoUrl, b.latitude, b.longitude,
            avg(r.score), count(r), null)
        from Rating r join r.burgerJoint b
        where (:area is null or b.area = :area)
        group by b.id, b.placeId, b.name, b.address, b.area, b.photoUrl, b.latitude, b.longitude
        order by count(r) desc
        """)
    Page<RankingItemDto> rankingByPopularity(@Param("area") String area, Pageable pageable);

    @Query("""
        select new com.hamburguesas.dto.RankingItemDto(
            b.id, b.placeId, b.name, b.address, b.area, b.photoUrl, b.latitude, b.longitude,
            avg(r.score), count(r), max(case when r.user.id = :userId then r.score else null end))
        from Rating r join r.burgerJoint b
        where b.id in (
            select r2.burgerJoint.id from Rating r2 where r2.user.id = :userId
        )
        group by b.id, b.placeId, b.name, b.address, b.area, b.photoUrl, b.latitude, b.longitude
        order by max(case when r.user.id = :userId then r.score else null end) desc
        """)
    Page<RankingItemDto> personalRanking(@Param("userId") Long userId, Pageable pageable);

    /** Los locales que ya puntuó alguien, que para esa persona son los ya visitados. */
    @Query("select distinct r.burgerJoint.id from Rating r where r.user.id = :userId")
    List<Long> idsPuntuadosPor(@Param("userId") Long userId);

    /**
     * El promedio y la cantidad de todos los locales puntuados, de una.
     *
     * El tour mira la nota de cada candidato para elegir por dónde empezar, y pedirla
     * de a uno sería una consulta por local.
     */
    @Query("select r.burgerJoint.id, avg(r.score), count(r) from Rating r group by r.burgerJoint.id")
    List<Object[]> promediosPorLocal();

    @Query("select avg(r.score) from Rating r where r.burgerJoint.id = :burgerJointId")
    Double averageScoreByBurgerJoint(@Param("burgerJointId") Long burgerJointId);

    long countByBurgerJoint_Id(Long burgerJointId);

    /** Para no borrar un local que alguien puntuó, aunque la limpieza diga que sobra. */
    boolean existsByBurgerJoint_Id(Long burgerJointId);

    long countByUser_Id(Long userId);

    @Query("select avg(r.score) from Rating r where r.user.id = :userId")
    Double averageScoreByUser(@Param("userId") Long userId);

    @Query("""
        select new com.hamburguesas.dto.ReseniaDePerfilDto(
            r.id, b.id, b.name, b.photoUrl, r.score, r.comment, r.createdAt)
        from Rating r join r.burgerJoint b
        where r.user.id = :userId
        order by r.createdAt desc
        """)
    List<ReseniaDePerfilDto> findAllByUserOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Las últimas de alguien, para asomarlas en su perfil.
     *
     * Es la misma consulta cortada: en el perfil ajeno se muestran unas pocas, las
     * suficientes para darse una idea de qué le gusta antes de decidir seguirlo.
     */
    @Query("""
        select new com.hamburguesas.dto.ReseniaDePerfilDto(
            r.id, b.id, b.name, b.photoUrl, r.score, r.comment, r.createdAt)
        from Rating r join r.burgerJoint b
        where r.user.id = :userId
        order by r.createdAt desc
        """)
    List<ReseniaDePerfilDto> ultimasDe(@Param("userId") Long userId, Pageable pagina);

    /**
     * Cuántas reseñas tiene cada uno de estos, en una sola consulta.
     *
     * Quien no tiene ninguna no aparece en el resultado: un group by no inventa filas
     * para los que no tienen nada que agrupar, y quien llama pone el cero.
     */
    @Query("""
        select new com.hamburguesas.dto.ReseniasPorUsuarioDto(r.user.id, count(r))
        from Rating r
        where r.user.id in :ids
        group by r.user.id
        """)
    List<ReseniasPorUsuarioDto> contarPorUsuario(@Param("ids") Collection<Long> ids);

    /**
     * El feed, de la más nueva a la más vieja, desde el corte hacia atrás.
     *
     * El corte va por (fecha, id) y no por cantidad de filas salteadas: dos reseñas
     * pueden compartir el instante, y una nueva entrando arriba correría todo hacia
     * abajo, con lo que la página siguiente repetiría la última de la anterior.
     *
     * La primera pantalla no manda null sino un corte en el futuro, que deja pasar
     * todo. Un "(:fecha is null or ...)" sería lo natural, pero Postgres no puede
     * deducir de qué tipo es un parámetro que solo aparece comparado contra null, y la
     * consulta falla entera; además esa forma le tapa el índice al planificador.
     */
    @Query("""
        select new com.hamburguesas.dto.ItemDeFeedDto(
            r.id, u.id, u.username, b.id, b.name, b.photoUrl, b.area,
            r.score, r.comment, r.createdAt,
            case when r.updatedAt is not null then true else false end)
        from Rating r join r.user u join r.burgerJoint b
        where r.createdAt < :fecha or (r.createdAt = :fecha and r.id < :id)
        order by r.createdAt desc, r.id desc
        """)
    List<ItemDeFeedDto> feedDeTodos(@Param("fecha") Instant fecha, @Param("id") Long id,
                                    Pageable pagina);

    /** El mismo feed, restringido a un grupo de personas: la pestaña "Siguiendo". */
    @Query("""
        select new com.hamburguesas.dto.ItemDeFeedDto(
            r.id, u.id, u.username, b.id, b.name, b.photoUrl, b.area,
            r.score, r.comment, r.createdAt,
            case when r.updatedAt is not null then true else false end)
        from Rating r join r.user u join r.burgerJoint b
        where u.id in :autores
          and (r.createdAt < :fecha or (r.createdAt = :fecha and r.id < :id))
        order by r.createdAt desc, r.id desc
        """)
    List<ItemDeFeedDto> feedDe(@Param("autores") Collection<Long> autores,
                               @Param("fecha") Instant fecha, @Param("id") Long id,
                               Pageable pagina);
}
