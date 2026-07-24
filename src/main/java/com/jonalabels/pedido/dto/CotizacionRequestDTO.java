package com.jonalabels.pedido.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CotizacionRequestDTO(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El WhatsApp es obligatorio")
        String whatsapp,

        String email,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 5000, message = "El pedido mínimo es de 5000 piezas")
        Integer cantidad,

        @NotBlank(message = "Las medidas son obligatorias")
        String medidas
) {}
