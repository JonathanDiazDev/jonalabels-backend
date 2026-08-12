package com.jonalabels.pedido.controller;

import com.jonalabels.pedido.dto.PedidoAdminResponseDTO;
import com.jonalabels.pedido.dto.PedidoCotizarRequestDTO;
import com.jonalabels.pedido.dto.PedidoCreateRequestDTO;
import com.jonalabels.pedido.dto.PedidoResponseDTO;
import com.jonalabels.pedido.service.PedidoService;
import com.jonalabels.auth.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PedidoResponseDTO> crearSolicitud(
            @Valid @RequestBody PedidoCreateRequestDTO request) {
        Long usuarioId = currentUserService.requireCurrentUser().getId();
        var pedido = pedidoService.crearSolicitud(
                usuarioId,
                request.productoId(),
                request.disenoId(),
                request.cantidad(),
                request.urlDiseno());
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponseDTO.from(pedido));
    }

    @PatchMapping("/{id}/cotizacion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PedidoAdminResponseDTO> cotizarPedido(
            @PathVariable Long id,
            @Valid @RequestBody PedidoCotizarRequestDTO request) {
        var pedido = pedidoService.cotizarPedido(
                id,
                request.tallerId(),
                request.costoTaller(),
                request.precioFinal(),
                request.comentarios());
        return ResponseEntity.ok(PedidoAdminResponseDTO.from(pedido));
    }

    @PatchMapping("/{id}/pago")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PedidoResponseDTO> registrarPago(@PathVariable Long id) {
        Long usuarioId = currentUserService.requireCurrentUser().getId();
        var pedido = pedidoService.registrarPago(id, usuarioId);
        return ResponseEntity.ok(PedidoResponseDTO.from(pedido));
    }
}
