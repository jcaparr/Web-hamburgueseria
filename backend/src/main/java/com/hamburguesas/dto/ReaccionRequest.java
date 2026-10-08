package com.hamburguesas.dto;

import com.hamburguesas.model.TipoDeReaccion;
import jakarta.validation.constraints.NotNull;

public record ReaccionRequest(@NotNull TipoDeReaccion tipo) {}
