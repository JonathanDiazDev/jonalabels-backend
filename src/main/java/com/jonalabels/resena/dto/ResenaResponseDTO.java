package com.jonalabels.resena.dto;

import com.jonalabels.resena.domain.EstadoModeracion;
import com.jonalabels.resena.domain.Resena;

import java.time.LocalDateTime;

public record ResenaResponseDTO(
        Long id,
        Long usuarioId,
        Long pedidoId,
        Long productoId,
        Integer calificacion,
        String comentario,
        EstadoModeracion estadoModeracion,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
    public static ResenaResponseDTO from(Resena resena) {
        return new ResenaResponseDTO(
                resena.getId(),
                resena.getUsuarioId(),
                resena.getPedidoId(),
                resena.getProductoId(),
                resena.getCalificacion(),
                resena.getComentario(),
                resena.getEstadoModeracion(),
                resena.getFechaCreacion(),
                resena.getFechaActualizacion()
        );
    }
}
