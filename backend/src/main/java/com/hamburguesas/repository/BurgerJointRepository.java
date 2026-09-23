package com.hamburguesas.repository;

import com.hamburguesas.model.BurgerJoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BurgerJointRepository extends JpaRepository<BurgerJoint, Long> {

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
    Page<BurgerJoint> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * El listado de Explorar cuando se apagaron las cadenas de comida rápida.
     *
     * Son 66 de los 417 locales, y entre McDonald's y Burger King ocupan tres páginas
     * enteras. Quién es cadena lo decide FastFoodMarker al arrancar, así que acá
     * alcanza con mirar la columna.
     *
     * Son dos consultas y no una con el nombre opcional a propósito: con el nombre en
     * nulo, Postgres no puede deducir de qué tipo es el parámetro y lo toma como
     * binario, así que falla con "no existe la función lower(bytea)". Los tests corren
     * sobre H2, que sí lo deduce, y no lo veían.
     */
    Page<BurgerJoint> findByFastFoodFalse(Pageable pageable);

    Page<BurgerJoint> findByNameContainingIgnoreCaseAndFastFoodFalse(String name, Pageable pageable);
    Optional<BurgerJoint> findByPlaceId(String placeId);

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
