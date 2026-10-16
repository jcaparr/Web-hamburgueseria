package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.dto.SavedTourDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.User;
import com.hamburguesas.model.WishlistItem;
import com.hamburguesas.repository.UserRepository;
import com.hamburguesas.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Las guardadas y los recorridos de otra persona, en su perfil público (#264).
 *
 * Hasta acá eran solo de quien los armaba. Ahora cada uno elige si los muestra, y
 * arrancan ocultos. La regla de quién los ve vive en un solo lugar, {@link #puedeVer}:
 * si un día pasa a ser "solo quienes lo siguen" o "todos", se cambia ahí y nada más.
 *
 * El servidor no devuelve las listas de quien no las muestra: esconder las pestañas en
 * la pantalla no alcanza, cualquiera puede pedir la dirección a mano.
 */
@Service
@RequiredArgsConstructor
public class ListasDelPerfil {

    private final SocialService socialService;
    private final WishlistRepository wishlistRepository;
    private final SavedTourService savedTourService;
    private final FichaDeLocal fichaDeLocal;
    private final UserRepository userRepository;

    /** Quien mira ve las listas de esta persona: las suyas siempre, las de otro si las muestra. */
    static boolean puedeVer(User duenio, Long quienMira) {
        return duenio.getId().equals(quienMira) || duenio.isMuestraListas();
    }

    /**
     * Sus guardadas, de la más nueva a la más vieja. El corazón de cada una es el de
     * quien mira: es lo que puede tocar desde ahí.
     */
    @Transactional(readOnly = true)
    public List<BurgerJointDto> guardadas(String username, Long quienMira) {
        User duenio = conListasVisibles(username, quienMira);
        return fichaDeLocal.para(
            wishlistRepository.findByUser_IdOrderByCreatedAtDesc(duenio.getId()).stream()
                .map(WishlistItem::getBurgerJoint)
                .toList(),
            quienMira);
    }

    /** Sus recorridos guardados, con las paradas marcadas según lo que puntuó quien mira. */
    @Transactional(readOnly = true)
    public List<SavedTourDto> recorridos(String username, Long quienMira) {
        User duenio = conListasVisibles(username, quienMira);
        return savedTourService.deUnaPersona(duenio.getId(), quienMira);
    }

    @Transactional(readOnly = true)
    public boolean lasMuestra(Long userId) {
        return usuario(userId).isMuestraListas();
    }

    @Transactional
    public boolean mostrar(Long userId, boolean muestra) {
        User user = usuario(userId);
        user.setMuestraListas(muestra);
        return muestra;
    }

    /**
     * La persona, si quien mira puede ver sus listas. Si no, un 404, como con un bloqueo:
     * para quien mira, esas listas no existen. El mensaje sí dice que no las muestra,
     * porque eso ya se sabe por el perfil (no tiene pestañas); lo que no se revela es un
     * bloqueo, que sigue contestando "No encontramos a esa persona".
     */
    private User conListasVisibles(String username, Long quienMira) {
        User duenio = socialService.visiblePara(username, quienMira);
        if (!puedeVer(duenio, quienMira)) {
            throw new ResourceNotFoundException("No muestra sus guardadas ni sus recorridos");
        }
        return duenio;
    }

    private User usuario(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
