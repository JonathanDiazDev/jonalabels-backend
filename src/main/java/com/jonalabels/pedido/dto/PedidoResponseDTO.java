package com.jonalabels.pedido.dto;

import com.jonalabels.pedido.domain.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PedidoResponseDTO(
        Long id,
        Long usuarioId,
        Long productoId,
        Long disenoId,
        Long tallerId,
        String estado,
        Integer cantidad,
        BigDecimal precioFinalCotizado,
        BigDecimal costoTallerAcordado,
        String comentariosAdmin,
        String urlDiseno,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
    public static PedidoResponseDTO from(Pedido pedido) {
        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getUsuario().getId(),
                pedido.getProducto().getId(),
                pedido.getDiseno().getId(),
                pedido.getTaller() != null ? pedido.getTaller().getId() : null,
                pedido.getEstado(),
                pedido.getCantidad(),
                pedido.getPrecioFinalCotizado(),
                pedido.getCostoTallerAcordado(),
                pedido.getComentariosAdmin(),
                pedido.getUrlDiseno(),
                pedido.getFechaCreacion(),
                pedido.getFechaActualizacion()
        );
    }
}
