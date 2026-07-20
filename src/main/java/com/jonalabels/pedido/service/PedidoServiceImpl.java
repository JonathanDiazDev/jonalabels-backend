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

    private static final String ESTADO_ESPERANDO_FACTIBILIDAD = "ESPERANDO_FACTIBILIDAD";
    private static final String ESTADO_COTIZADO = "COTIZADO";
    private static final String ESTADO_PAGADO = "PAGADO";

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
                .estado(ESTADO_ESPERANDO_FACTIBILIDAD)
                .build();

        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public Pedido cotizarPedido(Long pedidoId, Long tallerId, BigDecimal costoTaller,
                                BigDecimal precioFinal, String comentarios) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido", pedidoId));

        if (!ESTADO_ESPERANDO_FACTIBILIDAD.equals(pedido.getEstado())) {
            throw new IllegalPedidoStateException(
                    pedidoId, pedido.getEstado(), ESTADO_ESPERANDO_FACTIBILIDAD);
        }

        Taller taller = tallerRepository.findById(tallerId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Taller", tallerId));

        pedido.setTaller(taller);
        pedido.setCostoTallerAcordado(costoTaller);
        pedido.setPrecioFinalCotizado(precioFinal);
        pedido.setComentariosAdmin(comentarios);
        pedido.setEstado(ESTADO_COTIZADO);

        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public Pedido registrarPago(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido", pedidoId));

        if (!ESTADO_COTIZADO.equals(pedido.getEstado())) {
            throw new IllegalPedidoStateException(
                    pedidoId, pedido.getEstado(), ESTADO_COTIZADO);
        }

        pedido.setEstado(ESTADO_PAGADO);

        return pedidoRepository.save(pedido);
    }
}
