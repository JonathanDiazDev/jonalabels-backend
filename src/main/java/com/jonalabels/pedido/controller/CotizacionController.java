package com.jonalabels.pedido.controller;

import com.jonalabels.pedido.domain.EstadoCotizacion;
import com.jonalabels.pedido.dto.CotizacionRequestDTO;
import com.jonalabels.pedido.dto.CotizacionResponseDTO;
import com.jonalabels.pedido.dto.MetricasDashboardDTO;
import com.jonalabels.pedido.service.CotizacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/cotizaciones")
@RequiredArgsConstructor
public class CotizacionController {

    private final CotizacionService cotizacionService;

    @PostMapping
    public ResponseEntity<Void> crearCotizacion(
            @RequestPart("data") @Valid CotizacionRequestDTO request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {

        cotizacionService.crear(request, archivo);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/exportar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado) {
        EstadoCotizacion estadoEnum = null;
        if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
            estadoEnum = EstadoCotizacion.valueOf(estado.toUpperCase());
        }
        var csv = cotizacionService.exportarCotizacionesCsv(busqueda, estadoEnum);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"prospectos_jonalabels.csv\"");
        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

    @GetMapping("/metricas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MetricasDashboardDTO> obtenerMetricas() {
        return ResponseEntity.ok(cotizacionService.obtenerMetricas());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CotizacionResponseDTO>> listarCotizaciones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado) {
        EstadoCotizacion estadoEnum = null;
        if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
            estadoEnum = EstadoCotizacion.valueOf(estado.toUpperCase());
        }
        var pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(cotizacionService.obtenerCotizacionesPaginadas(busqueda, estadoEnum, pageable));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CotizacionResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoCotizacion estado) {
        var actualizada = cotizacionService.actualizarEstado(id, estado);
        return ResponseEntity.ok(actualizada);
    }
}
