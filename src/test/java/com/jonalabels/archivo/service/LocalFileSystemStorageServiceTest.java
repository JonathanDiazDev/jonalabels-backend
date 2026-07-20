package com.jonalabels.archivo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileSystemStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileSystemStorageService storageService;

    @BeforeEach
    void setUp() throws Exception {
        storageService = new LocalFileSystemStorageService();

        var field = LocalFileSystemStorageService.class.getDeclaredField("uploadDir");
        field.setAccessible(true);
        field.set(storageService, tempDir.toString());

        var initMethod = LocalFileSystemStorageService.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(storageService);
    }

    @Test
    void guardar_archivoValido_retornaNombreUnico() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "contenido".getBytes());

        String nombreGuardado = storageService.guardar(archivo);

        assertThat(nombreGuardado).endsWith(".png");
        assertThat(nombreGuardado).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png");
    }

    @Test
    void guardar_archivoVacio_lanzaExcepcion() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> storageService.guardar(archivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void guardar_archivoExtensionNoPermitida_lanzaExcepcion() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "virus.exe", "application/octet-stream", "contenido".getBytes());

        assertThatThrownBy(() -> storageService.guardar(archivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no permitido")
                .hasMessageContaining(".exe");
    }

    @Test
    void guardar_archivoSinExtension_lanzaExcepcion() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "sinextension", "application/octet-stream", "contenido".getBytes());

        assertThatThrownBy(() -> storageService.guardar(archivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("extensión");
    }

    @Test
    void guardar_archivoJpg_retornaNombreCorrecto() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", "contenido".getBytes());

        String nombreGuardado = storageService.guardar(archivo);

        assertThat(nombreGuardado).endsWith(".jpg");
    }

    @Test
    void guardar_archivoPdf_retornaNombreCorrecto() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "documento.pdf", "application/pdf", "contenido".getBytes());

        String nombreGuardado = storageService.guardar(archivo);

        assertThat(nombreGuardado).endsWith(".pdf");
    }

    @Test
    void guardar_archivoRealSeGuardaEnDisco() throws IOException {
        byte[] contenido = "test content".getBytes();
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", contenido);

        String nombreGuardado = storageService.guardar(archivo);

        Path archivoGuardado = tempDir.resolve(nombreGuardado);
        assertThat(Files.exists(archivoGuardado)).isTrue();
        assertThat(Files.readAllBytes(archivoGuardado)).isEqualTo(contenido);
    }

    @Test
    void cargarComoRecurso_archivoExistente_retornaResource() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "contenido".getBytes());
        String nombreGuardado = storageService.guardar(archivo);

        var recurso = storageService.cargarComoRecurso(nombreGuardado);

        assertThat(recurso).isNotNull();
        assertThat(recurso.exists()).isTrue();
    }

    @Test
    void cargarComoRecurso_archivoNoExiste_lanzaExcepcion() {
        assertThatThrownBy(() -> storageService.cargarComoRecurso("no-existe.png"))
                .isInstanceOf(com.jonalabels.common.exception.RecursoNoEncontradoException.class)
                .hasMessageContaining("Archivo");
    }
}
