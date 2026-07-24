package com.jonalabels.pedido.dto;

import com.jonalabels.pedido.domain.EstadoCotizacion;

import java.time.LocalDateTime;

public record CotizacionResponseDTO(
        Long id,
        String nombre,
        String whatsapp,
        String email,
        Integer cantidad,
        String medidas,
        LocalDateTime fechaCreacion,
        EstadoCotizacion estado,
        String urlDiseno
) {}
