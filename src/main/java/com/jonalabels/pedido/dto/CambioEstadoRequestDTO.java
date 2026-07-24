package com.jonalabels.pedido.dto;

import com.jonalabels.pedido.domain.EstadoCotizacion;

public record CambioEstadoRequestDTO(
        EstadoCotizacion estado
) {}
