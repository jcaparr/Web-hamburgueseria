package com.hamburguesas.model;

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
 * Que la base acepte exactamente las reacciones que el código sabe guardar (#365).
 *
 * Los demás tests corren sobre H2, donde el esquema lo arma Hibernate y la restricción de
 * la tabla no existe: solo vive en las migraciones. Cuando FUEGO y APLAUSO pasaron a ser
 * MARAVILLOSO y ENCANTA, olvidar la migración habría hecho que la base rechazara cada
 * reacción nueva recién en producción. Por eso se compara el enum con la última
 * restricción, leyendo el archivo.
 */
class TiposDeReaccionEnLaBaseTest {

    private static final Path MIGRACIONES = Path.of("src/main/resources/db/migration");

    private static final Pattern RESTRICCION =
        Pattern.compile("check\\s*\\(\\s*tipo\\s+in\\s*\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);

    @Test
    void laBaseAceptaTodasLasReaccionesDelCodigoYNingunaOtra() throws IOException {
        assertThat(tiposQueAceptaLaBase())
            .describedAs("la restricción de reacciones tiene que nombrar los valores de "
                + "TipoDeReaccion; si cambiaste el enum, falta la migración")
            .containsExactlyInAnyOrderElementsOf(
                Arrays.stream(TipoDeReaccion.values()).map(Enum::name).toList());
    }

    /** Las reacciones que ya estaban puestas no se pierden: se convierten, no se borran. */
    @Test
    void laMigracionConvierteLasReaccionesViejasAntesDePonerLaRestriccion() throws IOException {
        String sql = Files.readString(
            MIGRACIONES.resolve("V45__reacciones_maravilloso_y_encanta.sql"), StandardCharsets.UTF_8);

        assertThat(sql)
            .contains("set tipo = 'MARAVILLOSO' where tipo = 'FUEGO'")
            .contains("set tipo = 'ENCANTA'     where tipo = 'APLAUSO'");
        assertThat(sql.indexOf("drop constraint")).isLessThan(sql.indexOf("update reacciones"));
        assertThat(sql.indexOf("update reacciones")).isLessThan(sql.indexOf("add constraint"));
    }

    private List<String> tiposQueAceptaLaBase() throws IOException {
        try (Stream<Path> archivos = Files.list(MIGRACIONES)) {
            Optional<String> ultima = archivos
                .filter(archivo -> archivo.getFileName().toString().endsWith(".sql"))
                .sorted(Comparator.comparingInt(TiposDeReaccionEnLaBaseTest::version))
                .map(TiposDeReaccionEnLaBaseTest::leer)
                .filter(sql -> RESTRICCION.matcher(sql).find())
                .reduce((primera, segunda) -> segunda);

            assertThat(ultima).describedAs("ninguna migración define la restricción de reacciones").isPresent();

            Matcher matcher = RESTRICCION.matcher(ultima.get());
            assertThat(matcher.find()).isTrue();

            return Arrays.stream(matcher.group(1).split(","))
                .map(valor -> valor.trim().replace("'", ""))
                .filter(valor -> !valor.isEmpty())
                .toList();
        }
    }

    private static int version(Path archivo) {
        Matcher matcher = Pattern.compile("^V(\\d+)__").matcher(archivo.getFileName().toString());
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
