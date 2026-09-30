package com.hamburguesas.service;

import com.hamburguesas.dto.RatingRequest;
import com.hamburguesas.dto.RatingResponse;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.fotos.FotosDeResenias;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final BurgerJointRepository burgerJointRepository;
    private final UserRepository userRepository;
    private final FotosDeResenias fotos;

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

    @Transactional(readOnly = true)
    public Page<RatingResponse> list(Long burgerJointId, Pageable pageable) {
        return ratingRepository
            .findByBurgerJoint_IdOrderByCreatedAtDesc(burgerJointId, pageable)
            .map(this::toResponse);
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
            r.getId(), r.getUser().getId(), r.getUser().getUsername(),
            r.getScore(), r.getComment(), r.getPhotoUrl(), r.getCreatedAt()
        );
    }
}
