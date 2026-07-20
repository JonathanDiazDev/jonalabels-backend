package com.jonalabels.pedido.service;

import com.jonalabels.pedido.domain.Pedido;

import java.math.BigDecimal;

public interface PedidoService {

    Pedido crearSolicitud(Long usuarioId, Long productoId, Long disenoId, int cantidad, String urlDiseno);

    Pedido cotizarPedido(Long pedidoId, Long tallerId, BigDecimal costoTaller,
                         BigDecimal precioFinal, String comentarios);

    Pedido registrarPago(Long pedidoId);
}
