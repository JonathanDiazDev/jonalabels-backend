package com.jonalabels.archivo.controller;

import com.jonalabels.archivo.dto.ArchivoResponseDTO;
import com.jonalabels.archivo.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/archivos")
@RequiredArgsConstructor
public class ArchivoController {

    private final StorageService storageService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ArchivoResponseDTO> subirArchivo(
            @RequestParam("archivo") MultipartFile archivo) {
        String nombreGuardado = storageService.guardar(archivo);
        String url = "/api/v1/archivos/" + nombreGuardado;
        return ResponseEntity.ok(new ArchivoResponseDTO(nombreGuardado, url));
    }

    @GetMapping("/{nombreArchivo}")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable String nombreArchivo) {
        Resource recurso = storageService.cargarComoRecurso(nombreArchivo);

        String contentType = determineContentType(nombreArchivo);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + recurso.getFilename() + "\"")
                .body(recurso);
    }

    private String determineContentType(String nombreArchivo) {
        String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }
}
