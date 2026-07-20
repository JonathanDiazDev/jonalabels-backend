package com.jonalabels.pedido.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PedidoCreateRequestDTO(
        @NotNull(message = "El id del usuario es obligatorio")
        @Positive(message = "El id del usuario debe ser positivo")
        Long usuarioId,

        @NotNull(message = "El id del producto es obligatorio")
        @Positive(message = "El id del producto debe ser positivo")
        Long productoId,

        @NotNull(message = "El id del diseño es obligatorio")
        @Positive(message = "El id del diseño debe ser positivo")
        Long disenoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        Integer cantidad,

        String urlDiseno
) {
}
