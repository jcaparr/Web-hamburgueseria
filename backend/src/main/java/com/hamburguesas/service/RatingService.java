package com.hamburguesas.service;

import com.hamburguesas.dto.NotaYCuantasDto;
import com.hamburguesas.dto.RatingRequest;
import com.hamburguesas.dto.RatingResponse;
import com.hamburguesas.dto.ResumenDeReseniasDto;
import com.hamburguesas.dto.TemaDeReseniasDto;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.fotos.FotosDeResenias;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final BurgerJointRepository burgerJointRepository;
    private final UserRepository userRepository;
    private final FotosDeResenias fotos;
    private final FollowRepository followRepository;
    private final Bloqueos bloqueos;

    /**
     * La foto es parte de la reseña, no un agregado posterior.
     *
     * Antes se guardaba el texto primero y la foto después, para que un problema con la
     * foto no se llevara puesto lo escrito. Con la foto obligatoria ese razonamiento se
     * da vuelta: una reseña sin foto no es una reseña, así que guardarla igual sería
     * dejar en la base algo que la app no considera válido. Van juntas o no va ninguna.
     */
    @Transactional
    public RatingResponse rate(Long userId, Long burgerJointId, RatingRequest request,
                               MultipartFile foto) {
        if (ratingRepository.findByUser_IdAndBurgerJoint_Id(userId, burgerJointId).isPresent()) {
            throw new ConflictException("You already rated this burger joint. Update it instead of creating a new one.");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        BurgerJoint burgerJoint = burgerJointRepository.findById(burgerJointId)
            .orElseThrow(() -> new ResourceNotFoundException("Burger joint not found"));

        // Se guarda antes de tocar la base: si la foto no sirve, la transacción no
        // llegó a escribir nada y no hay archivo que limpiar.
        String rutaDeLaFoto = fotos.guardar(exigir(foto));

        Rating rating = Rating.builder()
            .user(user)
            .burgerJoint(burgerJoint)
            .score(request.score())
            .comment(request.comment())
            .photoUrl(rutaDeLaFoto)
            .build();

        rating = ratingRepository.save(rating);
        return toResponse(rating);
    }

    /**
     * Al editar, la foto solo hace falta si la reseña todavía no tiene.
     *
     * Cambiar una coma no puede obligar a volver a sacar la foto. Pero las reseñas de
     * antes de esta regla no tienen ninguna, y esas sí la piden: es la forma de que el
     * "toda reseña tiene foto" termine siendo cierto sin borrarle la reseña a nadie.
     */
    @Transactional
    public RatingResponse update(Long userId, Long burgerJointId, RatingRequest request,
                                 MultipartFile foto) {
        Rating rating = ratingRepository
            .findByUser_IdAndBurgerJoint_Id(userId, burgerJointId)
            .orElseThrow(() -> new ResourceNotFoundException("You haven't rated this burger joint yet"));

        boolean mandaFoto = foto != null && !foto.isEmpty();
        if (!mandaFoto && rating.getPhotoUrl() == null) {
            throw new ConflictException("Tu reseña necesita una foto");
        }

        if (mandaFoto) {
            String anterior = rating.getPhotoUrl();
            rating.setPhotoUrl(fotos.guardar(leer(foto)));
            if (anterior != null) {
                fotos.borrar(anterior);
            }
        }

        rating.setScore(request.score());
        rating.setComment(request.comment());
        return toResponse(rating);
    }

    private byte[] exigir(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new ConflictException("Toda reseña lleva una foto de lo que comiste");
        }
        return leer(foto);
    }

    /**
     * Las reseñas de un local, sin las de la gente con la que hay un bloqueo.
     *
     * Es la tercera pantalla donde aparece gente, después del feed y del buscador, y
     * hasta ahora era la única que no miraba los bloqueos: para volver a cruzarte con
     * alguien que bloqueaste alcanzaba con abrir una hamburguesería que los dos
     * hubieran visitado.
     *
     * Lo que no cambia es el promedio del local ni la distribución de notas. El puntaje
     * de una hamburguesería es un hecho del lugar, no de quién lo mira: si dependiera de
     * a quién bloqueaste, dos personas verían promedios distintos del mismo local y
     * ninguna sabría por qué. Se esconde a la persona, no se reescribe la nota.
     */
    @Transactional(readOnly = true)
    public Page<RatingResponse> list(Long burgerJointId, Long userId, Pageable pageable) {
        return ratingRepository
            .deUnLocalSalvo(burgerJointId, bloqueos.queNoPuedeVer(userId), pageable)
            .map(this::toResponse);
    }

    /**
     * Lo que va arriba de la lista de reseñas: la distribución de notas y lo que
     * dijeron los que seguís.
     *
     * Sin sesión la segunda parte viene vacía en vez de dar error. Un local se puede
     * mirar sin cuenta, y la distribución es información del local, no de nadie.
     */
    @Transactional(readOnly = true)
    public ResumenDeReseniasDto resumen(Long burgerJointId, Long userId) {
        Map<Integer, Long> cuantasPorNota = ratingRepository
            .distribucionDeNotas(burgerJointId).stream()
            .collect(Collectors.toMap(NotaYCuantasDto::nota, NotaYCuantasDto::cuantas));

        List<NotaYCuantasDto> distribucion = IntStream.rangeClosed(1, 5)
            .mapToObj(nota -> new NotaYCuantasDto(nota, cuantasPorNota.getOrDefault(nota, 0L)))
            .toList();

        return new ResumenDeReseniasDto(distribucion, temas(burgerJointId),
            deQuienesSigo(burgerJointId, userId));
    }

    /**
     * De qué hablan las reseñas escritas de este local.
     *
     * El cálculo vive aparte, sin Spring ni base de datos, porque lo que hay que poder
     * probar es qué cuenta como mención y qué no: acá adentro solo se traen las reseñas
     * y se traduce el resultado.
     */
    private List<TemaDeReseniasDto> temas(Long burgerJointId) {
        List<TemasDeLasResenias.Mencion> menciones = ratingRepository
            .notasYComentarios(burgerJointId).stream()
            .map(fila -> new TemasDeLasResenias.Mencion(fila.nota(), fila.comentario()))
            .toList();

        return TemasDeLasResenias.de(menciones).stream()
            .map(tema -> new TemaDeReseniasDto(tema.nombre(), tema.menciones(), tema.aFavor()))
            .toList();
    }

    private List<RatingResponse> deQuienesSigo(Long burgerJointId, Long userId) {
        if (userId == null) {
            return List.of();
        }

        List<Long> seguidos = new ArrayList<>(followRepository.idsQueSigue(userId));
        seguidos.removeAll(bloqueos.queNoPuedeVer(userId));
        if (seguidos.isEmpty()) {
            return List.of();
        }

        return ratingRepository.deAutoresEn(burgerJointId, seguidos);
    }

    private byte[] leer(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new ConflictException("No llegó ninguna foto");
        }
        try {
            return foto.getBytes();
        } catch (IOException ex) {
            throw new ConflictException("No pudimos leer esa foto. Probá de nuevo.");
        }
    }

    private RatingResponse toResponse(Rating r) {
        return new RatingResponse(
            r.getId(), r.getUser().getId(), r.getUser().getUsername(), r.getUser().getHamburguesa(),
            r.getScore(), r.getComment(), r.getPhotoUrl(), r.getCreatedAt()
        );
    }
}
