package com.jonalabels.resena.controller;

import com.jonalabels.resena.domain.EstadoModeracion;
import com.jonalabels.resena.dto.ResenaCreateRequestDTO;
import com.jonalabels.resena.dto.ResenaModerarRequestDTO;
import com.jonalabels.resena.dto.ResenaResponseDTO;
import com.jonalabels.resena.service.ResenaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resenas")
@RequiredArgsConstructor
public class ResenaController {

    private final ResenaService resenaService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ResenaResponseDTO> crearResena(
            @Valid @RequestBody ResenaCreateRequestDTO request) {
        var resena = resenaService.crearResena(
                request.usuarioId(),
                request.pedidoId(),
                request.calificacion(),
                request.comentario());
        return ResponseEntity.status(HttpStatus.CREATED).body(ResenaResponseDTO.from(resena));
    }

    @PatchMapping("/{id}/moderacion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResenaResponseDTO> moderarResena(
            @PathVariable Long id,
            @Valid @RequestBody ResenaModerarRequestDTO request) {
        var resena = resenaService.moderarResena(id, request.estado());
        return ResponseEntity.ok(ResenaResponseDTO.from(resena));
    }

    @GetMapping
    public ResponseEntity<List<ResenaResponseDTO>> obtenerResenasAprobadas() {
        var resenas = resenaService.obtenerResenasAprobadas();
        var response = resenas.stream()
                .map(ResenaResponseDTO::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
