package com.hamburguesas.service;

import com.hamburguesas.dto.BurgerJointDto;
import com.hamburguesas.dto.GuardarTourRequest;
import com.hamburguesas.dto.SavedTourDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.Block;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.ModoDeViaje;
import com.hamburguesas.model.User;
import com.hamburguesas.model.WishlistItem;
import com.hamburguesas.repository.BlockRepository;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.UserRepository;
import com.hamburguesas.repository.WishlistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Las guardadas y los recorridos en el perfil de otro (#264): cada uno elige si los
 * muestra, arrancan ocultos, y el servidor no los entrega si no los muestra.
 */
@SpringBootTest
@ActiveProfiles("test")
class ListasDelPerfilTest {

    @Autowired private ListasDelPerfil listas;
    @Autowired private SocialService socialService;
    @Autowired private SavedTourService savedTourService;
    @Autowired private UserRepository userRepository;
    @Autowired private BurgerJointRepository burgerJointRepository;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private SavedTourRepository savedTourRepository;
    @Autowired private BlockRepository blockRepository;

    private User ana;
    private User beto;
    private BurgerJoint uno;
    private BurgerJoint dos;

    /** La base es compartida entre tests: se busca antes de crear, y se deja como estaba. */
    @BeforeEach
    void setUp() {
        limpiar();
        ana = alguien("analistas");
        beto = alguien("betolistas");
        uno = local("place-listas-uno", -34.58, -58.42);
        dos = local("place-listas-dos", -34.59, -58.43);
        wishlistRepository.save(WishlistItem.builder().user(ana).burgerJoint(uno).build());
        savedTourService.guardar(ana.getId(), new GuardarTourRequest(List.of(uno.getId(), dos.getId()), ModoDeViaje.A_PIE));
    }

    @AfterEach
    void tearDown() {
        limpiar();
    }

    private void limpiar() {
        blockRepository.deleteAll();
        wishlistRepository.deleteAll();
        savedTourRepository.deleteAll();
        userRepository.findByUsername("analistas").ifPresent(u -> {
            u.setMuestraListas(false);
            userRepository.save(u);
        });
    }

    private User alguien(String username) {
        return userRepository.findByUsername(username).orElseGet(() -> userRepository.save(
            User.builder().username(username).email(username + "@example.com").emailVerified(true).build()));
    }

    private BurgerJoint local(String placeId, double lat, double lng) {
        return burgerJointRepository.findByPlaceId(placeId).orElseGet(() -> burgerJointRepository.save(
            BurgerJoint.builder().name(placeId).address("Una calle 123").area("Palermo")
                .placeId(placeId).latitude(lat).longitude(lng).build()));
    }

    private void muestra() {
        listas.mostrar(ana.getId(), true);
    }

    @Test
    void arrancanOcultas() {
        assertThat(listas.lasMuestra(ana.getId())).isFalse();
        assertThat(socialService.perfil("analistas", beto.getId()).seVenSusListas()).isFalse();
        assertThatThrownBy(() -> listas.guardadas("analistas", beto.getId()))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> listas.recorridos("analistas", beto.getId()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void lasPropiasSeVenSiempre() {
        assertThat(socialService.perfil("analistas", ana.getId()).seVenSusListas()).isTrue();
        assertThat(listas.guardadas("analistas", ana.getId())).hasSize(1);
        assertThat(listas.recorridos("analistas", ana.getId())).hasSize(1);
    }

    @Test
    void siLasMuestraOtroLasVe() {
        muestra();

        assertThat(socialService.perfil("analistas", beto.getId()).seVenSusListas()).isTrue();
        List<BurgerJointDto> guardadas = listas.guardadas("analistas", beto.getId());
        assertThat(guardadas).extracting(BurgerJointDto::id).containsExactly(uno.getId());
        List<SavedTourDto> recorridos = listas.recorridos("analistas", beto.getId());
        assertThat(recorridos).hasSize(1);
        assertThat(recorridos.get(0).paradas()).hasSize(2);
    }

    /** El corazón de cada guardada es el de quien mira, no el del dueño. */
    @Test
    void elCorazonEsDeQuienMira() {
        muestra();

        assertThat(listas.guardadas("analistas", beto.getId()).get(0).inWishlist()).isFalse();
    }

    @Test
    void apagarlasLasVuelveAOcultar() {
        muestra();
        listas.mostrar(ana.getId(), false);

        assertThatThrownBy(() -> listas.guardadas("analistas", beto.getId()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /** Con un bloqueo de por medio no se ven, aunque las muestre: como el resto del perfil. */
    @Test
    void conUnBloqueoNoSeVen() {
        muestra();
        blockRepository.save(Block.builder().blocker(ana).blocked(beto).createdAt(Instant.now()).build());

        assertThatThrownBy(() -> listas.guardadas("analistas", beto.getId()))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> listas.recorridos("analistas", beto.getId()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /** La regla, sola: si un día cambia ("solo seguidores", "todos"), este es el test que cambia. */
    @Test
    void laRegla() {
        User oculta = User.builder().id(1L).muestraListas(false).build();
        User visible = User.builder().id(2L).muestraListas(true).build();

        assertThat(ListasDelPerfil.puedeVer(oculta, 1L)).isTrue();
        assertThat(ListasDelPerfil.puedeVer(oculta, 9L)).isFalse();
        assertThat(ListasDelPerfil.puedeVer(visible, 9L)).isTrue();
    }
}
