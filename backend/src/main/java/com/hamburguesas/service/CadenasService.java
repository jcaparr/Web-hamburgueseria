package com.hamburguesas.service;

import com.hamburguesas.dto.CadenaDto;
import com.hamburguesas.dto.SucursalesDeCadenaDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.places.FastFoodMarker;
import com.hamburguesas.places.PlacesProperties;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.texto.Texto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Las cadenas de comida rápida, que en Explorar se ven solo buscándolas por nombre (#206).
 *
 * Son 295 sucursales de siete marcas. Mezcladas con las hamburgueserías ocupaban páginas
 * enteras, así que Explorar no las lista nunca, y quien busca una por su nombre recibe
 * una tarjeta por cadena —"Mostaza · 22 sucursales"— que lleva a la página con todas.
 *
 * Las sucursales no se juntan en la base: cada una sigue con su ficha, sus reseñas, su
 * horario y su lugar en el mapa, porque la gente opina de la sucursal a la que fue. Lo
 * que se agrupa es solo lo que se muestra.
 *
 * Qué local es de qué cadena sale de la marca al principio del nombre, con la misma
 * regla que FastFoodMarker usa para marcarlos.
 */
@Service
@RequiredArgsConstructor
public class CadenasService {

    /** Con una letra, "m" ya trae McDonald's, Mostaza y Mr Tasty sin que nadie las pidiera. */
    private static final int MINIMO_PARA_BUSCAR = 2;

    private final BurgerJointRepository burgerJointRepository;
    private final PlacesProperties properties;
    private final FichaDeLocal fichaDeLocal;

    /**
     * Las cadenas que coinciden con lo que se escribió en el buscador.
     *
     * Coincide si el nombre de la cadena contiene lo escrito —"mc", "king", "burger"— o si
     * lo escrito empieza con la cadena —"mostaza palermo"—. Se compara sin espacios, signos
     * ni acentos, así que "Mc Donalds" y "mcdonald's" son lo mismo.
     *
     * @param barrios los que haya elegidos en Explorar. Con alguno, la tarjeta cuenta solo
     *                las sucursales de esos barrios, y una cadena sin ninguna ahí no sale.
     */
    public List<CadenaDto> buscar(String texto, List<String> barrios) {
        String buscado = Texto.soloLetrasYNumeros(texto);
        if (buscado.length() < MINIMO_PARA_BUSCAR) {
            return List.of();
        }

        List<String> coinciden = marcas().stream()
            .filter(marca -> marca.contains(buscado) || buscado.startsWith(marca))
            .toList();
        if (coinciden.isEmpty()) {
            return List.of();
        }

        Set<String> enBarrios = barrios == null ? Set.of() : barrios.stream()
            .filter(barrio -> barrio != null && !barrio.isBlank())
            .collect(Collectors.toSet());
        Map<String, List<BurgerJoint>> porMarca = sucursalesPorMarca();

        return coinciden.stream()
            .map(marca -> {
                List<BurgerJoint> ahi = porMarca.getOrDefault(marca, List.of()).stream()
                    .filter(local -> enBarrios.isEmpty() || enBarrios.contains(local.getArea()))
                    .toList();
                return ahi.isEmpty() ? null
                    : new CadenaDto(marca, nombreDe(marca), ahi.size(), portadaDe(ahi));
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingLong(CadenaDto::sucursales).reversed()
                .thenComparing(CadenaDto::nombre))
            .toList();
    }

    /** Todas las sucursales de una cadena, por barrio y después por nombre. */
    public SucursalesDeCadenaDto sucursales(String marca, Long userId) {
        String clave = Texto.soloLetrasYNumeros(marca);
        if (!marcas().contains(clave)) {
            throw new ResourceNotFoundException("No encontramos esa cadena");
        }

        List<BurgerJoint> deLaMarca = sucursalesPorMarca().getOrDefault(clave, List.of()).stream()
            .sorted(Comparator.comparing(BurgerJoint::getArea, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(BurgerJoint::getName)
                .thenComparing(BurgerJoint::getId))
            .toList();

        return new SucursalesDeCadenaDto(clave, nombreDe(clave), fichaDeLocal.para(deLaMarca, userId));
    }

    private List<String> marcas() {
        return properties.getFastFoodBrands().stream()
            .map(Texto::soloLetrasYNumeros)
            .filter(marca -> !marca.isBlank())
            .toList();
    }

    private Map<String, List<BurgerJoint>> sucursalesPorMarca() {
        List<String> marcas = marcas();
        return burgerJointRepository.findByFastFoodTrue().stream()
            .filter(local -> FastFoodMarker.marcaDe(local.getName(), marcas) != null)
            .collect(Collectors.groupingBy(local -> FastFoodMarker.marcaDe(local.getName(), marcas)));
    }

    /** Sin nombre anotado se muestra la clave, que se lee peor pero no deja la tarjeta vacía. */
    private String nombreDe(String marca) {
        return properties.getNombresDeCadenas().getOrDefault(marca, marca);
    }

    /** La foto que más sucursales comparten: casi siempre es una sola, prestada de una a otra. */
    private static String portadaDe(List<BurgerJoint> sucursales) {
        return sucursales.stream()
            .map(BurgerJoint::getPhotoUrl)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }
}
