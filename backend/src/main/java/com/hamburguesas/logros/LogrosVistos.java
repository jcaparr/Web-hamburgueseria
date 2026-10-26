package com.hamburguesas.logros;

import java.util.List;

/**
 * Lo que la pantalla avisa que ya mostró (#317).
 *
 * @param codigos       los logros del aviso
 * @param retroactivos  si también se mostró el aviso único de los de la carga inicial
 */
public record LogrosVistos(List<String> codigos, boolean retroactivos) {
}
