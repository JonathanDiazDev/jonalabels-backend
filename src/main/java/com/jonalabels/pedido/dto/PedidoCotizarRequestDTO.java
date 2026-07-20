package com.jonalabels.pedido.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PedidoCotizarRequestDTO(
        @NotNull(message = "El id del taller es obligatorio")
        @Positive(message = "El id del taller debe ser positivo")
        Long tallerId,

        @NotNull(message = "El costo del taller es obligatorio")
        @Positive(message = "El costo del taller debe ser mayor a cero")
        BigDecimal costoTaller,

        @NotNull(message = "El precio final es obligatorio")
        @Positive(message = "El precio final debe ser mayor a cero")
        BigDecimal precioFinal,

        String comentarios
) {
}
