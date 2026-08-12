package com.jonalabels.pedido.controller;

import com.jonalabels.common.exception.GlobalExceptionHandler;
import com.jonalabels.entity.Diseno;
import com.jonalabels.entity.Producto;
import com.jonalabels.entity.Taller;
import com.jonalabels.auth.domain.Usuario;
import com.jonalabels.pedido.domain.EstadoPedido;
import com.jonalabels.pedido.domain.IllegalPedidoStateException;
import com.jonalabels.pedido.domain.Pedido;
import com.jonalabels.pedido.service.PedidoService;
import com.jonalabels.common.exception.RecursoNoEncontradoException;
import com.jonalabels.auth.domain.Usuario;
import com.jonalabels.auth.service.CurrentUserService;
import com.jonalabels.security.config.RateLimitFilter;
import com.jonalabels.security.config.SecurityConfig;
import com.jonalabels.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PedidoController.class)
@Import({SecurityConfig.class, RateLimitFilter.class, GlobalExceptionHandler.class})
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService pedidoService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CurrentUserService currentUserService;

    private Usuario cliente() {
        return Usuario.builder().id(1L).email("cliente@test.com").rol("CLIENTE").build();
    }

    private Pedido buildPedido(Long id, EstadoPedido estado) {
        Usuario usuario = Usuario.builder().id(1L).build();
        Producto producto = Producto.builder().id(1L).build();
        Diseno diseno = Diseno.builder().id(1L).usuario(usuario).urlArchivoLogo("x").build();

        return Pedido.builder()
                .id(id)
                .usuario(usuario)
                .producto(producto)
                .diseno(diseno)
                .estado(estado)
                .cantidad(500)
                .precioFinalCotizado(new BigDecimal("1250.00"))
                .costoTallerAcordado(new BigDecimal("750.00"))
                .comentariosAdmin("Factible")
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
    void crearSolicitud_retorna201() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(cliente());
        Pedido pedido = buildPedido(1L, EstadoPedido.ESPERANDO_FACTIBILIDAD);
        when(pedidoService.crearSolicitud(anyLong(), anyLong(), anyLong(), anyInt(), any())).thenReturn(pedido);

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productoId": 1,
                                    "disenoId": 1,
                                    "cantidad": 500
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("ESPERANDO_FACTIBILIDAD"))
                .andExpect(jsonPath("$.cantidad").value(500));
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
    void crearSolicitud_conUrlDiseno_retorna201() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(cliente());
        Pedido pedido = buildPedido(1L, EstadoPedido.ESPERANDO_FACTIBILIDAD);
        pedido.setUrlDiseno("https://s3.amazonaws.com/logos/diseno.png");
        when(pedidoService.crearSolicitud(anyLong(), anyLong(), anyLong(), anyInt(), any())).thenReturn(pedido);

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productoId": 1,
                                    "disenoId": 1,
                                    "cantidad": 500,
                                    "urlDiseno": "https://s3.amazonaws.com/logos/diseno.png"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("ESPERANDO_FACTIBILIDAD"))
                .andExpect(jsonPath("$.cantidad").value(500))
                .andExpect(jsonPath("$.urlDiseno").value("https://s3.amazonaws.com/logos/diseno.png"));
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
    void crearSolicitud_conDisenoIdNulo_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productoId": 1,
                                    "disenoId": null,
                                    "cantidad": 500
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"))
                .andExpect(jsonPath("$.details[0].field").value("disenoId"));
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
    void crearSolicitud_conCantidadNull_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productoId": 1,
                                    "disenoId": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"))
                .andExpect(jsonPath("$.details[0].field").value("cantidad"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cotizarPedido_retorna200() throws Exception {
        Pedido pedido = buildPedido(1L, EstadoPedido.COTIZADO);
        when(pedidoService.cotizarPedido(eq(1L), anyLong(), any(BigDecimal.class), any(BigDecimal.class), nullable(String.class)))
                .thenReturn(pedido);

        mockMvc.perform(patch("/api/v1/pedidos/1/cotizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "tallerId": 1,
                                    "costoTaller": 750.00,
                                    "precioFinal": 1250.00,
                                    "comentarios": "Factible"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COTIZADO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cotizarPedido_conTallerIdNulo_retorna400() throws Exception {
        mockMvc.perform(patch("/api/v1/pedidos/1/cotizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "tallerId": null,
                                    "costoTaller": 750.00,
                                    "precioFinal": 1250.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"))
                .andExpect(jsonPath("$.details[0].field").value("tallerId"));
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
    void registrarPago_retorna200() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(cliente());
        Pedido pedido = buildPedido(1L, EstadoPedido.PAGADO);
        when(pedidoService.registrarPago(1L, 1L)).thenReturn(pedido);

        mockMvc.perform(patch("/api/v1/pedidos/1/pago")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADO"));
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
    void registrarPago_estadoInvalido_retorna400() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(cliente());
        when(pedidoService.registrarPago(1L, 1L))
                .thenThrow(new IllegalPedidoStateException(1L, EstadoPedido.ESPERANDO_FACTIBILIDAD, EstadoPedido.COTIZADO));

        mockMvc.perform(patch("/api/v1/pedidos/1/pago")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ESPERANDO_FACTIBILIDAD")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cotizarPedido_pedidoNoEncontrado_retorna404() throws Exception {
        when(pedidoService.cotizarPedido(eq(99L), anyLong(), any(BigDecimal.class), any(BigDecimal.class), nullable(String.class)))
                .thenThrow(new RecursoNoEncontradoException("Pedido", 99L));

        mockMvc.perform(patch("/api/v1/pedidos/99/cotizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "tallerId": 1,
                                    "costoTaller": 750.00,
                                    "precioFinal": 1250.00
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("99")));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void cotizarPedido_comoCliente_retorna403() throws Exception {
        mockMvc.perform(patch("/api/v1/pedidos/1/cotizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "tallerId": 1,
                                    "costoTaller": 750.00,
                                    "precioFinal": 1250.00
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearSolicitud_sinAutenticacion_retorna403() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productoId": 1,
                                    "disenoId": 1,
                                    "cantidad": 500
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = "CLIENTE")
    void crearSolicitud_urlDisenoOpcional_retorna201() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(cliente());
        Pedido pedido = buildPedido(1L, EstadoPedido.ESPERANDO_FACTIBILIDAD);
        when(pedidoService.crearSolicitud(anyLong(), anyLong(), anyLong(), anyInt(), any())).thenReturn(pedido);

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "productoId": 1,
                                    "disenoId": 1,
                                    "cantidad": 500
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.urlDiseno").doesNotExist());
    }
}
