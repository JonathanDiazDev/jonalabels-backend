package com.jonalabels.resena.service;

import com.jonalabels.common.exception.RecursoNoEncontradoException;
import com.jonalabels.pedido.domain.EstadoPedido;
import com.jonalabels.pedido.domain.Pedido;
import com.jonalabels.pedido.repository.PedidoRepository;
import com.jonalabels.resena.domain.EstadoModeracion;
import com.jonalabels.resena.domain.Resena;
import com.jonalabels.resena.repository.ResenaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
class ResenaServiceImpl implements ResenaService {

    private final ResenaRepository resenaRepository;
    private final PedidoRepository pedidoRepository;

    @Override
    public Resena crearResena(Long usuarioId, Long pedidoId, int calificacion, String comentario) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido", pedidoId));

        if (!pedido.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalStateException("Solo compradores verificados pueden dejar una reseña");
        }

        if (pedido.getEstado() != EstadoPedido.PAGADO) {
            throw new IllegalStateException("Solo compradores verificados pueden dejar una reseña");
        }

        Resena resena = Resena.builder()
                .usuarioId(usuarioId)
                .pedidoId(pedidoId)
                .productoId(pedido.getProducto().getId())
                .calificacion(calificacion)
                .comentario(comentario)
                .estadoModeracion(EstadoModeracion.PENDIENTE)
                .build();

        return resenaRepository.save(resena);
    }

    @Override
    public Resena moderarResena(Long resenaId, EstadoModeracion estado) {
        Resena resena = resenaRepository.findById(resenaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Resena", resenaId));

        resena.setEstadoModeracion(estado);

        return resenaRepository.save(resena);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resena> obtenerResenasAprobadas() {
        return resenaRepository.findByEstadoModeracion(EstadoModeracion.APROBADA);
    }
}
