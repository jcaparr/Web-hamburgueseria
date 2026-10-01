package com.hamburguesas.repository;

import com.hamburguesas.model.BurgerJoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BurgerJointRepository
    extends JpaRepository<BurgerJoint, Long>, JpaSpecificationExecutor<BurgerJoint> {

    /** Los que todavía no tienen foto, para completarlas en la próxima sincronización. */
    List<BurgerJoint> findByPhotoUrlIsNull();

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
     * El listado de Explorar, con los filtros que hayan puesto: el nombre, el barrio y
     * si se muestran las cadenas.
     *
     * Eran cuatro consultas derivadas, una por combinación de nombre y cadenas, porque
     * escribir los filtros como opcionales rompía contra Postgres. Con el barrio serían
     * ocho. El porqué de armar la condición en vez de escribirla está en FiltroDeLocales.
     */
    default Page<BurgerJoint> buscar(String nombre, String barrio, boolean conCadenas,
                                     Pageable pagina) {
        return findAll(FiltroDeLocales.con(nombre, barrio, conCadenas), pagina);
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
}
