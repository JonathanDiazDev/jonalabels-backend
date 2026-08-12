package com.jonalabels.archivo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
class LocalFileSystemStorageService implements StorageService {

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "pdf");

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    private Path directorioDestino;

    @PostConstruct
    void init() throws IOException {
        directorioDestino = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(directorioDestino);
    }

    @Override
    public String guardar(MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        String nombreOriginal = StringUtils.cleanPath(archivo.getOriginalFilename());
        String extension = extraerExtension(nombreOriginal);

        if (!EXTENSIONES_PERMITIDAS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Tipo de archivo no permitido: ." + extension
                            + ". Extensiones válidas: " + EXTENSIONES_PERMITIDAS);
        }

        String nombreUnico = UUID.randomUUID() + "." + extension;
        Path destino = directorioDestino.resolve(nombreUnico);

        try {
            Files.copy(archivo.getInputStream(), destino);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error al guardar el archivo: " + e.getMessage(), e);
        }

        return nombreUnico;
    }

    @Override
    public Resource cargarComoRecurso(String nombreArchivo) {
        Path archivo = directorioDestino.resolve(nombreArchivo).normalize();

        if (!archivo.startsWith(directorioDestino)) {
            throw new IllegalArgumentException("Nombre de archivo no válido");
        }

        if (!Files.exists(archivo)) {
            throw new com.jonalabels.common.exception.RecursoNoEncontradoException(
                    "Archivo '" + nombreArchivo + "' no encontrado");
        }

        return new FileSystemResource(archivo.toFile());
    }

    private String extraerExtension(String nombreArchivo) {
        int puntoIndex = nombreArchivo.lastIndexOf('.');
        if (puntoIndex < 0) {
            throw new IllegalArgumentException("El archivo no tiene extensión");
        }
        return nombreArchivo.substring(puntoIndex + 1);
    }
}
