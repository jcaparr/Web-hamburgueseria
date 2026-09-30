package com.hamburguesas.repository;

import com.hamburguesas.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * Busca gente por un pedazo de su nombre de usuario.
     *
     * El patrón se arma afuera y llega listo, con lo que el usuario escribió ya
     * escapado: el guión bajo es parte de un nombre válido y a la vez es un comodín
     * de LIKE, así que sin escaparlo "juan_ca" encontraría también a "juanXca".
     *
     * Ordena primero a los que empiezan con lo escrito. Quien tipea "juan" busca
     * casi siempre a alguien que se llama juan-algo, no a "eljuanmas".
     *
     * En "ocultos" va todo el que no tiene que aparecer: quien busca —nadie se busca a
     * sí mismo— y cualquiera con quien haya un bloqueo de por medio. Son la misma cosa
     * desde acá, "gente que no va en esta lista", y separarlos en dos condiciones sería
     * escribir dos veces lo mismo. Nunca llega vacía, porque un "not in ()" no es SQL
     * válido; quien llama pone un id imposible cuando no hay nada que esconder.
     */
    @Query("""
        select u from User u
        where u.username like :patron escape '\\'
          and u.id not in :ocultos
        order by case when u.username like :prefijo escape '\\' then 0 else 1 end,
                 u.username
        """)
    List<User> buscarPorNombreDeUsuario(@Param("patron") String patron,
                                        @Param("prefijo") String prefijo,
                                        @Param("ocultos") Collection<Long> ocultos,
                                        Pageable pagina);
}
