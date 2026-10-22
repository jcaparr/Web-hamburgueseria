package com.hamburguesas.service;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.texto.Texto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Arma la dirección linda de cada local: burgometro.com.ar/<slug> (#305).
 *
 * El slug sale del nombre ("La Birra Bar" → la-birra-bar); si ya lo usa otro local se
 * le suma el barrio, y si sigue chocando, un número. Se arma una sola vez y no se toca
 * aunque cambie el nombre: un enlace compartido tiene que seguir andando.
 *
 * Corre al arrancar (para los locales que ya existen) y cada tanto (para los que trae
 * una sincronización), en vez de colgarse de cada lugar que crea locales: son varios, y
 * uno que se olvide dejaría un local sin dirección linda. Mientras no lo tiene, la web
 * usa la de siempre, con el id.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SlugsDeLocales {

    /**
     * Lo que un local no puede llamarse porque ya es una dirección de la app. Si se suma
     * una pantalla de primer nivel, va acá y en @vistaprevia del Caddyfile (un test
     * compara las dos listas).
     */
    public static final Set<String> RESERVADAS = Set.of(
        "api", "assets", "e", "u", "cadenas", "burger-joints", "ranking", "tour", "buscar",
        "feed", "profile", "reviews", "wishlist", "login", "register", "verify-email",
        "forgot-password", "terminos", "privacidad");

    private static final int LARGO_MAXIMO = 80;

    private final BurgerJointRepository burgerJointRepository;

    @Bean
    ApplicationRunner armarSlugsAlArrancar() {
        return args -> completar();
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS, initialDelay = 1)
    @Transactional
    public void completar() {
        List<BurgerJoint> pendientes = burgerJointRepository.findBySlugIsNull();
        if (pendientes.isEmpty()) {
            return;
        }
        Set<String> enUso = new HashSet<>(burgerJointRepository.slugsEnUso());
        for (BurgerJoint local : pendientes) {
            String slug = unicoPara(local.getName(), local.getArea(), enUso);
            local.setSlug(slug);
            enUso.add(slug);
        }
        burgerJointRepository.saveAll(pendientes);
        log.info("Locales a los que se les armó la dirección: {}", pendientes.size());
    }

    /** El slug de un local que no repite ninguno de {@code enUso}. */
    static String unicoPara(String nombre, String barrio, Set<String> enUso) {
        String base = slugDe(nombre);
        if (base.isEmpty()) {
            base = "local";
        }
        if (libre(base, enUso)) {
            return base;
        }
        String conBarrio = recortar(base + "-" + slugDe(barrio));
        if (!slugDe(barrio).isEmpty() && libre(conBarrio, enUso)) {
            return conBarrio;
        }
        for (int n = 2; ; n++) {
            String candidato = recortar(conBarrio) + "-" + n;
            if (libre(candidato, enUso)) {
                return candidato;
            }
        }
    }

    private static boolean libre(String slug, Set<String> enUso) {
        return !enUso.contains(slug) && !RESERVADAS.contains(slug);
    }

    /** Minúsculas, sin acentos, solo letras y números separados por guiones. */
    static String slugDe(String texto) {
        String limpio = Texto.sinAcentosEnMinuscula(texto)
            .replace("'", "")
            .replace("&", " y ")
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
        return recortar(limpio);
    }

    private static String recortar(String slug) {
        String corto = slug.length() > LARGO_MAXIMO ? slug.substring(0, LARGO_MAXIMO) : slug;
        return corto.replaceAll("-+$", "");
    }
}
