package com.hamburguesas.service;

import com.hamburguesas.dto.ReaccionesDto;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.Rating;
import com.hamburguesas.model.Reaccion;
import com.hamburguesas.model.TipoDeReaccion;
import com.hamburguesas.repository.ReaccionRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reaccionar a la reseña de otro, cambiar la reacción o sacarla (#186).
 *
 * Las dos operaciones devuelven cómo quedaron las reacciones de esa reseña, contadas
 * de nuevo: si otra persona reaccionó mientras tanto, la pantalla se pone al día con
 * la respuesta, en vez de sumarle uno a un número viejo.
 */
@Service
@RequiredArgsConstructor
public class ReaccionesService {

    private final ReaccionRepository reaccionRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final Bloqueos bloqueos;
    private final Reacciones reacciones;

    /** Elegir otra reacción reemplaza a la que había: hay una por persona. */
    @Transactional
    public ReaccionesDto reaccionar(Long ratingId, Long quien, TipoDeReaccion tipo) {
        Rating resenia = laQuePuedeVer(ratingId, quien);
        // Reaccionar a lo propio sería aplaudirse: el número dejaría de decir qué les
        // pareció a los demás.
        if (resenia.getUser().getId().equals(quien)) {
            throw new ConflictException("No podés reaccionar a tu propia reseña");
        }

        reaccionRepository.findByRating_IdAndUser_Id(ratingId, quien).ifPresentOrElse(
            ya -> ya.setTipo(tipo),
            () -> reaccionRepository.save(Reaccion.builder()
                .rating(resenia)
                .user(userRepository.getReferenceById(quien))
                .tipo(tipo)
                .build()));

        return comoQuedo(ratingId, quien);
    }

    /** Sacar una reacción que no estaba no es un error: ya estaba como se quería. */
    @Transactional
    public ReaccionesDto sacar(Long ratingId, Long quien) {
        laQuePuedeVer(ratingId, quien);
        reaccionRepository.deleteByRating_IdAndUser_Id(ratingId, quien);
        return comoQuedo(ratingId, quien);
    }

    /**
     * La reseña, si quien pide la puede ver.
     *
     * Con un bloqueo de por medio se contesta lo mismo que si no existiera, igual que en
     * el perfil: decir "está bloqueado" le confirmaría al bloqueado que lo bloquearon.
     */
    private Rating laQuePuedeVer(Long ratingId, Long quien) {
        Rating resenia = ratingRepository.findById(ratingId)
            .orElseThrow(() -> new ResourceNotFoundException("No encontramos esa reseña"));
        if (bloqueos.hayEntre(quien, resenia.getUser().getId())) {
            throw new ResourceNotFoundException("No encontramos esa reseña");
        }
        return resenia;
    }

    private ReaccionesDto comoQuedo(Long ratingId, Long quien) {
        return reacciones.de(List.of(ratingId), quien).getOrDefault(ratingId, ReaccionesDto.NINGUNA);
    }
}
