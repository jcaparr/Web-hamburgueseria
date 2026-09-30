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

    @Transactional
    public RatingResponse rate(Long userId, Long burgerJointId, RatingRequest request) {
        if (ratingRepository.findByUser_IdAndBurgerJoint_Id(userId, burgerJointId).isPresent()) {
            throw new ConflictException("You already rated this burger joint. Update it instead of creating a new one.");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        BurgerJoint burgerJoint = burgerJointRepository.findById(burgerJointId)
            .orElseThrow(() -> new ResourceNotFoundException("Burger joint not found"));

        Rating rating = Rating.builder()
            .user(user)
            .burgerJoint(burgerJoint)
            .score(request.score())
            .comment(request.comment())
            .build();

        rating = ratingRepository.save(rating);
        return toResponse(rating);
    }

    @Transactional
    public RatingResponse update(Long userId, Long burgerJointId, RatingRequest request) {
        Rating rating = ratingRepository
            .findByUser_IdAndBurgerJoint_Id(userId, burgerJointId)
            .orElseThrow(() -> new ResourceNotFoundException("You haven't rated this burger joint yet"));

        rating.setScore(request.score());
        rating.setComment(request.comment());
        return toResponse(rating);
    }

    @Transactional(readOnly = true)
    public Page<RatingResponse> list(Long burgerJointId, Pageable pageable) {
        return ratingRepository
            .findByBurgerJoint_IdOrderByCreatedAtDesc(burgerJointId, pageable)
            .map(this::toResponse);
    }

    /**
     * Subir una foto nueva reemplaza la anterior, y borra su archivo.
     *
     * Sin eso, cambiar de foto tres veces dejaría tres archivos en disco de los que
     * solo uno se muestra, y nadie volvería a mirar los otros dos.
     */
    @Transactional
    public RatingResponse guardarFoto(Long userId, Long burgerJointId, MultipartFile foto) {
        Rating rating = miResenia(userId, burgerJointId);
        String anterior = rating.getPhotoUrl();

        rating.setPhotoUrl(fotos.guardar(leer(foto)));

        if (anterior != null) {
            fotos.borrar(anterior);
        }
        return toResponse(rating);
    }

    @Transactional
    public RatingResponse borrarFoto(Long userId, Long burgerJointId) {
        Rating rating = miResenia(userId, burgerJointId);
        String anterior = rating.getPhotoUrl();

        rating.setPhotoUrl(null);

        if (anterior != null) {
            fotos.borrar(anterior);
        }
        return toResponse(rating);
    }

    /** La reseña de esa persona sobre ese local: nadie toca la de otro. */
    private Rating miResenia(Long userId, Long burgerJointId) {
        return ratingRepository.findByUser_IdAndBurgerJoint_Id(userId, burgerJointId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Primero escribí tu reseña, después le agregás la foto"));
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
