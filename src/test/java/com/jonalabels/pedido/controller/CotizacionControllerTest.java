package com.jonalabels.pedido.controller;

import com.jonalabels.cloudinary.service.CloudinaryService;
import com.jonalabels.common.exception.GlobalExceptionHandler;
import com.jonalabels.pedido.domain.EstadoCotizacion;
import com.jonalabels.pedido.dto.CotizacionResponseDTO;
import com.jonalabels.pedido.dto.MetricasDashboardDTO;
import com.jonalabels.pedido.service.CotizacionService;
import com.jonalabels.security.config.SecurityConfig;
import com.jonalabels.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CotizacionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CotizacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CotizacionService cotizacionService;

    @MockitoBean
    private CloudinaryService cloudinaryService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void crearCotizacion_conDatosValidos_retorna201() throws Exception {
        var archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "contenido-falso".getBytes());
        var data = new MockMultipartFile(
                "data", "", "application/json",
                """
                {"nombre": "María García", "whatsapp": "5512345678", "email": "maria@ejemplo.com", "cantidad": 5000, "medidas": "5cm x 3cm"}
                """.getBytes());

        when(cloudinaryService.subirArchivo(any())).thenReturn("https://res.cloudinary.com/...");

        mockMvc.perform(multipart("/api/v1/cotizaciones")
                        .file(archivo)
                        .file(data))
                .andExpect(status().isCreated());

        verify(cotizacionService).crear(any(), any());
    }

    @Test
    void crearCotizacion_soloCamposObligatorios_retorna201() throws Exception {
        var archivo = new MockMultipartFile(
                "archivo", "diseno.pdf", "application/pdf", "pdf-falso".getBytes());
        var data = new MockMultipartFile(
                "data", "", "application/json",
                """
                {"nombre": "Carlos López", "whatsapp": "+52 55 1234 5678", "cantidad": 5000, "medidas": "medidas estándar"}
                """.getBytes());

        when(cloudinaryService.subirArchivo(any())).thenReturn("https://res.cloudinary.com/...");

        mockMvc.perform(multipart("/api/v1/cotizaciones")
                        .file(archivo)
                        .file(data))
                .andExpect(status().isCreated());

        verify(cotizacionService).crear(any(), any());
    }

    @Test
    void crearCotizacion_emailNoEsObligatorio_retorna201() throws Exception {
        var archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "x".getBytes());
        var data = new MockMultipartFile(
                "data", "", "application/json",
                """
                {"nombre": "Juan Pérez", "whatsapp": "5512345678", "email": "", "cantidad": 5000, "medidas": "5x3"}
                """.getBytes());

        when(cloudinaryService.subirArchivo(any())).thenReturn("https://res.cloudinary.com/...");

        mockMvc.perform(multipart("/api/v1/cotizaciones")
                        .file(archivo)
                        .file(data))
                .andExpect(status().isCreated());
    }

    @Test
    void crearCotizacion_cantidadExacta5000_retorna201() throws Exception {
        var archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "x".getBytes());
        var data = new MockMultipartFile(
                "data", "", "application/json",
                """
                {"nombre": "Luis García", "whatsapp": "5512345678", "cantidad": 5000, "medidas": "10x10"}
                """.getBytes());

        when(cloudinaryService.subirArchivo(any())).thenReturn("https://res.cloudinary.com/...");

        mockMvc.perform(multipart("/api/v1/cotizaciones")
                        .file(archivo)
                        .file(data))
                .andExpect(status().isCreated());
    }

    @Test
    void crearCotizacion_cantidadMenorA5000_retorna400() throws Exception {
        var archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "x".getBytes());
        var data = new MockMultipartFile(
                "data", "", "application/json",
                """
                {"nombre": "María García", "whatsapp": "5512345678", "cantidad": 4999, "medidas": "5cm x 3cm"}
                """.getBytes());

        mockMvc.perform(multipart("/api/v1/cotizaciones")
                        .file(archivo)
                        .file(data))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearCotizacion_sinCantidad_retorna400() throws Exception {
        var archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "x".getBytes());
        var data = new MockMultipartFile(
                "data", "", "application/json",
                """
                {"nombre": "María García", "whatsapp": "5512345678", "cantidad": null, "medidas": "5cm x 3cm"}
                """.getBytes());

        mockMvc.perform(multipart("/api/v1/cotizaciones")
                        .file(archivo)
                        .file(data))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearCotizacion_sinNombre_retorna400() throws Exception {
        var archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "x".getBytes());
        var data = new MockMultipartFile(
                "data", "", "application/json",
                """
                {"nombre": "", "whatsapp": "5512345678", "cantidad": 5000, "medidas": "5cm x 3cm"}
                """.getBytes());

        mockMvc.perform(multipart("/api/v1/cotizaciones")
                        .file(archivo)
                        .file(data))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearCotizacion_sinWhatsapp_retorna400() throws Exception {
        var archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "x".getBytes());
        var data = new MockMultipartFile(
                "data", "", "application/json",
                """
                {"nombre": "María García", "whatsapp": "", "cantidad": 5000, "medidas": "5cm x 3cm"}
                """.getBytes());

        mockMvc.perform(multipart("/api/v1/cotizaciones")
                        .file(archivo)
                        .file(data))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearCotizacion_sinMedidas_retorna400() throws Exception {
        var archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "x".getBytes());
        var data = new MockMultipartFile(
                "data", "", "application/json",
                """
                {"nombre": "María García", "whatsapp": "5512345678", "cantidad": 5000, "medidas": ""}
                """.getBytes());

        mockMvc.perform(multipart("/api/v1/cotizaciones")
                        .file(archivo)
                        .file(data))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearCotizacion_sinArchivo_retorna201() throws Exception {
        var data = new MockMultipartFile(
                "data", "", "application/json",
                """
                {"nombre": "María García", "whatsapp": "5512345678", "cantidad": 5000, "medidas": "5cm x 3cm"}
                """.getBytes());

        mockMvc.perform(multipart("/api/v1/cotizaciones")
                        .file(data))
                .andExpect(status().isCreated());

        verify(cotizacionService).crear(any(), any());
    }

    @Test
    void crearCotizacion_sinAutenticacion_retorna201() throws Exception {
        var archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "x".getBytes());
        var data = new MockMultipartFile(
                "data", "", "application/json",
                """
                {"nombre": "Público", "whatsapp": "5512345678", "cantidad": 5000, "medidas": "estándar"}
                """.getBytes());

        when(cloudinaryService.subirArchivo(any())).thenReturn("https://res.cloudinary.com/...");

        mockMvc.perform(multipart("/api/v1/cotizaciones")
                        .file(archivo)
                        .file(data))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void listarCotizaciones_retornaPaginaConStatus200() throws Exception {
        var ahora = LocalDateTime.now();
        var cotizaciones = List.of(
                new CotizacionResponseDTO(1L, "María García", "5512345678", "maria@ejemplo.com", 5000, "Etiquetas de Satén", "5cm x 3cm", ahora, EstadoCotizacion.NUEVO, "https://res.cloudinary.com/demo.jpg"),
                new CotizacionResponseDTO(2L, "Carlos López", "5512345678", null, 10000, null, "10cm", ahora.minusHours(1), EstadoCotizacion.CONTACTADO, null));
        var pagina = new PageImpl<>(cotizaciones, PageRequest.of(0, 10), 2);

        when(cotizacionService.obtenerCotizacionesPaginadas(any(), any(), any())).thenReturn(pagina);

        mockMvc.perform(get("/api/v1/cotizaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].nombre", is("María García")))
                .andExpect(jsonPath("$.content[0].whatsapp", is("5512345678")))
                .andExpect(jsonPath("$.content[0].email", is("maria@ejemplo.com")))
                .andExpect(jsonPath("$.content[0].cantidad", is(5000)))
                .andExpect(jsonPath("$.content[0].medidas", is("5cm x 3cm")))
                .andExpect(jsonPath("$.content[0].estado", is("NUEVO")))
                .andExpect(jsonPath("$.content[1].id", is(2)))
                .andExpect(jsonPath("$.content[1].email").doesNotExist())
                .andExpect(jsonPath("$.content[1].estado", is("CONTACTADO")))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.number", is(0)));
    }

    @Test
    @WithMockUser
    void obtenerMetricas_retornaDashboardDTO() throws Exception {
        var metricas = new MetricasDashboardDTO(10L, 50000L, 3L);

        when(cotizacionService.obtenerMetricas()).thenReturn(metricas);

        mockMvc.perform(get("/api/v1/cotizaciones/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProspectos", is(10)))
                .andExpect(jsonPath("$.totalPiezasSolicitadas", is(50000)))
                .andExpect(jsonPath("$.prospectosNuevos", is(3)));
    }

    @Test
    @WithMockUser
    void exportarCsv_retornaArchivoConHeaders() throws Exception {
        var csv = "ID,Fecha,Nombre,WhatsApp,Email,Cantidad,Tipo Producto,Medidas,Estado\n1,01/01/2025 10:00,María García,5512345678,maria@ejemplo.com,5000,Etiquetas de Satén,5cm x 3cm,NUEVO\n".getBytes();

        when(cotizacionService.exportarCotizacionesCsv(any(), any())).thenReturn(csv);

        mockMvc.perform(get("/api/v1/cotizaciones/exportar"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"prospectos_jonalabels.csv\""))
                .andExpect(content().bytes(csv));
    }

    @Test
    @WithMockUser
    void cambiarEstado_retornaCotizacionActualizada() throws Exception {
        var actualizada = new CotizacionResponseDTO(1L, "María García", "5512345678", "maria@ejemplo.com", 5000, "Etiquetas de Satén", "5cm x 3cm", LocalDateTime.now(), EstadoCotizacion.COTIZADO, null);

        when(cotizacionService.actualizarEstado(anyLong(), any())).thenReturn(actualizada);

        mockMvc.perform(patch("/api/v1/cotizaciones/1/estado")
                        .param("estado", "COTIZADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("COTIZADO")))
                .andExpect(jsonPath("$.nombre", is("María García")));
    }

    @Test
    void crearCotizacion_fallaCloudinary_retorna500() throws Exception {
        var archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "x".getBytes());
        var data = new MockMultipartFile(
                "data", "", "application/json",
                """
                {"nombre": "Test", "whatsapp": "5512345678", "cantidad": 5000, "medidas": "5cm"}
                """.getBytes());

        when(cotizacionService.crear(any(), any()))
                .thenThrow(new RuntimeException("Error al subir archivo a Cloudinary"));

        mockMvc.perform(multipart("/api/v1/cotizaciones")
                        .file(archivo)
                        .file(data))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is("Error al subir archivo a Cloudinary")));
    }
}
