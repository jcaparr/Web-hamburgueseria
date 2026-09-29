package com.hamburguesas.service;

import com.hamburguesas.auth.Usernames;
import com.hamburguesas.dto.PerfilPublicoDto;
import com.hamburguesas.dto.ReseniasPorUsuarioDto;
import com.hamburguesas.dto.UsuarioBuscadoDto;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.Follow;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Buscar gente, mirar su perfil y seguirla.
 *
 * Todo lo que sale de acá pasa por un DTO armado a mano. Es la regla que sostiene lo
 * demás: de una persona que no sos vos se muestra su nombre, sus reseñas y sus
 * números, y nada que no esté en esa lista.
 */
@Service
@RequiredArgsConstructor
public class SocialService {

    /** Cuántos resultados devuelve una búsqueda. */
    private static final int RESULTADOS = 20;

    /** Cuántas reseñas se asoman en el perfil de otro. */
    private static final int RESENIAS_EN_EL_PERFIL = 5;

    /** Con menos de esto no se busca: dos letras devuelven medio padrón. */
    private static final int MINIMO_PARA_BUSCAR = 2;

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final RatingRepository ratingRepository;

    /**
     * Gente cuyo nombre de usuario contiene lo escrito.
     *
     * @param quienBusca puede ser nulo: sin sesión se busca igual, pero nadie figura
     *   como seguido y uno mismo no queda excluido porque no hay "uno mismo".
     */
    public List<UsuarioBuscadoDto> buscar(String texto, Long quienBusca) {
        String limpio = soloLoQuePuedeHaberEnUnNombre(texto);
        if (limpio.length() < MINIMO_PARA_BUSCAR) {
            return List.of();
        }

        String buscado = escapandoLosComodines(limpio);
        List<User> encontrados = userRepository.buscarPorNombreDeUsuario(
            "%" + buscado + "%", buscado + "%", quienBusca, PageRequest.of(0, RESULTADOS));

        Set<Long> sigue = aCualesSigue(quienBusca, encontrados.stream().map(User::getId).toList());
        Map<Long, Long> resenias = reseniasDe(encontrados);

        return encontrados.stream()
            .map(u -> new UsuarioBuscadoDto(u.getId(), u.getUsername(),
                resenias.getOrDefault(u.getId(), 0L), sigue.contains(u.getId())))
            .toList();
    }

    public PerfilPublicoDto perfil(String username, Long quienMira) {
        User persona = porNombre(username);
        boolean soyYo = persona.getId().equals(quienMira);

        return new PerfilPublicoDto(
            persona.getId(),
            persona.getUsername(),
            ratingRepository.countByUser_Id(persona.getId()),
            ratingRepository.averageScoreByUser(persona.getId()),
            followRepository.countByFollowed_Id(persona.getId()),
            followRepository.countByFollower_Id(persona.getId()),
            !soyYo && quienMira != null
                && followRepository.existsByFollower_IdAndFollowed_Id(quienMira, persona.getId()),
            soyYo,
            ratingRepository.ultimasDe(persona.getId(), PageRequest.of(0, RESENIAS_EN_EL_PERFIL)));
    }

    /** Seguir de nuevo a quien ya seguís no es un error: ya estabas donde querías estar. */
    @Transactional
    public void seguir(String username, Long quienSigue) {
        User seguido = porNombre(username);

        if (seguido.getId().equals(quienSigue)) {
            throw new ConflictException("No podés seguirte a vos mismo");
        }
        if (followRepository.existsByFollower_IdAndFollowed_Id(quienSigue, seguido.getId())) {
            return;
        }

        followRepository.save(Follow.builder()
            .follower(userRepository.getReferenceById(quienSigue))
            .followed(seguido)
            .build());
    }

    /** Dejar de seguir a quien no seguías tampoco es un error, por lo mismo. */
    @Transactional
    public void dejarDeSeguir(String username, Long quienSigue) {
        followRepository.deleteByFollower_IdAndFollowed_Id(quienSigue, porNombre(username).getId());
    }

    private User porNombre(String username) {
        return userRepository.findByUsername(Usernames.normalizar(username))
            .orElseThrow(() -> new ResourceNotFoundException("No encontramos a esa persona"));
    }

    /**
     * Lo escrito, dejando solo lo que puede haber en un nombre de usuario.
     *
     * Con esto ni un porcentaje ni una comilla llegan a la consulta: no porque hagan
     * falta para que sea segura —la consulta va parametrizada— sino porque buscar por
     * algo que ningún nombre puede contener no puede devolver nada.
     */
    private String soloLoQuePuedeHaberEnUnNombre(String texto) {
        return Usernames.normalizar(texto).replaceAll("[^a-z0-9_]", "");
    }

    /**
     * El guión bajo es parte de un nombre válido y a la vez vale por cualquier carácter
     * en un LIKE. Sin escaparlo, buscar "juan_ca" encontraría también a "juanXca".
     */
    private String escapandoLosComodines(String limpio) {
        return limpio.replace("_", "\\_");
    }

    private Set<Long> aCualesSigue(Long quienBusca, List<Long> candidatos) {
        if (quienBusca == null || candidatos.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(followRepository.idsQueSigueDeEntre(quienBusca, candidatos));
    }

    private Map<Long, Long> reseniasDe(List<User> gente) {
        if (gente.isEmpty()) {
            return Map.of();
        }
        return ratingRepository.contarPorUsuario(gente.stream().map(User::getId).toList()).stream()
            .collect(Collectors.toMap(ReseniasPorUsuarioDto::userId, ReseniasPorUsuarioDto::total));
    }
}
