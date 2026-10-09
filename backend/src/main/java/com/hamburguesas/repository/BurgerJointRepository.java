package com.hamburguesas.repository;

import com.hamburguesas.model.BurgerJoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BurgerJointRepository
    extends JpaRepository<BurgerJoint, Long>, JpaSpecificationExecutor<BurgerJoint> {

    /** Los que todavía no tienen foto, para completarlas en la próxima sincronización. */
    List<BurgerJoint> findByPhotoUrlIsNull();

    /** Todos los locales, para el mapa del sitio: cada uno tiene su ficha pública. */
    @Query("select b.id from BurgerJoint b order by b.id")
    List<Long> idsDeTodos();

    /**
     * Los locales a los que todavía no les preguntamos cuántas fotos tiene Google.
     *
     * Primero los que Google no clasifica como hamburguesería, que son los sospechosos:
     * un lugar anotado como "restaurant" que además tiene tres fotos probablemente no
     * sea una hamburguesería. Preguntar cuesta una ficha por local y el tramo del mes es
     * finito, así que el orden decide qué se alcanza a saber antes de que se termine.
     *
     * Las cadenas van al final: son las que menos dudas generan.
     */
    @Query("""
        select b from BurgerJoint b
        where b.fotosEnGoogle is null
        order by
          case when b.fastFood then 2
               when b.googlePrimaryType = 'hamburger_restaurant' then 1
               else 0 end,
          b.id
        """)
    List<BurgerJoint> sinSaberCuantasFotosTiene();

    /** Los que ya tienen foto, para prestársela a otras sucursales de la misma cadena. */
    List<BurgerJoint> findByPhotoUrlIsNotNull();

    /**
     * Los que tienen una foto elegida con una regla anterior a la de ahora. Se revisan
     * una vez, quedan anotados con la regla nueva y no se vuelven a mirar.
     */
    @Query("""
        select b from BurgerJoint b
        where b.photoUrl is not null
          and (b.photoRule is null or b.photoRule < :regla)
        """)
    List<BurgerJoint> conFotoElegidaConUnaReglaVieja(@Param("regla") int regla);

    /**
     * El listado de Explorar, con los filtros que hayan puesto: el nombre, los barrios y
     * si se muestran las cadenas.
     *
     * Eran cuatro consultas derivadas, una por combinación de nombre y cadenas, porque
     * escribir los filtros como opcionales rompía contra Postgres. Con el barrio serían
     * ocho. El porqué de armar la condición en vez de escribirla está en FiltroDeLocales.
     */
    default Page<BurgerJoint> buscar(String nombre, List<String> barrios, boolean conCadenas,
                                     OrdenDeLocales orden, Pageable pagina) {
        return findAll(FiltroDeLocales.con(nombre, barrios, conCadenas, orden), pagina);
    }

    Optional<BurgerJoint> findByPlaceId(String placeId);

    /** Los locales de un puñado de fichas puntuales, para las fotos elegidas a mano. */
    List<BurgerJoint> findByPlaceIdIn(Collection<String> placeIds);

    /**
     * Los candidatos a un tour, sin las cadenas de comida rápida.
     *
     * Sin paginar porque el tour elige entre todos: son cuatrocientos y pico y el
     * recorrido se arma en memoria, comparando cada uno contra la última parada.
     */
    List<BurgerJoint> findByFastFoodFalse();

    /** Los barrios que tienen al menos una hamburguesería, para el selector del tour. */
    @Query("select distinct b.area from BurgerJoint b where b.area is not null order by b.area")
    List<String> barriosConLocales();

    /**
     * Los nombres de los locales de un rubro. Sirve para reconocer las sucursales que
     * Google clasificó distinto al resto de su cadena.
     */
    @Query("select b.name from BurgerJoint b where b.googlePrimaryType = :rubro")
    List<String> nombresDeRubro(@Param("rubro") String rubro);

    /**
     * Los locales a los que hay que pedirles el horario, en el orden en que conviene.
     *
     * Primero los que nunca se preguntaron y después los más viejos, así la cuota del
     * mes va a lo que falta antes que a refrescar. Los que se preguntaron después de
     * {@code antesDe} no salen: su horario todavía está fresco.
     */
    @Query("""
        select b from BurgerJoint b
        where b.placeId is not null
          and (b.horarioConsultadoEl is null or b.horarioConsultadoEl < :antesDe)
        order by b.horarioConsultadoEl asc nulls first, b.id
        """)
    List<BurgerJoint> paraPedirleElHorario(@Param("antesDe") Instant antesDe);

    @Modifying
    @Transactional
    @Query("update BurgerJoint b set b.horarioConsultadoEl = :cuando where b.id = :id")
    void anotarHorarioConsultado(@Param("id") Long id, @Param("cuando") Instant cuando);
}
