package com.jonalabels.resena.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ResenaCreateRequestDTO(
        @NotNull(message = "El id del usuario es obligatorio")
        @Positive(message = "El id del usuario debe ser positivo")
        Long usuarioId,

        @NotNull(message = "El id del pedido es obligatorio")
        @Positive(message = "El id del pedido debe ser positivo")
        Long pedidoId,

        @NotNull(message = "La calificación es obligatoria")
        @Min(value = 1, message = "La calificación mínima es 1")
        @Max(value = 5, message = "La calificación máxima es 5")
        Integer calificacion,

        String comentario
) {
}
