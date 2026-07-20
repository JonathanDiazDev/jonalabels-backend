package com.jonalabels.pedido.domain;

public class IllegalPedidoStateException extends RuntimeException {

    public IllegalPedidoStateException(String mensaje) {
        super(mensaje);
    }

    public IllegalPedidoStateException(Long pedidoId, String estadoActual, String estadoRequerido) {
        super(String.format(
                "Pedido %d en estado '%s' no puede realizar esta operación. Se requiere estado '%s'",
                pedidoId, estadoActual, estadoRequerido));
    }
}
