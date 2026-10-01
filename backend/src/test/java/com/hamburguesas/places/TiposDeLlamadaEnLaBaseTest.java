package com.hamburguesas.places;

import com.hamburguesas.model.PlacesCallType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Que la base acepte todos los tipos de llamada que el código sabe contar.
 *
 * Esto se escribió después de que no lo hiciera. Se agregó RESUMEN al enum y a la
 * configuración, pero la restricción de la tabla seguía admitiendo los tres anteriores,
 * así que la base rechazaba el primer resumen que se intentaba anotar. Eso no pasa al
 * arrancar sino en la mitad de la sincronización, y la excepción se llevaba puesto todo
 * lo que faltaba hacer.
 *
 * El resto de los tests no podía verlo: corren sobre H2, donde el esquema lo arma
 * Hibernate desde las entidades y esta restricción no existe. Vive solo en la migración.
 *
 * Por eso este test lee el archivo en vez de la base. Es raro y se banca: lo que hay que
 * atajar es que el enum crezca y la migración se quede, y para eso alcanza con comparar
 * las dos listas sin levantar un Postgres.
 */
class TiposDeLlamadaEnLaBaseTest {

    private static final Path MIGRACIONES = Path.of("src/main/resources/db/migration");

    /** Lo que declara la última migración que tocó la restricción. */
    private static final Pattern RESTRICCION =
        Pattern.compile("check\\s*\\(\\s*call_type\\s+in\\s*\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);

    @Test
    void laBaseAceptaTodosLosTiposQueElCodigoCuenta() throws IOException {
        List<String> aceptados = tiposQueAceptaLaBase();

        assertThat(aceptados)
            .describedAs("la restricción de places_api_usage tiene que nombrar todos los "
                + "valores de PlacesCallType; si agregaste uno, falta la migración")
            .containsExactlyInAnyOrderElementsOf(
                Arrays.stream(PlacesCallType.values()).map(Enum::name).toList());
    }

    /**
     * Los tipos que nombra la última migración que redefinió la restricción.
     *
     * La última y no todas: cada una la reemplaza entera, así que la que manda es la de
     * número más alto. Se ordenan por número y no alfabéticamente, que es lo que hace
     * Flyway y lo que evita que V9 venga después de V22.
     */
    private List<String> tiposQueAceptaLaBase() throws IOException {
        try (Stream<Path> archivos = Files.list(MIGRACIONES)) {
            Optional<String> ultima = archivos
                .filter(archivo -> archivo.getFileName().toString().endsWith(".sql"))
                .sorted(Comparator.comparingInt(TiposDeLlamadaEnLaBaseTest::version))
                .map(TiposDeLlamadaEnLaBaseTest::leer)
                .filter(sql -> RESTRICCION.matcher(sql).find())
                .reduce((primera, segunda) -> segunda);

            assertThat(ultima)
                .describedAs("ninguna migración define la restricción de call_type")
                .isPresent();

            Matcher matcher = RESTRICCION.matcher(ultima.get());
            assertThat(matcher.find()).isTrue();

            return Arrays.stream(matcher.group(1).split(","))
                .map(valor -> valor.trim().replace("'", ""))
                .filter(valor -> !valor.isEmpty())
                .toList();
        }
    }

    private static int version(Path archivo) {
        String nombre = archivo.getFileName().toString();
        Matcher matcher = Pattern.compile("^V(\\d+)__").matcher(nombre);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static String leer(Path archivo) {
        try {
            return Files.readString(archivo, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer " + archivo, ex);
        }
    }
}
