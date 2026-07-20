package com.jonalabels.resena.dto;

import com.jonalabels.resena.domain.EstadoModeracion;
import jakarta.validation.constraints.NotNull;

public record ResenaModerarRequestDTO(
        @NotNull(message = "El estado de moderación es obligatorio")
        EstadoModeracion estado
) {
}
