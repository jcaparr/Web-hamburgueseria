package com.hamburguesas.logros;

import java.util.List;

/** Lo de logros que lleva un perfil (#319): el exhibido y los que se pueden ver. */
public record PerfilDeLogros(String exhibido, List<String> activos) {
}
