package com.jonalabels.pedido.service;

import com.jonalabels.common.exception.RecursoNoEncontradoException;
import com.jonalabels.entity.Diseno;
import com.jonalabels.entity.Producto;
import com.jonalabels.entity.Taller;
import com.jonalabels.auth.domain.Usuario;
import com.jonalabels.auth.repository.UsuarioRepository;
import com.jonalabels.repository.DisenoRepository;
import com.jonalabels.repository.ProductoRepository;
import com.jonalabels.repository.TallerRepository;
import com.jonalabels.pedido.domain.EstadoPedido;
import com.jonalabels.pedido.domain.IllegalPedidoStateException;
import com.jonalabels.pedido.domain.Pedido;
import com.jonalabels.pedido.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final DisenoRepository disenoRepository;
    private final TallerRepository tallerRepository;

    @Override
    @Transactional
    public Pedido crearSolicitud(Long usuarioId, Long productoId, Long disenoId, int cantidad, String urlDiseno) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", productoId));

        Diseno diseno = disenoRepository.findById(disenoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Diseno", disenoId));

        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .producto(producto)
                .diseno(diseno)
                .cantidad(cantidad)
                .urlDiseno(urlDiseno)
                .estado(EstadoPedido.ESPERANDO_FACTIBILIDAD)
                .build();

        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public Pedido cotizarPedido(Long pedidoId, Long tallerId, BigDecimal costoTaller,
                                BigDecimal precioFinal, String comentarios) {
        Pedido pedido = pedidoRepository.findByIdConAsociaciones(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido", pedidoId));

        if (pedido.getEstado() != EstadoPedido.ESPERANDO_FACTIBILIDAD) {
            throw new IllegalPedidoStateException(
                    pedidoId, pedido.getEstado(), EstadoPedido.ESPERANDO_FACTIBILIDAD);
        }

        Taller taller = tallerRepository.findById(tallerId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Taller", tallerId));

        pedido.setTaller(taller);
        pedido.setCostoTallerAcordado(costoTaller);
        pedido.setPrecioFinalCotizado(precioFinal);
        pedido.setComentariosAdmin(comentarios);
        pedido.setEstado(EstadoPedido.COTIZADO);

        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public Pedido registrarPago(Long pedidoId, Long usuarioId) {
        Pedido pedido = pedidoRepository.findByIdConAsociaciones(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido", pedidoId));

        if (!pedido.getUsuario().getId().equals(usuarioId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para registrar el pago de este pedido");
        }

        if (pedido.getEstado() != EstadoPedido.COTIZADO) {
            throw new IllegalPedidoStateException(
                    pedidoId, pedido.getEstado(), EstadoPedido.COTIZADO);
        }

        pedido.setEstado(EstadoPedido.PAGADO);

        return pedidoRepository.save(pedido);
    }
}
