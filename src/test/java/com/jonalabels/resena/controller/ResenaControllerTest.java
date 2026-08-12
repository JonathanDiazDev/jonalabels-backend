package com.jonalabels.resena.controller;

import com.jonalabels.common.exception.GlobalExceptionHandler;
import com.jonalabels.common.exception.RecursoNoEncontradoException;
import com.jonalabels.resena.domain.EstadoModeracion;
import com.jonalabels.resena.domain.Resena;
import com.jonalabels.resena.service.ResenaService;
import com.jonalabels.auth.domain.Usuario;
import com.jonalabels.auth.service.CurrentUserService;
import com.jonalabels.security.config.RateLimitFilter;
import com.jonalabels.security.config.SecurityConfig;
import com.jonalabels.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResenaController.class)
@Import({SecurityConfig.class, RateLimitFilter.class, GlobalExceptionHandler.class})
class ResenaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResenaService resenaService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CurrentUserService currentUserService;

    private Usuario cliente() {
        return Usuario.builder().id(1L).email("cliente@test.com").rol("CLIENTE").build();
    }

    private Resena buildResena(Long id, EstadoModeracion estado) {
        return Resena.builder()
                .id(id)
                .usuarioId(1L)
                .pedidoId(1L)
                .productoId(1L)
                .calificacion(5)
                .comentario("Excelente producto")
                .estadoModeracion(estado)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
    void crearResena_datosValidos_retorna201() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(cliente());
        Resena resena = buildResena(1L, EstadoModeracion.PENDIENTE);
        when(resenaService.crearResena(eq(1L), eq(1L), anyInt(), nullable(String.class)))
                .thenReturn(resena);

        mockMvc.perform(post("/api/v1/resenas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "pedidoId": 1,
                                    "calificacion": 5,
                                    "comentario": "Excelente producto"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estadoModeracion").value("PENDIENTE"))
                .andExpect(jsonPath("$.calificacion").value(5));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void crearResena_calificacionInvalida_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/resenas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "pedidoId": 1,
                                    "calificacion": 7
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void crearResena_calificacionNula_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/resenas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "pedidoId": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"));
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
    void crearResena_usuarioSinPedidoPagado_retorna409() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(cliente());
        when(resenaService.crearResena(eq(1L), eq(1L), anyInt(), nullable(String.class)))
                .thenThrow(new IllegalStateException("Solo compradores verificados pueden dejar una reseña"));

        mockMvc.perform(post("/api/v1/resenas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "pedidoId": 1,
                                    "calificacion": 5,
                                    "comentario": "test"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Solo compradores verificados pueden dejar una reseña"));
    }

    @Test
    void crearResena_sinAutenticacion_retorna403() throws Exception {
        mockMvc.perform(post("/api/v1/resenas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "pedidoId": 1,
                                    "calificacion": 5
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void moderarResena_datosValidos_retorna200() throws Exception {
        Resena resena = buildResena(1L, EstadoModeracion.APROBADA);
        when(resenaService.moderarResena(1L, EstadoModeracion.APROBADA)).thenReturn(resena);

        mockMvc.perform(patch("/api/v1/resenas/1/moderacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "estado": "APROBADA"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoModeracion").value("APROBADA"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void moderarResena_comoCliente_retorna403() throws Exception {
        mockMvc.perform(patch("/api/v1/resenas/1/moderacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "estado": "APROBADA"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void moderarResena_resenaNoEncontrada_retorna404() throws Exception {
        when(resenaService.moderarResena(99L, EstadoModeracion.APROBADA))
                .thenThrow(new RecursoNoEncontradoException("Resena", 99L));

        mockMvc.perform(patch("/api/v1/resenas/99/moderacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "estado": "APROBADA"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("99")));
    }

    @Test
    void obtenerResenasAprobadas_retorna200() throws Exception {
        Resena aprobada1 = buildResena(1L, EstadoModeracion.APROBADA);
        Resena aprobada2 = buildResena(2L, EstadoModeracion.APROBADA);
        when(resenaService.obtenerResenasAprobadas()).thenReturn(List.of(aprobada1, aprobada2));

        mockMvc.perform(get("/api/v1/resenas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].estadoModeracion").value("APROBADA"))
                .andExpect(jsonPath("$[1].estadoModeracion").value("APROBADA"));
    }

    @Test
    void obtenerResenasAprobadas_listaVacia_retorna200() throws Exception {
        when(resenaService.obtenerResenasAprobadas()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/resenas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void obtenerResenasAprobadas_sinAutenticacion_retorna200() throws Exception {
        when(resenaService.obtenerResenasAprobadas()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/resenas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
