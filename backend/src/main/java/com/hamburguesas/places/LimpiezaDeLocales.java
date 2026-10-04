package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.SavedTourRepository;
import com.hamburguesas.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Revisa lo que ya está guardado: le corrige la zona y saca lo que no va.
 *
 * Hace falta porque los errores viejos no se arreglan solos. La zona equivocada sí se
 * corrige al volver a encontrar el local, pero un local que ninguna búsqueda devuelve
 * —los de otras provincias, justamente— se quedaría para siempre. En la base había 29 de
 * Mar del Plata, San Nicolás de los Arroyos, Las Flores, el conurbano, Colombia y México,
 * y 136 con el barrio cambiado.
 *
 * No cuesta ninguna búsqueda: los límites están en el disco y las coordenadas ya estaban
 * guardadas. Lo único que puede gastar es algún resumen de reseñas, y solo por los
 * locales que ninguna prueba barata alcanza a resolver.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LimpiezaDeLocales {

    private final BurgerJointRepository burgerJointRepository;
    private final Zonas zonas;
    private final ClasificadorDeLocales clasificador;
    private final RatingRepository ratingRepository;
    private final WishlistRepository wishlistRepository;
    private final SavedTourRepository savedTourRepository;

    LimpiezaResult revisarLoGuardado(Set<String> cadenas) {
        int corregidos = 0;
        int borrados = 0;

        // Los que van quedando, para reconocer al que ya vimos dos veces. Se arma acá y
        // no con una consulta porque comparar nombres y distancias no se escribe bien
        // en una consulta, y son cuatrocientos.
        List<BurgerJoint> quedan = new ArrayList<>();

        for (BurgerJoint joint : burgerJointRepository.findAll()) {
            var barrio = zonas.zonaDe(joint.getLatitude(), joint.getLongitude(), joint.getAddress());

            // De un local del que Google no tiene ni una foto no hay nada que mostrar:
            // la tarjeta queda con un recuadro de iniciales y nadie entra a mirarlo. Se
            // anota en la revisión de fotos, y solo cuando Google contestó que no tiene
            // ninguna: no tener la foto bajada es otra cosa, y es problema nuestro.
            boolean sinNadaQueMostrar = joint.isSinFotosEnGoogle();

            // Borrar es definitivo, así que no alcanza con que el veredicto diga que no:
            // tiene que decir por qué. Si no se pudo preguntar porque se acabó la cuota,
            // el local se queda. Un límite nuestro no puede terminar en borrarle la
            // hamburguesería a alguien, y el mes que viene se vuelve a mirar.
            Veredicto veredicto = clasificador.evaluarGuardado(joint, cadenas);
            boolean noSabemos = veredicto.prueba() == Veredicto.Prueba.NO_SE_PUDO_PREGUNTAR;

            boolean sobra = barrio.isEmpty()
                || sinNadaQueMostrar
                || clasificador.casiSinFotosYNoEsHamburgueseria(joint)
                || (!veredicto.vendeHamburguesas() && !noSabemos);

            if (sobra) {
                // Si alguien lo puntuó, lo anotó para ir o lo tiene en un recorrido
                // guardado, se queda: su decisión vale más que nuestra idea de qué
                // locales corresponden.
                if (fueTocadaPorAlguien(joint)) {
                    log.info("{} no corresponde pero alguien lo tiene guardado, se deja", joint.getName());
                    continue;
                }
                log.info("Se borra {} ({}): {}", joint.getName(), joint.getAddress(),
                    barrio.isEmpty() ? "fuera del radio de búsqueda"
                        : sinNadaQueMostrar ? "Google no tiene ninguna foto"
                        : "no es una hamburguesería");
                burgerJointRepository.delete(joint);
                borrados++;
                continue;
            }

            BurgerJoint repetido = Duplicados.elMismoLocalEntre(quedan, joint);
            if (repetido != null) {
                BurgerJoint sobrante = cualSobra(repetido, joint);
                if (sobrante == null) {
                    log.info("{} está dos veces y las dos tienen reseñas, se dejan", joint.getName());
                } else {
                    log.info("Se borra {} ({}): está repetido", sobrante.getName(), sobrante.getAddress());
                    quedan.remove(sobrante);
                    burgerJointRepository.delete(sobrante);
                    borrados++;
                    if (sobrante == repetido) {
                        quedan.add(joint);
                    }
                    continue;
                }
            }
            quedan.add(joint);

            // Los que todavía no tienen anotada su prueba la anotan ahora, para que la
            // limpieza que viene no tenga que volver a pagarla (#97). La primera vez sí
            // puede costar un resumen de reseñas; después, ninguno.
            boolean cambio = false;
            if (joint.getPruebaDeHamburguesas() == null && veredicto.vendeHamburguesas()) {
                joint.setPruebaDeHamburguesas(veredicto.prueba().name());
                cambio = true;
            }
            if (!barrio.get().equals(joint.getArea())) {
                joint.setArea(barrio.get());
                corregidos++;
                cambio = true;
            }
            if (cambio) {
                burgerJointRepository.save(joint);
            }
        }

        return new LimpiezaResult(corregidos, borrados);
    }

    /**
     * Cuál de las dos fichas repetidas hay que borrar, o null si no se puede borrar
     * ninguna porque las dos tienen reseñas. Juntar dos conjuntos de reseñas es una
     * decisión que no corresponde tomar acá, así que en ese caso quedan las dos y se
     * avisa en el registro.
     */
    private BurgerJoint cualSobra(BurgerJoint una, BurgerJoint otra) {
        boolean tocadaUna = fueTocadaPorAlguien(una);
        boolean tocadaOtra = fueTocadaPorAlguien(otra);

        if (tocadaUna && tocadaOtra) {
            return null;
        }
        if (tocadaUna) {
            return otra;
        }
        if (tocadaOtra) {
            return una;
        }
        // Ninguna tiene reseñas: se queda la que tiene foto, que es lo que se ve.
        BurgerJoint mejor = Duplicados.mejorDeLasDos(una, otra);
        return mejor == una ? otra : una;
    }

    /**
     * Si alguien la puntuó, la anotó para ir o la tiene en un recorrido guardado.
     *
     * Nada de eso se borra por decisión nuestra. Un tour guardado al que le falta una
     * parada no se puede rehacer, y quien lo guardó no tiene forma de saber qué le pasó.
     */
    private boolean fueTocadaPorAlguien(BurgerJoint joint) {
        return ratingRepository.existsByBurgerJoint_Id(joint.getId())
            || wishlistRepository.existsByBurgerJoint_Id(joint.getId())
            || savedTourRepository.estaEnAlgunTour(joint.getId());
    }
}
