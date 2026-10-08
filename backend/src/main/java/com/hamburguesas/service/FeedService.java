package com.hamburguesas.service;

import com.hamburguesas.dto.ItemDeFeedDto;
import com.hamburguesas.dto.PaginaDeFeedDto;
import com.hamburguesas.dto.ReaccionesDto;
import com.hamburguesas.model.FuenteDelFeed;
import com.hamburguesas.repository.FollowRepository;
import com.hamburguesas.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Las últimas reseñas, de todos o solo de quienes seguís.
 *
 * Se arma al momento, consultando las reseñas y cortando por fecha. No hay una tabla
 * de feed que se vaya llenando a medida que alguien publica: con este tamaño sería
 * trabajo de más y una fuente de estados que no coinciden, y la consulta directa
 * aguanta de sobra.
 */
@Service
@RequiredArgsConstructor
public class FeedService {

    /** Cuántas reseñas trae cada tramo. */
    private static final int POR_PAGINA = 20;

    private final RatingRepository ratingRepository;
    private final FollowRepository followRepository;
    private final Bloqueos bloqueos;
    private final Reacciones reacciones;

    public PaginaDeFeedDto ver(FuenteDelFeed fuente, String cursor, Long quienMira) {
        Corte corte = Corte.de(cursor);
        List<Long> ocultos = bloqueos.queNoPuedeVer(quienMira);

        // Se pide uno de más para saber si hay página siguiente. La alternativa es
        // contar el total, que con esto cuesta una consulta entera de más para
        // responder algo que este registro sobrante ya contesta.
        var pagina = PageRequest.of(0, POR_PAGINA + 1);
        List<ItemDeFeedDto> traidos = switch (fuente) {
            case TODOS -> ratingRepository.feedDeTodos(corte.fecha(), corte.id(), ocultos, pagina);
            case SIGUIENDO -> deQuienesSigue(corte, pagina, ocultos, quienMira);
        };

        boolean hayMas = traidos.size() > POR_PAGINA;
        List<ItemDeFeedDto> items = conReacciones(
            conTodasLasFotos(hayMas ? traidos.subList(0, POR_PAGINA) : traidos), quienMira);

        return new PaginaDeFeedDto(items, hayMas ? cursorDe(items.get(items.size() - 1)) : null);
    }

    /**
     * La consulta del feed trae solo la portada de cada reseña: las demás fotos se
     * buscan acá, de una vez para toda la página (#185).
     */
    private List<ItemDeFeedDto> conTodasLasFotos(List<ItemDeFeedDto> items) {
        if (items.isEmpty()) {
            return items;
        }
        Map<Long, List<String>> fotos = ratingRepository.fotosPorResenia(
            items.stream().map(ItemDeFeedDto::ratingId).toList());
        return items.stream()
            .map(item -> item.conFotos(fotos.getOrDefault(item.ratingId(), item.fotosDeLaResenia())))
            .toList();
    }

    /** Las reacciones de toda la página, en dos consultas y no dos por tarjeta (#186). */
    private List<ItemDeFeedDto> conReacciones(List<ItemDeFeedDto> items, Long quienMira) {
        if (items.isEmpty()) {
            return items;
        }
        Map<Long, ReaccionesDto> deCadaUna = reacciones.de(
            items.stream().map(ItemDeFeedDto::ratingId).toList(), quienMira);
        return items.stream()
            .map(item -> item.conReacciones(deCadaUna.getOrDefault(item.ratingId(), ReaccionesDto.NINGUNA)))
            .toList();
    }

    private List<ItemDeFeedDto> deQuienesSigue(Corte corte, PageRequest pagina,
                                               List<Long> ocultos, Long quienMira) {
        List<Long> sigueA = followRepository.idsQueSigue(quienMira);
        if (sigueA.isEmpty()) {
            // Sin esto la consulta llevaría un "in ()" vacío. Además ahorra la ida a la
            // base para contestar algo que ya se sabe: no sigue a nadie, no hay nada.
            return List.of();
        }
        return ratingRepository.feedDe(sigueA, corte.fecha(), corte.id(), ocultos, pagina);
    }

    /**
     * Por dónde seguir: el instante y el id de la última reseña mostrada.
     *
     * Van los dos porque dos reseñas pueden compartir el instante, y con la fecha sola
     * el corte entre una página y la siguiente podría saltear o repetir una.
     */
    private record Corte(Instant fecha, Long id) {

        /**
         * El corte de la primera pantalla: uno que deja pasar todo.
         *
         * Lo natural sería mandar null y que la consulta lo ignore, pero Postgres no
         * puede deducir de qué tipo es un parámetro que solo aparece comparado contra
         * null y la consulta falla entera. Una fecha en el año 9999 dice lo mismo
         * —"desde el principio del feed"— sin ese problema, y la comparación de
         * siempre le deja usar el índice al planificador.
         */
        private static final Corte DESDE_EL_PRINCIPIO =
            new Corte(Instant.parse("9999-12-31T23:59:59Z"), Long.MAX_VALUE);

        /** Un cursor que no se entiende se trata como "desde el principio". */
        static Corte de(String cursor) {
            if (cursor == null || cursor.isBlank()) {
                return DESDE_EL_PRINCIPIO;
            }
            String[] partes = cursor.split("_");
            if (partes.length != 2) {
                return DESDE_EL_PRINCIPIO;
            }
            try {
                return new Corte(
                    Instant.ofEpochMilli(Long.parseLong(partes[0])), Long.parseLong(partes[1]));
            } catch (NumberFormatException e) {
                // Devolver la primera pantalla es mejor que un error: quien llegó acá
                // con un cursor roto ve el feed desde arriba, que es algo razonable.
                return DESDE_EL_PRINCIPIO;
            }
        }
    }

    private String cursorDe(ItemDeFeedDto ultimo) {
        return ultimo.createdAt().toEpochMilli() + "_" + ultimo.ratingId();
    }
}
