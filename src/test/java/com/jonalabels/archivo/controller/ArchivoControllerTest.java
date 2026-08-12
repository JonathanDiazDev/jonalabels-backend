package com.jonalabels.archivo.controller;

import com.jonalabels.archivo.dto.ArchivoResponseDTO;
import com.jonalabels.archivo.service.StorageService;
import com.jonalabels.common.exception.GlobalExceptionHandler;
import com.jonalabels.security.config.RateLimitFilter;
import com.jonalabels.security.config.SecurityConfig;
import com.jonalabels.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArchivoController.class)
@Import({SecurityConfig.class, RateLimitFilter.class, GlobalExceptionHandler.class})
class ArchivoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(roles = "CLIENTE")
    void subirArchivo_archivoValido_retorna200ConNombreYUrl() throws Exception {
        when(storageService.guardar(any())).thenReturn("abc-123.png");

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "contenido".getBytes());

        mockMvc.perform(multipart("/api/v1/archivos").file(archivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreArchivo").value("abc-123.png"))
                .andExpect(jsonPath("$.url").value("/api/v1/archivos/abc-123.png"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void subirArchivo_archivoVacio_retorna400() throws Exception {
        when(storageService.guardar(any()))
                .thenThrow(new IllegalArgumentException("El archivo está vacío"));

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "vacio.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/v1/archivos").file(archivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El archivo está vacío"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void subirArchivo_extensionNoPermitida_retorna400() throws Exception {
        when(storageService.guardar(any()))
                .thenThrow(new IllegalArgumentException("Tipo de archivo no permitido: .exe"));

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "virus.exe", "application/octet-stream", " contenido".getBytes());

        mockMvc.perform(multipart("/api/v1/archivos").file(archivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("no permitido")));
    }

    @Test
    void subirArchivo_sinAutenticacion_retorna403() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "contenido".getBytes());

        mockMvc.perform(multipart("/api/v1/archivos").file(archivo))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void subirArchivo_comoAdmin_retorna403() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "contenido".getBytes());

        mockMvc.perform(multipart("/api/v1/archivos").file(archivo))
                .andExpect(status().isForbidden());
    }

    @Test
    void descargarArchivo_archivoExistente_retorna200() throws Exception {
        ByteArrayResource recurso = new ByteArrayResource("contenido".getBytes());
        when(storageService.cargarComoRecurso("abc-123.png")).thenReturn(recurso);

        mockMvc.perform(get("/api/v1/archivos/abc-123.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("inline")));
    }

    @Test
    void descargarArchivo_archivoPdf_retorna200() throws Exception {
        ByteArrayResource recurso = new ByteArrayResource("pdf content".getBytes());
        when(storageService.cargarComoRecurso("doc-456.pdf")).thenReturn(recurso);

        mockMvc.perform(get("/api/v1/archivos/doc-456.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void descargarArchivo_archivoNoExiste_retorna404() throws Exception {
        when(storageService.cargarComoRecurso("no-existe.png"))
                .thenThrow(new com.jonalabels.common.exception.RecursoNoEncontradoException("Archivo 'no-existe.png' no encontrado"));

        mockMvc.perform(get("/api/v1/archivos/no-existe.png"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void descargarArchivo_sinAutenticacion_retorna200() throws Exception {
        ByteArrayResource recurso = new ByteArrayResource("contenido".getBytes());
        when(storageService.cargarComoRecurso("public-logo.png")).thenReturn(recurso);

        mockMvc.perform(get("/api/v1/archivos/public-logo.png"))
                .andExpect(status().isOk());
    }
}
